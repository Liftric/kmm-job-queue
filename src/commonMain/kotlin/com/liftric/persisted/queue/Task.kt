package com.liftric.persisted.queue

interface Task {
    @Throws(Throwable::class)
    suspend fun body()
    suspend fun onRepeat(cause: Throwable): Boolean = false
}
