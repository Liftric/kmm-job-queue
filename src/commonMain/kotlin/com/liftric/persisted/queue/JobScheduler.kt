package com.liftric.persisted.queue

import kotlinx.coroutines.flow.*

class JobScheduler(
    val factory: TaskFactory,
    configuration: Queue.Configuration? = null
) {
    val queue = JobQueue(configuration)

    @PublishedApi
    internal val delegate = JobDelegate()

    val onEvent = MutableSharedFlow<JobEvent>(extraBufferCapacity = Int.MAX_VALUE)

    init {
        delegate.onExit = { /* Do something */ }
        delegate.onRepeat = { repeat(it) }
        delegate.onEvent = { onEvent.emit(it) }
    }

    suspend inline fun <reified T: Task> schedule() {
        schedule<T> { this }
    }

    suspend inline fun <reified T: Task> schedule(init: JobInfo.() -> JobInfo) = try {
        val info = init(JobInfo()).apply {
            rules.forEach { it.mutating(this) }
        }

        val task = factory.create(T::class, info.params)

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
