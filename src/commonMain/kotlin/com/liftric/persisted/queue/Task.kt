package com.liftric.persisted.queue

interface DataTask<Data> {
    val data: Data
    @Throws(Throwable::class)
    suspend fun body()
    suspend fun onRepeat(cause: Throwable): Boolean = false
}

abstract class Task: DataTask<Unit> {
    override val data: Unit = Unit
}
