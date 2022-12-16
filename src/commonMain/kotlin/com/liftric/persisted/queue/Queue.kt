package com.liftric.persisted.queue

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.getOrEmpty
import io.github.xxfast.kstore.minus
import io.github.xxfast.kstore.plus
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

interface Queue {
    val jobs: List<JobContext>
    val configuration: Configuration

    data class Configuration(
        val scope: CoroutineScope,
        val maxConcurrency: Int
    )
}

class JobQueue(private val store: KStore<List<Job<*>>>, override val configuration: Queue.Configuration): Queue {
    private val cancellationQueue = MutableSharedFlow<kotlinx.coroutines.Job>(extraBufferCapacity = Int.MAX_VALUE)
    private val queue = atomic(mutableListOf<Job<*>>())
    private val lock = Semaphore(configuration.maxConcurrency, 0)
    private val isCancelling = Mutex(false)
    override val jobs: List<JobContext>
        get() = queue.value

    init {
        cancellationQueue.onEach { it.join() }
            .flowOn(Dispatchers.Default)
            .launchIn(configuration.scope)

        configuration.scope.launch {
            store.getOrEmpty().forEach { job ->
                add(job)
            }
        }
    }

    @PublishedApi
    internal fun add(job: Job<*>) {
        queue.value = queue.value.plus(listOf(job)).sortedBy { it.startTime }.toMutableList()
    }

    suspend fun start() {
        while (configuration.scope.isActive) {
            if (queue.value.isEmpty()) break
            if (isCancelling.isLocked) break
            if (lock.availablePermits < 1) break
            val job = queue.value.first()
            if (job.isCancelled) {
                queue.value.remove(job)
            } else if (job.startTime <= Clock.System.now()) {
                lock.withPermit {
                    if (job.info.shouldPersist) {
                        store.plus(job)
                    }
                    withTimeout(job.info.timeout) {
                        queue.value.remove(job)
                        store.minus(job)
                    }
                }
            }
        }
    }

    suspend fun cancel() {
        submitCancellation(coroutineContext) {
            isCancelling.withLock {
                configuration.scope.coroutineContext.cancelChildren()
                queue.value.clear()
                store.delete()
            }
        }
    }

    suspend fun cancel(id: UUID) {
        submitCancellation(coroutineContext) {
            isCancelling.withLock {
                queue.value.firstOrNull { it.id == id }?.let { job ->
                    job.cancel()
                    queue.value.remove(job)
                    store.minus(job)
                }
            }
        }
    }

    suspend fun cancel(tag: String) {
        submitCancellation(coroutineContext) {
            isCancelling.withLock {
                queue.value.firstOrNull { it.info.tag == tag }?.let { job ->
                    job.cancel()
                    queue.value.remove(job)
                    store.minus(job)
                }
            }
        }
    }

    private fun submitCancellation(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val job = configuration.scope.launch(context, CoroutineStart.LAZY, block)
        cancellationQueue.tryEmit(job)
    }
}
