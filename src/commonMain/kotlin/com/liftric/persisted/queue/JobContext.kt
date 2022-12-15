package com.liftric.persisted.queue

import kotlinx.datetime.Instant
import kotlin.time.Duration

interface JobContext {
    val id: UUID
    val timeout: Duration
    val task: DataTask<*>
    val tag: String?
    val rules: List<JobRule>
    val startTime: Instant
    suspend fun cancel()
    suspend fun repeat(id: UUID = this.id, timeout: Duration = this.timeout, task: DataTask<*> = this.task, tag: String? = this.tag, rules: List<JobRule> = this.rules, startTime: Instant = this.startTime)
    suspend fun broadcast(event: RuleEvent)
}
