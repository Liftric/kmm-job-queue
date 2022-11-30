package com.liftric.persisted.queue

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

    internal suspend fun run() {
        val event = try {
            rules.forEach { it.willRun(this@Job) }

            delegate?.broadcast(JobEvent.WillRun(this@Job))

            task.body()

            JobEvent.DidEnd(this@Job)
        } catch (e: Error) {
            terminate()
            JobEvent.DidFail(this@Job, e)
        }

        try {
            delegate?.broadcast(event)

            rules.forEach { it.willRemove(this@Job, event) }
        } catch (e: Error) {
            terminate()
            JobEvent.DidFailOnRemove(this@Job, e)
        }
    }

    override suspend fun terminate() {
        delegate?.exit()
    }

    override suspend fun repeat(id: UUID, timeout: Duration, task: Task, tag: String?, rules: List<JobRule>, startTime: Instant) {
        delegate?.repeat(Job(id, timeout, task, tag, rules, startTime))
    }

    override suspend fun broadcast(event: RuleEvent) {
        delegate?.broadcast(event)
    }
}
