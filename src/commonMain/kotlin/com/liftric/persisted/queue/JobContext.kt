package com.liftric.persisted.queue

import kotlinx.datetime.Instant

interface JobContext {
    val id: UUID
    val task: Task
    val tag: String?
    val rules: List<JobRule>
    val startTime: Instant
    suspend fun terminate()
    suspend fun repeat(id: UUID = this.id, task: Task = this.task, tag: String? = this.tag, rules: List<JobRule> = this.rules, startTime: Instant = this.startTime)
    suspend fun broadcast(event: Event)
}