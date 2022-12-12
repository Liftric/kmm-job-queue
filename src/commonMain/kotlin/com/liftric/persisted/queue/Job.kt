package com.liftric.persisted.queue

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Duration

@Serializable
data class Job(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val timeout: Duration,
    override val task: Task,
    override val tag: String?,
    override val rules: List<JobRule>,
    override val startTime: Instant = Clock.System.now()
): JobContext {
    @Transient
    var delegate: JobDelegate? = null

    constructor(task: Task, info: JobInfo) : this (UUID::class.instance(), info.timeout, task, info.tag, info.rules)

    private var cancellable: kotlinx.coroutines.Job? = null

    var isCancelled: Boolean = false
        private set

    private var canRepeat: Boolean = false

    internal suspend fun run() {
        coroutineScope {
            if (isCancelled) return@coroutineScope
            cancellable = launch {
                val event = try {
                    rules.forEach { it.willRun(this@Job) }

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

                    rules.forEach { it.willRemove(this@Job, event) }
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

    override suspend fun repeat(id: UUID, timeout: Duration, task: Task, tag: String?, rules: List<JobRule>, startTime: Instant) {
        if (canRepeat) {
            delegate?.repeat(Job(id, timeout, task, tag, rules, startTime))
        } else {
            delegate?.broadcast(JobEvent.NotAllowedToRepeat(this@Job))
        }
    }

    override suspend fun broadcast(event: RuleEvent) {
        delegate?.broadcast(event)
    }
}
