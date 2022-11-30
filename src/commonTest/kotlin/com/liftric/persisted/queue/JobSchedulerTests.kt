package com.liftric.persisted.queue

import com.liftric.persisted.queue.rules.*
import kotlinx.coroutines.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class JobSchedulerTests {
    @Test
    fun testSchedule() = runBlocking {
        val factory = TestFactory()
        val scheduler = JobScheduler(factory)
        val id = UUID::class.instance().toString()
        val job = async {
            scheduler.onEvent.collect {
                println(it)
            }
        }

        scheduler.schedule<TestTask> {
            rules {
                delay(1.seconds)
                unique(id)
            }
            params("testResultId" to id)
        }

        scheduler.schedule<TestTask> {
            rules {
                unique(id)
            }
            params("testResultId" to id)
        }

        assertEquals(1, scheduler.queue.jobs.count())

        scheduler.queue.start()

        assertEquals(0, scheduler.queue.jobs.count())

        job.cancel()
    }

    @Test
    fun testRetry() = runBlocking {
        val factory = TestFactory()
        val scheduler = JobScheduler(factory)

        var count = 0
        val job = async {
            scheduler.onEvent.collect {
                println(it)
                if (it is JobEvent.DidFail) {
                    count += 1
                }
            }
        }

        scheduler.schedule<TestErrorTask> {
            rules {
                retry(RetryLimit.Limited(3), delay = 10.seconds)
            }
        }

        scheduler.queue.start()
        delay(1000L)
        job.cancel()
        assertEquals(4, count)
    }

    @Test
    fun testCancel() {
        val factory = TestFactory()
        val scheduler = JobScheduler(factory)

        runBlocking {
            scheduler.schedule<TestTask> {
                rules {
                    delay(10.seconds)
                }
            }

            launch {
                scheduler.onEvent.collect {
                    println(it)
                    if (it is JobEvent.DidEnd || it is JobEvent.DidFail) fail("Continued after run")
                    if (it is JobEvent.WillRun) {
                        scheduler.queue.cancel()
                        cancel()
                    }
                }
            }

            scheduler.queue.start()
        }
    }
}
