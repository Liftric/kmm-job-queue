package com.liftric.persisted.queue

import com.liftric.persisted.queue.rules.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic

expect class JobScheduler: AbstractJobScheduler
abstract class AbstractJobScheduler(
    serializers: SerializersModule,
    configuration: Queue.Configuration?,
    private val store: JsonStorage
) {
    private val delegate = JobDelegate()
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

    val onEvent = MutableSharedFlow<JobEvent>(extraBufferCapacity = Int.MAX_VALUE)
    val queue = JobQueue(
        store = store,
        format = format,
        configuration = configuration ?: Queue.DefaultConfiguration,
        onRestore = { job -> job.apply { job.delegate = delegate } }
    )

    init {
        CoroutineScope(Dispatchers.Default).launch {
            delegate.onEvent.collect { event ->
                when (event) {
                    is JobEvent.DidCancel -> {
                        if (event.job.info.shouldPersist) {
                            store.remove(event.job.id.toString())
                        }
                        queue.cancel(event.job.id)
                        onEvent.emit(event)
                    }
                    is JobEvent.DidExit -> {
                        if (event.job.info.shouldPersist) {
                            store.remove(event.job.id.toString())
                        }
                    }
                    is JobEvent.ShouldRepeat -> {
                        repeat(event.job)
                    }
                    else -> onEvent.emit(event)
                }
            }
        }
    }

    suspend fun schedule(task: () -> Task, configure: JobInfo.() -> JobInfo = { JobInfo() }) {
        schedule(task(), configure)
    }

    suspend fun <Data> schedule(data: Data, task: (Data) -> DataTask<Data>, configure: JobInfo.() -> JobInfo = { JobInfo() }) {
        schedule(task(data), configure)
    }

    suspend fun schedule(task: Task, configure: JobInfo.() -> JobInfo = { JobInfo() }) = try {
        val info = configure(JobInfo()).apply {
            rules.forEach { it.mutating(this) }
        }

        val job = Job(task, info)

        schedule(job).apply {
            onEvent.emit(JobEvent.DidSchedule(job))
        }
    } catch (error: Error) {
        onEvent.emit(JobEvent.DidThrowOnSchedule(error))
    }

    private suspend fun repeat(job: Job) = try {
        schedule(job).apply {
            onEvent.emit(JobEvent.DidScheduleRepeat(job))
        }
    } catch (error: Error) {
        onEvent.emit(JobEvent.DidThrowOnRepeat(error))
    }

    private suspend fun schedule(job: Job) {
        job.delegate = delegate

        job.info.rules.forEach {
            it.willSchedule(queue, job)
        }

        if (job.info.shouldPersist) {
            store.set(job.id.toString(), format.encodeToString(job))
        }

        queue.add(job)
    }
}
