package com.liftric.persisted.queue

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Job(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val task: Task,
    override val tag: String?,
    override val rules: List<JobRule>,
    override val startTime: Instant = Clock.System.now()
): JobContext {
    @Transient
    var delegate: JobDelegate? = null

    constructor(task: Task, info: JobInfo) : this (UUID::class.instance(), task, info.tag, info.rules)

    internal suspend fun run() {
        val event = try {
            rules.forEach { it.willRun(this@Job) }

            task.body()

            Event.DidEnd(this@Job)
        } catch (e: Error) {
            terminate()
            Event.DidFail(this@Job, e)
        }

        try {
            delegate?.broadcast(event)

            rules.forEach { it.willRemove(this@Job, event) }
        } catch (e: Error) {
            terminate()
            Event.DidFailOnRemove(this@Job, e)
        }
    }

    override suspend fun terminate() {
        delegate?.exit()
    }

    override suspend fun repeat(id: UUID, task: Task, tag: String?, rules: List<JobRule>, startTime: Instant) {
        delegate?.repeat(Job(id, task, tag, rules, startTime))
    }

    override suspend fun broadcast(event: Event) {
        delegate?.broadcast(event)
    }
}
