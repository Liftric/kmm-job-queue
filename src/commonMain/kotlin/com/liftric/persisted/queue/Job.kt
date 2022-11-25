package com.liftric.persisted.queue

abstract class Job {
    abstract val params: Map<String, Any>
    abstract suspend fun body(context: Context<Job>)
}
