package com.liftric.persisted.queue

import com.liftric.persisted.queue.rules.RetryLimit
import com.liftric.persisted.queue.rules.retry
import com.liftric.persisted.queue.rules.unique
import kotlinx.coroutines.*
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

        var count = 0
        val job = async {
            jobManager.onEvent.collect {
                println(it)
                if (it is Event.DidRetry) {
                    count += 1
                }
            }
        }

        jobManager.schedule<TestErrorJob> {
            rules {
                retry(RetryLimit.Limited(3), delay = 10.seconds)
            }
        }

        jobManager.start()
        delay(1000L)
        job.cancel()
        assertEquals(3, count)
    }
}
