package com.liftric.persisted.queue

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

interface Queue {
    val jobs: List<JobContext>
    val configuration: Configuration

    data class Configuration(
        val scope: CoroutineScope,
        val maxConcurrency: Int
    )
}

class JobQueue(override val configuration: Queue.Configuration): Queue {
    private val cancellationQueue = MutableSharedFlow<kotlinx.coroutines.Job>(extraBufferCapacity = Int.MAX_VALUE)
    private val queue = atomic(mutableListOf<Job>())
    private val lock = Semaphore(configuration.maxConcurrency, 0)
    private val isCancelling = Mutex(false)
    override val jobs: List<JobContext>
        get() = queue.value

    init {
        cancellationQueue.onEach { it.join() }
            .flowOn(Dispatchers.Default)
            .launchIn(configuration.scope)
    }

    internal fun add(job: Job) {
        queue.value = queue.value.plus(listOf(job)).sortedBy { it.startTime }.toMutableList()
    }

    suspend fun start() {
        while (configuration.scope.isActive) {
            delay(1000L)
            if (queue.value.isEmpty()) break
            if (isCancelling.isLocked) break
            if (lock.availablePermits < 1) break
            val job = queue.value.first()
            if (job.isCancelled) {
                queue.value.remove(job)
            } else if (job.startTime <= Clock.System.now()) {
                lock.withPermit {
                    withTimeout(job.info.timeout) {
                        job.run()
                        queue.value.remove(job)
                    }
                }
            }
        }
    }

    suspend fun cancel() {
        submitCancellation {
            isCancelling.withLock {
                configuration.scope.coroutineContext.cancelChildren()
                queue.value.clear()
            }
        }
    }

    suspend fun cancel(id: UUID) {
        submitCancellation {
            isCancelling.withLock {
                queue.value.firstOrNull { it.id == id }?.let { job ->
                    job.cancel()
                    queue.value.remove(job)
                }
            }
        }
    }

    suspend fun cancel(tag: String) {
        submitCancellation {
            isCancelling.withLock {
                queue.value.firstOrNull { it.info.tag == tag }?.let { job ->
                    job.cancel()
                    queue.value.remove(job)
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
