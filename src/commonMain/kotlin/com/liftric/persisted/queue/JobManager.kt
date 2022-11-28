package com.liftric.persisted.queue

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock


class JobManager(val factory: JobFactory) {
    val queue = Queue()
    val delegate = TaskDelegate()

    val onEvent = MutableSharedFlow<Event>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    init {
        delegate.onExit = { }
        delegate.onRepeat = { task ->
            repeat(task)
        }
        delegate.onEvent = { event ->
            onEvent.emit(event)
        }
    }

    suspend inline fun <reified T: Job> schedule(init: TaskInfo.() -> TaskInfo) {
        try {
            val info = init(TaskInfo()).apply {
                rules.forEach { it.mutating(this) }
            }

            val job = factory.create(T::class, info.params)

            val task = Task(job, info)

            task.rules.forEach {
                it.willSchedule(queue, task)
            }

            onEvent.emit(Event.DidSchedule(task))

            queue.tasks.value.add(task)

            queue.tasks.value.sortBy { it.startTime }
        } catch (error: Error) {
            onEvent.emit(Event.Error(error))
        }
    }

    suspend fun start() {
        queue.dispatcher {
             launch {
                 while (true) {
                     delay(1000L)
                     if (queue.isRunning.isLocked) break
                     if (queue.tasks.value.isEmpty()) break
                     if ((queue.tasks.value.first().startTime) <= Clock.System.now()) {
                         queue.isRunning.withLock {
                             run()
                         }
                     }
                 }
             }
        }
    }

    private suspend fun repeat(task: Task) {
        try {
            val newTask = task

            newTask.rules.forEach {
                it.willSchedule(queue, newTask)
            }

            onEvent.emit(Event.DidRepeat(newTask))

            queue.tasks.value.add(newTask)

            queue.tasks.value.sortBy { it.startTime }
        } catch (error: Error) {
            onEvent.emit(Event.Error(error))
        }
    }

    private suspend fun run() {
        queue.dispatcher {
            launch {
                val task = queue.tasks.value.removeFirst()
                task.delegate = delegate
                task.run()
            }
        }
    }
}
