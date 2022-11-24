package com.liftric.persisted.queue

abstract class Job {
    val id: UUID = UUID::class.instance()
    var tag: String? = null
    val rules: MutableSet<JobRule> = mutableSetOf()
    abstract val params: Map<String, Any>
    abstract suspend fun body(context: Context<Job>)
}
