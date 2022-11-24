package com.liftric.persisted.queue

interface Context<T: Job> {
    val name: String
    suspend fun done()
    suspend fun fail(error: Error)
    suspend fun cancel(error: Error)
}

class JobDelegate<T: Job>(job: T): Context<T> {
    override val name: String = job::class.simpleName!!

    sealed class Event {
        object DidEnd: Event()
        data class DidCancel(val error: Error): Event()
        data class DidFail(val error: Error): Event()
    }

    var onEvent: ((Event) -> Unit)? = null

    override suspend fun done() {
        onEvent?.invoke(Event.DidEnd)
    }

    override suspend fun fail(error: Error) {
        onEvent?.invoke(Event.DidFail(error))
    }

    override suspend fun cancel(error: Error) {
        onEvent?.invoke(Event.DidCancel(error))
    }
}
