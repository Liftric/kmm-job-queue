package com.liftric.persisted.queue

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

interface Queue {
    val jobs: List<JobContext>
    val runningJobs: List<JobContext>
    val configuration: Configuration

    data class Configuration(
        val scope: CoroutineScope,
        val maxConcurrency: Int,
        val startsAutomatically: Boolean
    )

    companion object {
        val DefaultConfiguration = Configuration(
            scope = CoroutineScope(Dispatchers.Default),
            maxConcurrency = 1,
            startsAutomatically = false
        )
    }
}

/**
 * Handles job enqueuing and cancelling.
 * @param store Storage to restore jobs
 * @param format Serializer to decode stored jobs
 * @param configuration Queue configuration
 * @param onRestore Callback to mutate restored jobs
 */
class JobQueue(
    private val store: JsonStorage,
    private val format: Json,
    override val configuration: Queue.Configuration,
    private val onRestore: (Job) -> Job
): Queue {
    /**
     * Scheduled jobs
     */
    private val scheduledJobs = atomic(mutableListOf<Job>())

    /**
     * Running jobs
     */
    private val _runningJobs = atomic(mutableListOf<Job>())

    /**
     * Semaphore to limit concurrency
     */
    private val lock = Semaphore(configuration.maxConcurrency, 0)
    private val isCancelling = Mutex()

    private var cancellable: kotlinx.coroutines.Job? = null

    override val jobs: List<JobContext>
        get() = scheduledJobs.value
    override val runningJobs: List<JobContext>
        get() = _runningJobs.value

    init {
        restore()

        if (configuration.startsAutomatically) {
            start()
        }
    }

    /**
     * Starts enqueuing scheduled jobs
     */
    fun start() {
        if (cancellable != null) return
        cancellable = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                lock.withPermit {
                    if (isCancelling.isLocked) return@withPermit
                    if (scheduledJobs.value.isEmpty()) return@withPermit
                    if (scheduledJobs.value.first().startTime.minus(Clock.System.now()) > 0.seconds) return@withPermit
                    val job = scheduledJobs.value.removeFirst()
                    if (job.isCancelled) return@withPermit
                    _runningJobs.value.add(job)
                    configuration.scope.launch {
                        try {
                            withTimeout(job.info.timeout) {
                                job.run()
                            }
                        } finally {
                            _runningJobs.value.remove(job)
                        }
                    }
                }
            }
        }
    }

    /**
     * Stops enqueuing scheduled jobs
     */
    fun stop() {
        cancellable?.cancel()
        cancellable = null
    }

    /**
     * Removes all scheduled jobs
     * @param cancelJobs Cancels running jobs
     * @param clearStore Removes persisted jobs
     */
    suspend fun clear(cancelJobs: Boolean = true, clearStore: Boolean = true) {
        isCancelling.withLock {
            scheduledJobs.value.clear()
            if (cancelJobs) {
                _runningJobs.value.clear()
                configuration.scope.coroutineContext.cancelChildren()
            }
            if (clearStore) {
                store.clear()
            }
        }
    }

    /**
     * Cancels jobs
     * @param id Unique identifier of the job
     */
    suspend fun cancel(id: UUID) {
        isCancelling.withLock {
            scheduledJobs.value.firstOrNull { it.id == id }?.let { job ->
                job.cancel()
                scheduledJobs.value.remove(job)
            } ?: _runningJobs.value.firstOrNull { it.id == id }?.let { job ->
                job.cancel()
                _runningJobs.value.remove(job)
            }
        }
    }

    /**
     * Cancels job
     * @param tag User defined tag of the job
     */
    suspend fun cancel(tag: String) {
        isCancelling.withLock {
            scheduledJobs.value.firstOrNull { it.info.tag == tag }?.let { job ->
                job.cancel()
                scheduledJobs.value.remove(job)
            } ?: _runningJobs.value.firstOrNull { it.info.tag == tag }?.let { job ->
                job.cancel()
                _runningJobs.value.remove(job)
            }
        }
    }

    /**
     * Enqueues job and sorts queue based on start time
     * @param job Job to enqueue
     */
    internal fun add(job: Job) {
        scheduledJobs.value = scheduledJobs.value.plus(listOf(job)).sortedBy { it.startTime }.toMutableList()
    }

    /**
     * Restores all persisted jobs. Ensures job not already in queue.
     */
    internal fun restore() {
        store.keys.forEach { key ->
            val job: Job = format.decodeFromString(store.get(key))
            if (jobs.plus(this.runningJobs).none { it.id == job.id }) {
                add(onRestore(job))
            }
        }
    }
}
