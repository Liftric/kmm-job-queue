package com.liftric.job.queue

import com.liftric.job.queue.rules.*
import kotlinx.coroutines.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

expect class JobQueueTests : AbstractJobQueueTests
abstract class AbstractJobQueueTests(private val queue: JobQueue) {
    @AfterTest
    fun tearDown() = runBlocking {
        queue.stop()
        queue.clear()
    }

    @Test
    fun testSchedule() {
        runBlocking {
            val id = UUIDFactory.create().toString()
            val job = async {
                queue.jobEventListener.collect {
                    println(it)
                }
            }

            queue.schedule(TestData(id), ::TestTask) {
                delay(1.seconds)
                unique(id)
            }

            queue.schedule(TestTask(TestData(id))) {
                unique(id)
            }

            assertEquals(1, queue.numberOfJobs)

            queue.start()

            delay(2000L)

            assertEquals(0, queue.numberOfJobs)

            job.cancel()

        }
    }

    @Test
    fun testRetry() = runBlocking {
        var count = 0
        val job = launch {
            queue.jobEventListener.collect {
                println(it)
                if (it is JobEvent.DidScheduleRepeat) {
                    count += 1
                }
            }
        }

        delay(1000L)

        queue.schedule(TestErrorTask()) {
            retry(RetryLimit.Limited(3), delay = 1.seconds)
        }

        queue.start()
        delay(15000L)
        job.cancel()
        assertEquals(3, count)
    }

    @Test
    fun testCancelDuringRun() {
        runBlocking {
            val listener = launch {
                queue.jobEventListener.collect {
                    println(it)
                    if (it is JobEvent.DidSucceed || it is JobEvent.DidFail) fail("Continued after run")
                    if (it is JobEvent.WillRun) {
                        queue.cancel(it.job.id)
                    }
                    if (it is JobEvent.DidCancel) {
                        assertTrue(queue.numberOfJobs == 0)
                    }
                }
            }

            queue.start()

            delay(1000L)

            queue.schedule(::LongRunningTask)

            delay(10000L)

            listener.cancel()
        }
    }

    @Test
    fun testCancelByIdBeforeEnqueue() {
        runBlocking {
            val completable = CompletableDeferred<UUID>()

            launch {
                queue.jobEventListener.collect {
                    println(it)
                    if (it is JobEvent.DidSucceed || it is JobEvent.DidFail) fail("Continued after run")
                    if (it is JobEvent.DidSchedule) {
                        completable.complete(it.job.id)
                    }
                    if (it is JobEvent.DidCancel) {
                        assertTrue(queue.numberOfJobs == 0)
                        cancel()
                    }
                }
            }

            delay(1000L)

            queue.schedule(::LongRunningTask) {
                delay(2.seconds)
            }

            queue.cancel(completable.await())
        }
    }

    @Test
    fun testCancelByIdAfterEnqueue() {
        runBlocking {
            launch {
                queue.jobEventListener.collect {
                    println(it)
                    if (it is JobEvent.DidSchedule) {
                        delay(3000L)
                        queue.cancel(it.job.id)
                    }
                    if (it is JobEvent.DidCancel) {
                        cancel()
                    }
                }
            }

            delay(1000L)

            queue.start()

            queue.schedule(::LongRunningTask) {
                delay(10.seconds)
            }
        }
    }

    @Test
    fun testPersist() = runBlocking {
        queue.schedule(TestData(UUIDFactory.create().toString()), ::TestTask) {
            persist()
        }

        assertEquals(1, queue.numberOfJobs)

        queue.clear(clearStore = false)

        assertEquals(0, queue.numberOfJobs)

        queue.restore()

        assertEquals(1, queue.numberOfJobs)
    }

    @Test
    fun testNetworkRuleSatisfied() = runBlocking {
        val id = UUIDFactory.create().toString()
        val job = async {
            queue.jobEventListener.collect {
                println("TEST -> JOB INFO: $it")
                assertTrue(it is JobEvent.DidSucceed)
            }
        }

        queue.schedule(TestData(id), ::TestTask) {
            minRequiredNetwork(NetworkState.MOBILE)
        }

        queue.networkListener.networkState = NetworkState.WIFI
        println("Network State: ${queue.networkListener.networkState}")

        queue.start()
        delay(200)
        job.cancel()
    }

    @Test
    fun testNetworkRuleUnSatisfied() = runBlocking {
        val id = UUIDFactory.create().toString()
        val job = launch {
            queue.jobEventListener.collect {
                println("TEST -> JOB INFO: $it")
                assertTrue(it is JobEvent.DidFail)
            }
        }

        queue.schedule(TestData(id), ::TestTask) {
            minRequiredNetwork(NetworkState.WIFI)
        }

        queue.networkListener.networkState = NetworkState.NONE
        println("Network State: ${queue.networkListener.networkState}")

        queue.start()
        delay(200)
        job.cancel()
    }
}
