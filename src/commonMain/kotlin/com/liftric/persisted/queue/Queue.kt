package com.liftric.persisted.queue

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock

interface Queue {
    val jobs: List<JobContext>
    val configuration: Configuration

    data class Configuration(
        val scope: CoroutineScope,
        val maxConcurrency: Int
    )
}

class JobQueue(override val configuration: Queue.Configuration): Queue {
    private val queue = atomic(mutableListOf<Job>())
    private val lock = Semaphore(configuration.maxConcurrency, 0)
    private val isCancelling = Mutex(false)
    override val jobs: List<JobContext>
        get() = queue.value

    constructor(configuration: Queue.Configuration?) : this(configuration ?: Queue.Configuration(CoroutineScope(Dispatchers.Default), 1))

    internal fun add(job: Job) {
        queue.value = queue.value.plus(listOf(job)).sortedBy { it.startTime }.toMutableList()
    }

    suspend fun start() {
        while (configuration.scope.isActive) {
            delay(1000L)
            if (queue.value.isEmpty()) break
            if (isCancelling.isLocked) break
            if (lock.availablePermits < 1) break
            val job = queue.value.first()
            if (job.isCancelled) {
                queue.value.remove(job)
            } else if (job.startTime <= Clock.System.now()) {
                lock.withPermit {
                    withTimeout(job.info.timeout) {
                        job.run()
                        queue.value.remove(job)
                    }
                }
            }
        }
    }

    suspend fun cancel() {
        isCancelling.withLock {
            configuration.scope.coroutineContext.cancelChildren()
            queue.value.clear()
        }
    }

    suspend fun cancel(id: UUID) {
        isCancelling.withLock {
            val job = queue.value.first { it.id == id }
            job.cancel()
            queue.value.remove(job)
        }
    }

    suspend fun cancel(tag: String) {
        isCancelling.withLock {
            val job = queue.value.first { it.info.tag == tag }
            job.cancel()
            queue.value.remove(job)
        }
    }
}
