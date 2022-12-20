package com.liftric.persisted.queue

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.time.Duration
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
    private val enqueuedJobs = atomic(mutableListOf<Job>())

    /**
     * Running jobs
     */
    private val dequeuedJobs = atomic(mutableListOf<Job>())

    /**
     * Semaphore to limit concurrency
     */
    private val lock = Semaphore(configuration.maxConcurrency, 0)

    private var cancellable: kotlinx.coroutines.Job? = null

    override val jobs: List<JobContext>
        get() = enqueuedJobs.value
    override val runningJobs: List<JobContext>
        get() = dequeuedJobs.value

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
        cancellable = CoroutineScope(Dispatchers.Default).launchPeriodicAsync(1.seconds) {
            if (enqueuedJobs.value.isEmpty()) return@launchPeriodicAsync
            if (lock.availablePermits < 1) return@launchPeriodicAsync
            val job = enqueuedJobs.value.removeFirst()
            if (job.isCancelled) return@launchPeriodicAsync
            dequeuedJobs.value.add(job)
            if (job.startTime <= Clock.System.now()) {
                configuration.scope.launch {
                    lock.withPermit {
                        withTimeout(job.info.timeout) {
                            job.run()
                            dequeuedJobs.value.remove(job)
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
    fun clear(cancelJobs: Boolean = true, clearStore: Boolean = true) {
        enqueuedJobs.value.clear()
        if (cancelJobs) {
            dequeuedJobs.value.clear()
            configuration.scope.coroutineContext.cancelChildren()
        }
        if (clearStore) {
            store.clear()
        }
    }

    /**
     * Cancels jobs
     * @param id Unique identifier of the job
     */
    suspend fun cancel(id: UUID) {
        enqueuedJobs.value.firstOrNull { it.id == id }?.let { job ->
            job.cancel()
            enqueuedJobs.value.remove(job)
        } ?: dequeuedJobs.value.firstOrNull { it.id == id }?.let { job ->
            job.cancel()
            dequeuedJobs.value.remove(job)
        }
    }

    /**
     * Cancels job
     * @param tag User defined tag of the job
     */
    suspend fun cancel(tag: String) {
        enqueuedJobs.value.firstOrNull { it.info.tag == tag }?.let { job ->
            job.cancel()
            enqueuedJobs.value.remove(job)
        } ?: dequeuedJobs.value.firstOrNull { it.info.tag == tag }?.let { job ->
            job.cancel()
            dequeuedJobs.value.remove(job)
        }
    }

    /**
     * Enqueues job and sorts queue based on start time
     * @param job Job to enqueue
     */
    internal fun add(job: Job) {
        enqueuedJobs.value = enqueuedJobs.value.plus(listOf(job)).sortedBy { it.startTime }.toMutableList()
    }

    /**
     * Restores all persisted jobs. Ensures job not already in queue.
     */
    internal fun restore() {
        store.keys.forEach { key ->
            val job: Job = format.decodeFromString(store.get(key))
            if (jobs.plus(runningJobs).none { it.id == job.id }) {
                add(onRestore(job))
            }
        }
    }
}

private fun CoroutineScope.launchPeriodicAsync(
    repeat: Duration,
    action: suspend () -> Unit
) = async {
    while (isActive) {
        action()
        delay(repeat)
    }
}
