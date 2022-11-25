package com.liftric.persisted.queue

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JobManager(val factory: JobFactory) {
    val queue = Queue()
    private val delegate = TaskDelegate()

    init {
        delegate.onEvent = { event ->
            when (event) {
                TaskDelegate.Event.Terminate -> {
                    next()
                }
            }
        }
    }

    suspend inline fun <reified T: Job> schedule(init: TaskInfo.() -> TaskInfo) {
        try {
            val info = init(TaskInfo()).apply {
                rules.forEach { it.mutating(this) }
            }

            val job = factory.create(T::class, info.params)

            val task = Task(job, info)

            task.rules.forEach { it.willSchedule(queue, task) }

            queue.tasks.add(task)

            println("Added task id=${task.id}, tag=${task.tag}")
        } catch (e: Exception) {
            println(e.message)
        }
    }

    suspend fun start() = next()

    private suspend fun next() {
        if (queue.tasks.isEmpty()) return
        withContext(queue.dispatcher) {
            launch {
                val task = queue.tasks.removeFirst()
                task.delegate = delegate
                task.run()
            }
        }
    }
}
