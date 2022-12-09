package com.liftric.persisted.queue

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock

interface Queue {
    val scope: CoroutineScope
    val jobs: List<JobContext>
    val maxConcurrency: Int

    data class Configuration(
        val scope: CoroutineScope,
        val maxConcurrency: Int
    )
}

class JobQueue(
    override val scope: CoroutineScope,
    override val maxConcurrency: Int
): Queue {
    private val queue = atomic(mutableListOf<Job>())
    private val lock = Semaphore(maxConcurrency, 0)
    private val isCancelling = Mutex(false)
    override val jobs: List<JobContext>
        get() = queue.value

    constructor(configuration: Queue.Configuration?) : this(
        configuration?.scope ?: CoroutineScope(Dispatchers.Default),
        configuration?.maxConcurrency ?: 1
    )

    @PublishedApi
    internal fun add(job: Job) {
        queue.value = queue.value.plus(listOf(job)).sortedBy { it.startTime }.toMutableList()
    }

    suspend fun start() {
        while (scope.isActive) {
            delay(1000L)
            if (queue.value.isEmpty()) break
            if (isCancelling.isLocked) break
            if (lock.availablePermits < 1) break
            val job = queue.value.first()
            if (job.isCancelled) {
                queue.value.remove(job)
            } else if (job.startTime <= Clock.System.now()) {
                lock.withPermit {
                    withTimeout(job.timeout) {
                        job.run()
                        queue.value.remove(job)
                    }
                }
            }
        }
    }

    suspend fun cancel() {
        isCancelling.withLock {
            scope.coroutineContext.cancelChildren()
            queue.value.clear()
        }
    }

    suspend fun cancel(id: UUID) {
        isCancelling.withLock {
            val job = queue.value.first { it.id == id }
            job.cancel()
            queue.value.remove(job)
        }
    }

    suspend fun cancel(tag: String) {
        isCancelling.withLock {
            val job = queue.value.first { it.tag == tag }
            job.cancel()
            queue.value.remove(job)
        }
    }
}
