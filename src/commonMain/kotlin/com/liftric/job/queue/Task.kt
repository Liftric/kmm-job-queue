package com.liftric.job.queue

interface Task {
    @Throws(Throwable::class)
    suspend fun body()
    suspend fun onRepeat(cause: Throwable): Boolean = false
}

interface DataTask<Data>: Task {
    val data: Data
}
