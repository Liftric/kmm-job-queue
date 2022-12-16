package com.liftric.persisted.queue

import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
data class TestData(val testResultId: String)

@Serializable
data class TestTask(override val data: TestData): DataTask<TestData> {
    override suspend fun body() {  }
}

@Serializable
class TestErrorTask: Task() {
    override suspend fun body() {  throw Error("Oh shoot!") }
    override suspend fun onRepeat(cause: Throwable): Boolean = cause is Error
}

@Serializable
class LongRunningTask: Task() {
    override suspend fun body() { delay(10.seconds) }
}
