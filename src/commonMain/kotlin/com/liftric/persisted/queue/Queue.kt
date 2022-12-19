package com.liftric.persisted.queue

import com.russhwolf.settings.Settings
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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

    companion object Default {
        val DefaultConfiguration = Configuration(
            scope = CoroutineScope(Dispatchers.Default),
            maxConcurrency = 1
        )
    }
}

class JobQueue(
    private val settings: Settings,
    private val format: Json,
    override val configuration: Queue.Configuration,
    private val onRestore: (Job) -> Unit
): Queue {
    private val cancellationQueue = MutableSharedFlow<kotlinx.coroutines.Job>(extraBufferCapacity = Int.MAX_VALUE)
    private val queue = atomic(mutableListOf<Job>())
    private val lock = Semaphore(configuration.maxConcurrency, 0)
    private val isCancelling = Mutex(false)
    override val jobs: List<JobContext>
        get() = queue.value
    private var isRunning: Boolean = false
    private var cancellable: kotlinx.coroutines.Job? = null

    init {
        cancellationQueue.onEach { it.join() }
            .flowOn(Dispatchers.Default)
            .launchIn(configuration.scope)
    }

    internal fun add(job: Job) {
        queue.value = queue.value.plus(listOf(job)).sortedBy { it.startTime }.toMutableList()
    }

    fun start() {
        if (isRunning) return else isRunning = true
        restore()
        cancellable = CoroutineScope(Dispatchers.Default).launch {
            while (coroutineContext.isActive) {
                if (queue.value.isEmpty()) break
                if (isCancelling.isLocked) break
                if (lock.availablePermits < 1) break
                val job = queue.value.first()
                if (job.isCancelled) {
                    queue.value.remove(job)
                } else if (job.startTime <= Clock.System.now()) {
                    lock.withPermit {
                        configuration.scope.launch {
                            withTimeout(job.info.timeout) {
                                job.run()
                                queue.value.remove(job)
                            }
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        if (isRunning) isRunning = false else return
        cancellable?.cancel()
    }

    suspend fun clear() {
        submitCancellation(coroutineContext) {
            isCancelling.withLock {
                queue.value.clear()
                configuration.scope.coroutineContext.cancelChildren()
                settings.clear()
            }
        }
    }

    suspend fun cancel(id: UUID) {
        submitCancellation(coroutineContext) {
            isCancelling.withLock {
                queue.value.firstOrNull { it.id == id }?.let { job ->
                    job.cancel()
                    queue.value.remove(job)
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
                }
            }
        }
    }

    private fun restore() {
        settings.keys.forEach { key ->
            val job: Job = format.decodeFromString(settings.getString(key, ""))
            onRestore(job)
            add(job)
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
