package com.liftric.persisted.queue

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
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

interface Queue {
    val jobs: List<JobContext>
    val runningJobs: List<JobContext>
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
    private val store: JsonStorage,
    private val format: Json,
    override val configuration: Queue.Configuration,
    private val onRestore: (Job) -> Unit
): Queue {
    private val enqueuedJobs = atomic(mutableListOf<Job>())
    private val dequeuedJobs = atomic(mutableListOf<Job>())
    private val lock = Semaphore(configuration.maxConcurrency, 0)
    private var cancellable: kotlinx.coroutines.Job? = null

    override val jobs: List<JobContext>
        get() = enqueuedJobs.value
    override val runningJobs: List<JobContext>
        get() = dequeuedJobs.value

    init {
        restore()
    }

    fun start() {
        cancellable = CoroutineScope(Dispatchers.Default).launchPeriodicAsync(1.seconds) {
            if (enqueuedJobs.value.isEmpty()) return@launchPeriodicAsync
            if (lock.availablePermits < 1) return@launchPeriodicAsync
            val job = enqueuedJobs.value.removeFirst()
            dequeuedJobs.value.add(job)
            if (job.isCancelled) return@launchPeriodicAsync
            if (job.startTime <= Clock.System.now()) {
                lock.withPermit {
                    configuration.scope.launch {
                        withTimeout(job.info.timeout) {
                            job.run()
                            dequeuedJobs.value.remove(job)
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        cancellable?.cancel()
        cancellable = null
    }

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

    suspend fun cancel(id: UUID) {
        enqueuedJobs.value.firstOrNull { it.id == id }?.let { job ->
            job.cancel()
            enqueuedJobs.value.remove(job)
        } ?: dequeuedJobs.value.firstOrNull { it.id == id }?.let { job ->
            job.cancel()
            dequeuedJobs.value.remove(job)
        }
    }

    suspend fun cancel(tag: String) {
        enqueuedJobs.value.firstOrNull { it.info.tag == tag }?.let { job ->
            job.cancel()
            enqueuedJobs.value.remove(job)
        } ?: dequeuedJobs.value.firstOrNull { it.info.tag == tag }?.let { job ->
            job.cancel()
            dequeuedJobs.value.remove(job)
        }
    }

    internal fun add(job: Job) {
        enqueuedJobs.value = enqueuedJobs.value.plus(listOf(job)).sortedBy { it.startTime }.toMutableList()
    }

    internal fun restore() {
        store.keys.forEach { key ->
            val job: Job = format.decodeFromString(store.get(key))
            if (jobs.plus(runningJobs).none { it.id == job.id }) {
                onRestore(job)
                add(job)
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
