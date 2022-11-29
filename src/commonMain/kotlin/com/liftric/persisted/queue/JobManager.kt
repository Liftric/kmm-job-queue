package com.liftric.persisted.queue

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class JobManager(
    val factory: JobFactory,
    configuration: Queue.Configuration? = null
) {
    val queue = OperationsQueue(configuration)
    val onEvent = MutableSharedFlow<Event>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    private val delegate = JobDelegate()
    private var listener: kotlinx.coroutines.Job? = null

    init {
        delegate.onExit = { /* Do something */ }
        delegate.onRepeat = { repeat(it) }
        delegate.onEvent = { onEvent.emit(it) }
    }

    suspend fun start() {
        listener = queue.dispatcher {
            launch {
                while (true) {
                    delay(1000L)
                    if (queue.isRunning.isLocked) break
                    if (queue.operations.value.isEmpty()) break
                    if ((queue.operations.value.first().startTime) <= Clock.System.now()) {
                        queue.isRunning.withLock {
                            run()
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        listener?.cancel()
        listener = null
    }

    suspend inline fun <reified T: Job> schedule(init: JobInfo.() -> JobInfo) {
        try {
            val info = init(JobInfo()).apply {
                rules.forEach { it.mutating(this) }
            }

            val job = factory.create(T::class, info.params)

            val operation = Operation(job, info)

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
            operation.rules.forEach {
                it.willSchedule(queue, operation)
            }

            onEvent.emit(Event.DidScheduleRepeat(operation))

            queue.add(operation)
        } catch (error: Error) {
            onEvent.emit(Event.DidThrowRepeat(error))
        }
    }

    private suspend fun run() {
        queue.dispatcher {
            launch {
                val task = queue.removeFirst()
                task.delegate = delegate
                task.run()
            }
        }
    }
}
