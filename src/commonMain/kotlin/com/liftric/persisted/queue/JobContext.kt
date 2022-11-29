package com.liftric.persisted.queue

import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

interface JobContext {
    val id: UUID
    val job: Job
    val tag: String?
    val rules: List<JobRule>
    val startTime: Instant
    suspend fun terminate()
    suspend fun repeat(id: UUID = this.id, job: Job = this.job, tag: String? = this.tag, rules: List<JobRule> = this.rules, startTime: Instant = this.startTime)
    suspend fun broadcast(event: Event)
}

@Serializable
data class Operation(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val job: Job,
    override val tag: String?,
    override val rules: List<JobRule>,
    override val startTime: Instant = Clock.System.now()
): JobContext {
    @Transient
    var delegate: JobDelegate? = null

    constructor(job: Job, info: JobInfo) : this (UUID::class.instance(), job, info.tag, info.rules)

    suspend fun run() {
        val event = try {
            rules.forEach { it.willRun(this@Operation) }

            job.body()

            Event.DidEnd(this@Operation)
        } catch (e: Error) {
            terminate()
            Event.DidFail(this@Operation, e)
        }

        try {
            delegate?.broadcast(event)

            rules.forEach { it.willRemove(this@Operation, event) }
        } catch (e: Error) {
            terminate()
            Event.DidFail(this@Operation, e)
        }
    }

    override suspend fun terminate() {
        delegate?.exit()
    }

    override suspend fun repeat(id: UUID, job: Job, tag: String?, rules: List<JobRule>, startTime: Instant) {
        delegate?.repeat(Operation(id, job, tag, rules, startTime))
    }

    override suspend fun broadcast(event: Event) {
        delegate?.broadcast(event)
    }
}

class JobDelegate {
    var onExit: (suspend () -> Unit)? = null
    var onRepeat: (suspend (Operation) -> Unit)? = null
    var onEvent: (suspend (Event) -> Unit)? = null

    suspend fun broadcast(event: Event) {
        onEvent?.invoke(event)
    }

    suspend fun exit() {
        onExit?.invoke()
    }

    suspend fun repeat(operation: Operation) {
        onRepeat?.invoke(operation)
    }
}
