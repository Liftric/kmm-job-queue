package com.liftric.persisted.queue

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

    suspend inline fun <reified T: Job> schedule(init: JobInfo.() -> JobInfo) {
        try {
            val info = init(JobInfo()).apply {
                rules.forEach { it.mapping(this) }
            }

            val job = factory.create(T::class, info.params)
            job.tag = info.tag
            job.rules.addAll(info.rules)

            val task = Task(job)

            job.rules.forEach { it.willSchedule(queue, task) }

            queue.tasks.add(task)

            println("Added task id=${task.job.id}, tag=${task.job.tag}")
        } catch (e: Exception) {
            println(e.message)
        }
    }

    suspend fun start() = next()

    suspend fun next() {
        if (queue.tasks.isEmpty()) return
        withContext(queue.dispatcher) {
            val task = queue.tasks.removeFirst()
            task.delegate = delegate
            task.run()
        }
    }
}
