package com.liftric.persisted.queue

import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
data class TestData(val testResultId: String)

class TestTask(data: TestData): DataTask<TestData>(data) {
    override suspend fun body() {  }
}

class TestErrorTask: Task() {
    override suspend fun body() {  throw Error("Oh shoot!") }
    override suspend fun onRepeat(cause: Throwable): Boolean = cause is Error
}

class LongRunningTask: Task() {
    override suspend fun body() { delay(10000L) }
}
