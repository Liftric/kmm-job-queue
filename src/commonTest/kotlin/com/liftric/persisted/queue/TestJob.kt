package com.liftric.persisted.queue

class TestJob(override val params: Map<String, Any>): Job {
    private val testResultId: String by params

    override suspend fun body() {
        println("testResultId=$testResultId")
    }
}

class TestErrorJob(override val params: Map<String, Any>): Job {
    override suspend fun body() {
        throw Error("Oh shoot!")
    }
}
