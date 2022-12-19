package com.liftric.persisted.queue

import com.liftric.persisted.queue.rules.*
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.*
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
    private val settings: Settings
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
        settings = settings,
        format = format,
        configuration = configuration ?: Queue.DefaultConfiguration,
        onRestore = { job ->
            job.delegate = delegate
        }
    )

    init {
        delegate.onExit = { job ->
            if (job.info.shouldPersist) {
                settings.remove(job.id.toString())
            }
        }
        delegate.onRepeat = { job ->
            repeat(job)
        }
        delegate.onEvent = { event ->
            onEvent.emit(event)
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

    private suspend fun schedule(job: Job) {
        job.delegate = delegate

        job.info.rules.forEach {
            it.willSchedule(queue, job)
        }

        if (job.info.shouldPersist) {
            settings[job.id.toString()] = format.encodeToString(job)
        }

        queue.add(job)
    }

    private suspend fun repeat(job: Job) = try {
        job.delegate = delegate

        schedule(job).apply {
            onEvent.emit(JobEvent.DidScheduleRepeat(job))
        }
    } catch (error: Error) {
        onEvent.emit(JobEvent.DidThrowOnRepeat(error))
    }
}
