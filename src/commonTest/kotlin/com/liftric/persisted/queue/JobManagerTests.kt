package com.liftric.persisted.queue

import com.liftric.persisted.queue.rules.RetryLimit
import com.liftric.persisted.queue.rules.retry
import com.liftric.persisted.queue.rules.unique
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class JobManagerTests {
    @Test
    fun testSchedule() = runBlocking {
        val jobFactory = TestFactory()
        val jobManager = JobManager(jobFactory)
        val id = UUID::class.instance().toString()

        jobManager.schedule<TestJob> {
            rules {
                unique(id)
            }
            params("testResultId" to id)
        }

        jobManager.schedule<TestJob> {
            rules {
                unique(id)
            }
            params("testResultId" to id)
        }

        assertEquals(1, jobManager.queue.tasks.count())

        jobManager.start()

        assertEquals(0, jobManager.queue.tasks.count())
    }

    @Test
    fun testRetry() = runBlocking {
        val jobFactory = TestFactory()
        val jobManager = JobManager(jobFactory)

        jobManager.schedule<TestErrorJob> {
            rules {
                retry(RetryLimit.Limited(3), delay = 10.seconds)
            }
        }

        jobManager.start()
    }
}
