package com.liftric.job.queue

import com.liftric.job.queue.rules.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

expect class JobQueue: AbstractJobQueue
abstract class AbstractJobQueue(
    serializers: SerializersModule,
    final override val configuration: Queue.Configuration,
    private val store: JsonStorage
): Queue {
    val onEvent = MutableSharedFlow<JobEvent>(extraBufferCapacity = Int.MAX_VALUE)

    override val jobs: List<JobContext>
        get() = queue.value
    override val numberOfJobs: Int
        get() = queue.value.count()

    private val module = SerializersModule {
        contextual(UUIDSerializer)
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

    /**
     * Scheduled jobs
     */
    private val queue = atomic(mutableListOf<Job>())

    /**
     * Reference to the running jobs
     */
    private val running = atomic(mutableMapOf<String, kotlinx.coroutines.Job>())

    /**
     * Semaphore to limit concurrency
     */
    private val lock = Semaphore(configuration.maxConcurrency, 0)

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
        schedule(task(), configure)
    }

    suspend fun <Data> schedule(data: Data, task: (Data) -> DataTask<Data>, configure: JobInfo.() -> JobInfo = { JobInfo() }) {
        schedule(task(data), configure)
    }

    suspend fun schedule(task: Task, configure: JobInfo.() -> JobInfo = { JobInfo() }) {
        val info = configure(JobInfo()).apply {
            rules.forEach { it.mutating(this) }
        }

        val job = Job(task, info)

        schedule(job).apply {
            onEvent.emit(JobEvent.DidSchedule(job))
        }
    }

    private suspend fun schedule(job: Job) = try {
        job.info.rules.forEach {
            it.willSchedule(this, job)
        }

        if (job.info.shouldPersist) {
            store.set(job.id.toString(), format.encodeToString(job))
        }

        queue.value = queue.value.plus(listOf(job)).sortedBy { it.startTime }.toMutableList()
    } catch (e: Throwable) {
        onEvent.emit(JobEvent.DidThrowOnSchedule(e))
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
                                onEvent.emit(JobEvent.DidScheduleRepeat(event.job))
                            }
                        }
                        else -> onEvent.emit(event)
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
                running.value[job.id.toString()] = configuration.scope.launch {
                    try {
                        withTimeout(job.info.timeout) {
                            onEvent.emit(JobEvent.WillRun(job))
                            val result = job.run()
                            onEvent.emit(result)
                        }
                    } catch (e: CancellationException) {
                        onEvent.emit(JobEvent.DidCancel(job))
                    } finally {
                        if (job.info.shouldPersist) {
                            store.remove(job.id.toString())
                        }
                        running.value[job.id.toString()]?.cancel()
                        running.value.remove(job.id.toString())
                        lock.release()
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
            if (clearStore) { store.clear() }
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
                onEvent.emit(JobEvent.DidCancel(job))
            } ?: running.value[id.toString()]?.cancel()
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
