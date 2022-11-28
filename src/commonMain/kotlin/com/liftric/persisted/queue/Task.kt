package com.liftric.persisted.queue

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class Task(
    val id: UUID,
    val job: Job,
    val tag: String?,
    val rules: Set<JobRule>,
    val startTime: Instant = Clock.System.now()
) {
    var delegate: TaskDelegate? = null

    constructor(job: Job, info: TaskInfo) : this (UUID::class.instance(), job, info.tag, info.rules)

    suspend fun run() {
        coroutineScope {
            ensureActive()

            rules.forEach { it.willRun(this@Task) }

            val event = try {
                job.body()
                Event.DidEnd(job)
            } catch (e: Error) {
                terminate()
                Event.DidFail(job, e)
            }

            delegate?.broadcast(event)

            rules.forEach { it.willRemove(this@Task, event) }
        }
    }

    suspend fun terminate() {
        delegate?.exit()
    }

    suspend fun repeat(task: Task) {
        delegate?.repeat(task)
    }

    suspend fun broadcast(event: Event) {
        delegate?.broadcast(event)
    }
}

class TaskDelegate {
    var onExit: (suspend () -> Unit)? = null
    var onRepeat: (suspend (Task) -> Unit)? = null
    var onEvent: (suspend (Event) -> Unit)? = null

    suspend fun broadcast(event: Event) {
        onEvent?.invoke(event)
    }

    suspend fun exit() {
        onExit?.invoke()
    }

    suspend fun repeat(task: Task) {
        onRepeat?.invoke(task)
    }
}
