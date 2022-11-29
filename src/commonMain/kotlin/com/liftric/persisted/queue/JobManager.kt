package com.liftric.persisted.queue

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

class JobManager(
    val factory: JobFactory,
    configuration: Queue.Configuration? = null
) {
    val queue = OperationsQueue(configuration)
    val onEvent = MutableSharedFlow<Event>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    @PublishedApi
    internal val delegate = JobDelegate()

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

    suspend inline fun <reified T: Job> schedule(init: JobInfo.() -> JobInfo) {
        try {
            val info = init(JobInfo()).apply {
                rules.forEach { it.mutating(this) }
            }

            val job = factory.create(T::class, info.params)

            val operation = Operation(job, info)
            operation.delegate = delegate

            operation.rules.forEach {
                it.willSchedule(queue, operation)
            }

            onEvent.emit(Event.DidSchedule(operation))

            queue.add(operation)
        } catch (error: Error) {
            onEvent.emit(Event.DidThrowSchedule(error))
        }
    }

    private suspend fun repeat(operation: Operation) {
        try {
            operation.delegate = delegate

            operation.rules.forEach {
                it.willSchedule(queue, operation)
            }

            onEvent.emit(Event.DidScheduleRepeat(operation))

            queue.add(operation)
        } catch (error: Error) {
            onEvent.emit(Event.DidThrowRepeat(error))
        }
    }
}
