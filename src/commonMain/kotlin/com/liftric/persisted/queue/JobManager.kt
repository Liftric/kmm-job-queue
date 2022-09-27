package com.liftric.persisted.queue

import kotlinx.coroutines.*

class JobManager(val factory: JobFactory) {
    val queue = Queue()

    suspend inline fun <reified T: Job> schedule(rules: List<JobRule>, params: Map<String, Any>) {
        try {
            val job = factory.create(T::class, params)

            val operation = rules.fold(Operation(rules, job)) { operation, rule ->
                rule.mapping(operation)
            }

            rules.forEach {
                it.willSchedule(queue, operation)
            }

            queue.operations.add(operation)
        } catch (e: Exception) {
            println(e.message)
        }
    }

    suspend inline fun next() {
        val operation = queue.operations.removeFirst()

        val event: Deferred<Delegate.Event> = withContext(Dispatchers.Default) {
            val result = CompletableDeferred<Delegate.Event>()

            val delegate = Delegate(operation.job)
            delegate.onEvent = { event ->
                result.complete(event)
            }

            operation.rules.forEach { it.willRun(queue, operation) }

            operation.job.body(delegate)

            result
        }

        when(event.await()) {
            is Delegate.Event.DidEnd -> {
                println("Delegate.Event.DidEnd")
            }
            is Delegate.Event.DidCancel -> {
                println("Delegate.Event.DidCancel")
            }
            is Delegate.Event.DidFail -> {
                println("Delegate.Event.DidFail")
            }
        }

        operation.rules.forEach { it.willRemove(operation) }
    }
}
