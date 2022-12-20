package com.liftric.persisted.queue

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.coroutines.coroutineContext

@Serializable
data class Job(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val info: JobInfo,
    override val task: Task,
    override val startTime: Instant
): JobContext {
    @Transient var delegate: JobDelegate? = null
    @Transient var isCancelled: Boolean = false
        private set

    constructor(task: Task, info: JobInfo) : this (UUIDFactory.create(), info, task, Clock.System.now())

    private var canRepeat: Boolean = false

    suspend fun run() {
        withContext(coroutineContext) {
            if (isCancelled) return@withContext
            val event = try {
                info.rules.forEach { it.willRun(this@Job) }

                delegate?.broadcast(JobEvent.WillRun(this@Job))

                task.body()

                JobEvent.DidSucceed(this@Job)
            } catch (e: CancellationException) {
                JobEvent.DidCancel(this@Job)
            } catch (e: Throwable) {
                canRepeat = task.onRepeat(e)
                JobEvent.DidFail(this@Job, e)
            }

            try {
                delegate?.broadcast(event)

                if (isCancelled) return@withContext

                info.rules.forEach { it.willRemove(this@Job, event) }
            } catch (e: CancellationException) {
                delegate?.broadcast(JobEvent.DidCancel(this@Job))
            } catch (e: Throwable) {
                delegate?.broadcast(JobEvent.DidFailOnRemove(this@Job, e))
            } finally {
                delegate?.exit(this@Job)
            }
        }
    }

    override suspend fun cancel() {
        if (isCancelled) return
        isCancelled = true
        delegate?.cancel(this@Job)
    }

    override suspend fun repeat(id: UUID, info: JobInfo, task: Task, startTime: Instant) {
        if (canRepeat) {
            delegate?.repeat(Job(id, info, task, startTime))
        } else {
            delegate?.broadcast(JobEvent.NotAllowedToRepeat(this@Job))
        }
    }

    override suspend fun broadcast(event: RuleEvent) {
        delegate?.broadcast(event)
    }
}
