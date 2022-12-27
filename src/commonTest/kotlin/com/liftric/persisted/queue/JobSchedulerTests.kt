package com.liftric.persisted.queue

import com.liftric.persisted.queue.rules.*
import kotlinx.coroutines.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class JobSchedulerTests {
    @Test
    fun testSchedule() = runBlocking {
        val scheduler = JobScheduler()
        val id = UUID::class.instance().toString()
        val job = async {
            scheduler.onEvent.collect {
                println(it)
            }
        }

        scheduler.schedule(TestTask(TestData(id))) {
            delay(1.seconds)
            unique(id)
        }

        scheduler.schedule(TestTask(TestData(id))) {
            unique(id)
        }

        assertEquals(1, scheduler.queue.jobs.count())

        scheduler.queue.start()

        delay(2000L)

        assertEquals(0, scheduler.queue.jobs.count())

        job.cancel()
    }

    @Test
    fun testRetry() = runBlocking {
        val scheduler = JobScheduler()

        var count = 0
        val job = launch {
            scheduler.onEvent.collect {
                println(it)
                if (it is JobEvent.DidScheduleRepeat) {
                    count += 1
                }
            }
        }

        scheduler.schedule(TestErrorTask()) {
            retry(RetryLimit.Limited(3), delay = 1.seconds)
        }

        scheduler.queue.start()
        delay(10000L)
        job.cancel()
        assertEquals(3, count)
    }

    @Test
    fun testCancelDuringRun() {
        val scheduler = JobScheduler()

        runBlocking {
            scheduler.schedule(LongRunningTask()) {
                delay(10.seconds)
            }

            launch {
                scheduler.onEvent.collect {
                    println(it)
                    if (it is JobEvent.DidEnd || it is JobEvent.DidFail) fail("Continued after run")
                    if (it is JobEvent.WillRun) {
                        scheduler.queue.cancel(it.job.id)
                    }
                    if (it is JobEvent.DidCancel) {
                        assertTrue(scheduler.queue.jobs.isEmpty())
                        cancel()
                    }
                }
            }

            scheduler.queue.start()
        }
    }

    @Test
    fun testCancelByIdBeforeEnqueue() {
        val scheduler = JobScheduler()

        runBlocking {
            val completable = CompletableDeferred<UUID>()

            launch {
                scheduler.onEvent.collect {
                    println(it)
                    if (it is JobEvent.DidEnd || it is JobEvent.DidFail) fail("Continued after run")
                    if (it is JobEvent.DidSchedule) {
                        completable.complete(it.job.id)
                    }
                    if (it is JobEvent.DidCancel) {
                        assertTrue(scheduler.queue.jobs.isEmpty())
                        cancel()
                    }
                }
            }

            delay(1000L)

            scheduler.schedule(::LongRunningTask) {
                delay(2.seconds)
            }

            scheduler.queue.cancel(completable.await())
        }
    }

    @Test
    fun testCancelByIdAfterEnqueue() {
        val scheduler = JobScheduler()

        runBlocking {
            launch {
                scheduler.onEvent.collect {
                    println(it)
                    if (it is JobEvent.DidSchedule) {
                        delay(3000L)
                        scheduler.queue.cancel(it.job.id)
                    }
                    if (it is JobEvent.DidCancel) {
                        cancel()
                    }
                }
            }

            delay(1000L)

            scheduler.queue.start()

            scheduler.schedule(::LongRunningTask) {
                delay(10.seconds)
            }
        }
    }
}
