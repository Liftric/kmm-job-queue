package com.liftric.persisted.queue

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class Task(
    private val job: Job,
    info: TaskInfo
) {
    val id: UUID = UUID::class.instance()
    val tag: String? = info.tag
    val rules: Set<JobRule> = info.rules

    var delegate: TaskDelegate? = null

    suspend fun run() {
        val event: Event = coroutineScope {
            val result = CompletableDeferred<Event>()

            val delegate = JobDelegate(job)
            delegate.onEvent = { event ->
                result.complete(event)
            }

            rules.forEach { it.willRun(this@Task) }

            job.body(delegate)

            result
        }.await()

        when(event) {
            is Event.DidEnd -> {
                terminate()
            }
            is Event.DidCancel -> {
                terminate()
            }
            else -> Unit
        }

        delegate?.broadcast(event)

        rules.forEach { it.willRemove(this@Task, event) }
    }

    suspend fun terminate() {
        delegate?.broadcast(Event.DidTerminate(this))
    }
}

class TaskDelegate {
    var onEvent: (suspend (Event) -> Unit)? = null

    suspend fun broadcast(event: Event) {
        onEvent?.invoke(event)
    }
}
