package com.liftric.persisted.queue

interface Job {
    val params: Map<String, Any>
    @Throws(Throwable::class)
    suspend fun body()
}
