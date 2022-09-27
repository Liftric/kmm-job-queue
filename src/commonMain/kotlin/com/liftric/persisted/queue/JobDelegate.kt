package com.liftric.persisted.queue

interface Context<T: Job> {
    val name: String
    suspend fun done()
    suspend fun fail(exception: Exception)
    suspend fun cancel()
}

class Delegate<T: Job>(job: T): Context<T> {
    override val name: String = job::class.simpleName!!

    sealed class Event {
        object DidEnd: Event()
        object DidCancel: Event()
        data class DidFail(val throwable: Throwable): Event()
    }

    var onEvent: ((Event) -> Unit)? = null

    override suspend fun done() {
        onEvent?.invoke(Event.DidEnd)
    }

    override suspend fun fail(exception: Exception) {
        onEvent?.invoke(Event.DidFail(exception))
    }

    override suspend fun cancel() {
        onEvent?.invoke(Event.DidCancel)
    }
}
