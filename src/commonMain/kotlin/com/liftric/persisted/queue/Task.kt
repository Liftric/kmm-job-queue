package com.liftric.persisted.queue

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class Task(val job: Job) {
    var delegate: TaskDelegate? = null

    suspend fun run() {
        val event: JobDelegate.Event = coroutineScope {
            val result = CompletableDeferred<JobDelegate.Event>()

            val delegate = JobDelegate(job)
            delegate.onEvent = { event ->
                result.complete(event)
            }

            job.rules.forEach { it.willRun(this@Task) }

            job.body(delegate)

            result
        }.await()

        when(event) {
            is JobDelegate.Event.DidEnd -> {
                println("Delegate.Event.DidEnd")
                terminate()
            }
            is JobDelegate.Event.DidCancel -> {
                println("Delegate.Event.DidCancel: ${event.error.message}")
                terminate()
            }
            is JobDelegate.Event.DidFail -> {
                println("Delegate.Event.DidFail: ${event.error.message}")
            }
        }

        job.rules.forEach { it.willRemove(this@Task, event) }
    }

    suspend fun terminate() {
        delegate?.terminate()
    }
}

class TaskDelegate{
    sealed class Event {
        object Terminate: Event()
    }

    var onEvent: (suspend (Event) -> Unit)? = null

    suspend fun terminate() {
        onEvent?.invoke(Event.Terminate)
    }
}
