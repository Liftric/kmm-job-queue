package com.liftric.job.queue

import com.liftric.job.queue.rules.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlin.time.Duration.Companion.seconds

expect class JobQueue : AbstractJobQueue
abstract class AbstractJobQueue(
    serializers: SerializersModule,
    val networkListener: NetworkListener,
    final override val configuration: Queue.Configuration,
    private val store: JsonStorage
) : Queue {
    private val module = SerializersModule {
        contextual(InstantIso8601Serializer)
        polymorphic(JobRule::class) {
            subclass(DelayRule::class, DelayRule.serializer())
            subclass(PeriodicRule::class, PeriodicRule.serializer())
            subclass(RetryRule::class, RetryRule.serializer())
            subclass(TimeoutRule::class, TimeoutRule.serializer())
            subclass(UniqueRule::class, UniqueRule.serializer())
            subclass(PersistenceRule::class, PersistenceRule.serializer())
        }
    }
    private val format = Json { serializersModule = module + serializers }

    val jobEventListener = MutableSharedFlow<JobEvent>(extraBufferCapacity = Int.MAX_VALUE)

    /**
     * Scheduled jobs
     */
    private val running = atomic(mutableMapOf<UUID, kotlinx.coroutines.Job>())
    private val queue = atomic(mutableListOf<Job>())

    override val jobs: List<JobData>
        get() = queue.value
    override val numberOfJobs: Int
        get() = jobs.count()

    /**
     * Semaphore to limit concurrency
     */
    private val lock = Semaphore(permits = configuration.maxConcurrency, acquiredPermits = 0)

    /**
     * Mutex to suspend queue operations during cancellation
     */
    private val isCancelling = Mutex()

    private var scheduler: kotlinx.coroutines.Job? = null

    init {
        if (configuration.startsAutomatically) {
            start()
        }
    }

    suspend fun schedule(task: () -> Task, configure: JobInfo.() -> JobInfo = { JobInfo() }) {
        schedule(task = task(), configure = configure)
    }

    suspend fun <Data> schedule(
        data: Data,
        task: (Data) -> DataTask<Data>,
        configure: JobInfo.() -> JobInfo = { JobInfo() }
    ) {
        schedule(task = task(data), configure = configure)
    }

    suspend fun schedule(task: Task, configure: JobInfo.() -> JobInfo = { JobInfo() }) {
        val info = configure(JobInfo()).apply {
            rules.forEach { rule ->
                rule.mutating(jobInfo = this)
            }
        }

        val job = Job(task = task, info = info)

        schedule(job).apply {
            jobEventListener.emit(JobEvent.DidSchedule(job))
        }
    }

    private suspend fun schedule(job: Job) = try {
        job.info.rules.forEach {
            it.willSchedule(queue = this, jobContext = job)
        }

        if (job.info.shouldPersist) {
            store.set(id = job.id.toString(), json = format.encodeToString(job))
        }

        queue.value = queue.value
            .plus(listOf(job))
            .sortedBy { queueJob ->
                queueJob.startTime
            }
            .toMutableList()
    } catch (e: Throwable) {
        jobEventListener.emit(JobEvent.DidThrowOnSchedule(e))
    }

    private val delegate = JobDelegate()

    /**
     * Starts enqueuing scheduled jobs
     */
    fun start() {
        if (scheduler != null) return
        scheduler = CoroutineScope(Dispatchers.Default).launch {
            launch {
                delegate.onEvent.collect { event ->
                    when (event) {
                        is JobEvent.DidCancel -> {
                            cancel(event.job.id)
                        }
                        is JobEvent.ShouldRepeat -> {
                            schedule(event.job).apply {
                                jobEventListener.emit(JobEvent.DidScheduleRepeat(event.job))
                            }
                        }
                        else -> jobEventListener.emit(event)
                    }
                }
            }
            restore()
            while (isActive) {
                if (isCancelling.isLocked) continue
                if (queue.value.isEmpty()) continue
                if (queue.value.first().startTime.minus(Clock.System.now()) > 0.seconds) continue
                lock.acquire()
                val job = queue.value.removeFirst()
                job.delegate = delegate
                running.value[job.id] = configuration.scope.launch {
                    try {
                        var shouldRunJob = false
                        try {
                            withTimeout(job.info.networkRuleTimeout) NetworkRuleTimeout@{
                                networkListener.currentNetworkState.collect { currentNetworkState ->
                                    val isNetworkRuleSatisfied =
                                        networkListener.isNetworkRuleSatisfied(
                                            jobInfo = job.info,
                                            currentNetworkState = currentNetworkState
                                        )
                                    if (isNetworkRuleSatisfied) {
                                        shouldRunJob = true
                                        jobEventListener.emit(JobEvent.NetworkRuleSatisfied(job))
                                        this@NetworkRuleTimeout.cancel("Network rule satisfied")
                                    }
                                }
                            }
                        } catch (e: CancellationException) {
                            if (shouldRunJob) {
                                jobEventListener.emit(JobEvent.WillRun(job))
                                withTimeout(job.info.jobTimeout) {
                                    val result = job.run()
                                    jobEventListener.emit(result)
                                }
                            } else throw NetworkRuleTimeoutException("Timeout exceeded for the network.")
                        }
                    } catch (e: CancellationException) {
                        when (e) {
                            is TimeoutCancellationException -> {
                                jobEventListener.emit(JobEvent.JobTimeout(job))
                            }
                            is NetworkRuleTimeoutException -> {
                                jobEventListener.emit(JobEvent.NetworkRuleTimeout(job))
                            }
                        }
                        jobEventListener.emit(JobEvent.DidCancel(job))
                    } finally {
                        if (job.info.shouldPersist) {
                            store.remove(job.id.toString())
                        }
                        running.value[job.id]?.cancel()
                        running.value.remove(job.id)
                        lock.release()
                        networkListener.stopMonitoring()
                    }
                }
            }
        }
    }

    /**
     * Stops enqueuing scheduled jobs
     */
    fun stop() {
        scheduler?.cancel()
        scheduler = null
    }

    /**
     * Removes all scheduled jobs
     */
    suspend fun clear() {
        clear(true)
    }

    internal suspend fun clear(clearStore: Boolean = true) {
        isCancelling.withLock {
            queue.value.clear()
            running.value.clear()
            configuration.scope.coroutineContext.cancelChildren()
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
            queue.value.firstOrNull { it.id == id }?.let { job ->
                queue.value.remove(job)
                jobEventListener.emit(JobEvent.DidCancel(job))
            } ?: running.value[id]?.cancel()
        }
    }

    /**
     * Restores all persisted jobs. Ensures job not already in queue.
     */
    internal suspend fun restore() {
        store.keys.forEach { key ->
            val job: Job = format.decodeFromString(store.get(key))
            if (queue.value.none { it.id == job.id }) {
                schedule(job)
            }
        }
    }
}
