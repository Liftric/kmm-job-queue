package com.liftric.persisted.queue

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class JobManagerTests {
    @Test
    fun testSchedule() = runBlocking {
        val jobManager = JobManager(TestFactory())
        val id = UUID::class.instance().toString()

        jobManager.schedule<TestJob>(
            rules = setOf(UniqueRule(id)),
            params = mapOf(
                "testResultId" to id
            )
        )

        jobManager.schedule<TestJob>(
            rules = setOf(UniqueRule(id)),
            params = mapOf(
                "testResultId" to id
            )
        )

        assertEquals(1, jobManager.queue.operations.count())

        jobManager.next()

        assertEquals(0, jobManager.queue.operations.count())
    }
}
