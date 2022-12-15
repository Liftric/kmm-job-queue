package com.liftric.persisted.queue

import kotlinx.serialization.Serializable

@Serializable
abstract class DataTask<Data>(val data: Data) {
    @Throws(Throwable::class)
    abstract suspend fun body()
    open suspend fun onRepeat(cause: Throwable): Boolean = false
}

@Serializable
abstract class Task: DataTask<Unit>(Unit)
