package com.liftric.job.queue

import kotlinx.datetime.Instant

interface JobContext : JobData {
    suspend fun cancel()
    suspend fun repeat(
        id: UUID = this.id,
        info: JobInfo = this.info,
        task: Task = this.task,
        startTime: Instant = this.startTime,
    )
}

interface JobData {
    val id: UUID
    val info: JobInfo
    val task: Task
    val startTime: Instant
}
