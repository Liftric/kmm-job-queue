package com.liftric.persisted.queue

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Job<Data>(
    @Contextual override val id: UUID,
    override val info: JobInfo,
    override val task: DataTask<Data>,
    @Contextual override val startTime: Instant
): JobContext {
    @Transient var delegate: JobDelegate? = null

    constructor(task: DataTask<Data>, info: JobInfo) : this (UUID::class.instance(), info, task, Clock.System.now())

    private var cancellable: kotlinx.coroutines.Job? = null

    var isCancelled: Boolean = false
        private set

    private var canRepeat: Boolean = false

    internal suspend fun run() {
        coroutineScope {
            if (isCancelled) return@coroutineScope
            cancellable = launch {
                val event = try {
                    info.rules.forEach { it.willRun(this@Job) }

                    delegate?.broadcast(JobEvent.WillRun(this@Job))

                    task.body()

                    JobEvent.DidEnd(this@Job)
                } catch (e: CancellationException) {
                    JobEvent.DidCancel(this@Job, "Cancelled during run")
                } catch (e: Error) {
                    canRepeat = task.onRepeat(e)
                    JobEvent.DidFail(this@Job, e)
                }

                try {
                    delegate?.broadcast(event)

                    if (isCancelled) return@launch

                    info.rules.forEach { it.willRemove(this@Job, event) }
                } catch (e: CancellationException) {
                    delegate?.broadcast(JobEvent.DidCancel(this@Job, "Cancelled after run"))
                } catch (e: Error) {
                    delegate?.broadcast(JobEvent.DidFailOnRemove(this@Job, e))
                }
            }
        }
    }

    override suspend fun cancel() {
        if (isCancelled) return
        isCancelled = true
        cancellable?.cancel(CancellationException("Cancelled during run")) ?: run {
            delegate?.broadcast(JobEvent.DidCancel(this@Job, "Cancelled before run"))
        }
        delegate?.exit()
    }

    override suspend fun repeat(id: UUID, info: JobInfo, task: DataTask<*>, startTime: Instant) {
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
