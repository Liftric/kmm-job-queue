package com.liftric.persisted.queue

import kotlinx.coroutines.delay

class TestTask(override val params: Map<String, Any>): Task {
    private val testResultId: String by params

    override suspend fun body() { }
}

class TestErrorTask(override val params: Map<String, Any>): Task {
    override suspend fun body() {
        throw Error("Oh shoot!")
    }

    override suspend fun onRepeat(cause: Throwable): Boolean {
        return cause is Error
    }
}

class LongRunningTask(override val params: Map<String, Any>): Task {
    override suspend fun body() {
        delay(10000L)
    }
}
