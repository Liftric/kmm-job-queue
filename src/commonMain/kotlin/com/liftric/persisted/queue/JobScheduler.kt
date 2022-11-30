package com.liftric.persisted.queue

import kotlinx.coroutines.flow.*

class JobScheduler(
    val factory: TaskFactory,
    configuration: Queue.Configuration? = null
) {
    val queue = JobQueue(configuration)

    @PublishedApi
    internal val delegate = JobDelegate()

    val onEvent = MutableSharedFlow<Event>(extraBufferCapacity = Int.MAX_VALUE)

    init {
        delegate.onExit = { /* Do something */ }
        delegate.onRepeat = { repeat(it) }
        delegate.onEvent = { onEvent.emit(it) }
    }

    suspend fun start() {
        queue.start()
    }

    fun stop() {
        queue.cancel()
    }

    suspend inline fun <reified T: Task> schedule(init: JobInfo.() -> JobInfo) {
        try {
            val info = init(JobInfo()).apply {
                rules.forEach { it.mutating(this) }
            }

            val task = factory.create(T::class, info.params)

            val job = Job(task, info)
            job.delegate = delegate

            job.rules.forEach {
                it.willSchedule(queue, job)
            }

            onEvent.emit(Event.DidSchedule(job))

            queue.add(job)
        } catch (error: Error) {
            onEvent.emit(Event.DidThrowOnSchedule(error))
        }
    }

    private suspend fun repeat(job: Job) {
        try {
            job.delegate = delegate

            job.rules.forEach {
                it.willSchedule(queue, job)
            }

            onEvent.emit(Event.DidScheduleRepeat(job))

            queue.add(job)
        } catch (error: Error) {
            onEvent.emit(Event.DidThrowOnRepeat(error))
        }
    }
}
