package com.liftric.persisted.queue

import kotlinx.coroutines.flow.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializerOrNull

class JobScheduler(configuration: Queue.Configuration? = null) {
    val queue = JobQueue(configuration)
    val onEvent = MutableSharedFlow<JobEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val delegate = JobDelegate()

    init {
        delegate.onExit = { /* Do something */ }
        delegate.onRepeat = { repeat(it) }
        delegate.onEvent = { onEvent.emit(it) }
    }

    suspend fun schedule(task: () -> DataTask<*>, configure: JobInfo.() -> JobInfo = { JobInfo() }) {
        schedule(task(), configure)
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun schedule(task: DataTask<*>, configure: JobInfo.() -> JobInfo = { JobInfo() }) = try {
        val info = configure(JobInfo()).apply {
            rules.forEach { it.mutating(this) }
        }

        if (task.data!!::class.serializerOrNull() == null) throw Exception("Data must be serializable")

        val job = Job(task, info)
        job.delegate = delegate

        job.rules.forEach {
            it.willSchedule(queue, job)
        }

        queue.add(job).apply {
            onEvent.emit(JobEvent.DidSchedule(job))
        }
    } catch (error: Error) {
        onEvent.emit(JobEvent.DidThrowOnSchedule(error))
    }

    private suspend fun repeat(job: Job) = try {
        job.delegate = delegate

        job.rules.forEach {
            it.willSchedule(queue, job)
        }

        onEvent.emit(JobEvent.DidScheduleRepeat(job))

        queue.add(job)
    } catch (error: Error) {
        onEvent.emit(JobEvent.DidThrowOnRepeat(error))
    }
}
