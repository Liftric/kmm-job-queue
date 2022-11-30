package com.liftric.persisted.queue

class TestTask(override val params: Map<String, Any>): Task {
    private val testResultId: String by params

    override suspend fun body() {
        println("testResultId=$testResultId")
    }
}

class TestErrorTask(override val params: Map<String, Any>): Task {
    override suspend fun body() {
        throw Error("Oh shoot!")
    }
}
