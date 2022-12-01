package com.liftric.persisted.queue

interface Task {
    val params: Map<String, Any>
    @Throws(Throwable::class)
    suspend fun body()
    suspend fun onRepeat(cause: Throwable): Boolean = false
}
