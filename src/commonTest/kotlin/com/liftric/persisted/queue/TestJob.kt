package com.liftric.persisted.queue

class TestJob(override val params: Map<String, Any>): Job() {
    private val testResultId: String by params

    override suspend fun body(context: Context<Job>) = try {
        println(testResultId)
        context.done()
    } catch (e: Error) {
        context.cancel(e)
    }
}

class TestErrorJob(override val params: Map<String, Any>): Job() {
    override suspend fun body(context: Context<Job>) = try {
        context.fail(Error("Oh shoot!"))
    } catch (e: Error) {
        context.cancel(e)
    }
}
