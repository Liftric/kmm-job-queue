package com.liftric.persisted.queue

interface Context<T: Job> {
    val name: String
    suspend fun done()
    suspend fun fail(error: Error)
    suspend fun cancel(error: Error)
}

class JobDelegate<T: Job>(private val job: T): Context<T> {
    override val name: String = job::class.simpleName!!

    var onEvent: ((Event) -> Unit)? = null

    override suspend fun done() {
        onEvent?.invoke(Event.DidEnd(job))
    }

    override suspend fun fail(error: Error) {
        onEvent?.invoke(Event.DidFail(job, error))
    }

    override suspend fun cancel(error: Error) {
        onEvent?.invoke(Event.DidCancel(job, error))
    }
}
