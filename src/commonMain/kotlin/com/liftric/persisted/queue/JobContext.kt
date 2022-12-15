package com.liftric.persisted.queue

import kotlinx.datetime.Instant

interface JobContext {
    val id: UUID
    val info: JobInfo
    val task: DataTask<*>
    val startTime: Instant
    suspend fun cancel()
    suspend fun repeat(id: UUID = this.id, info: JobInfo = this.info, task: DataTask<*> = this.task, startTime: Instant = this.startTime)
    suspend fun broadcast(event: RuleEvent)
}
