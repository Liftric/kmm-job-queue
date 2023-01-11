package com.liftric.job.queue

import kotlinx.datetime.Instant

interface JobContext {
    val id: UUID
    val info: JobInfo
    val task: Task
    val startTime: Instant
    suspend fun cancel()
    suspend fun repeat(id: UUID = this.id, info: JobInfo = this.info, task: Task = this.task, startTime: Instant = this.startTime)
}
