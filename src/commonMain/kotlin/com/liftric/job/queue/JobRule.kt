package com.liftric.job.queue

import kotlinx.serialization.Serializable

@Serializable
abstract class JobRule {
    open suspend fun mutating(jobInfo: JobInfo) {}
    @Throws(Throwable::class)
    open suspend fun willSchedule(queue: Queue, jobContext: JobContext) {}
    open suspend fun willRun(jobContext: JobContext) {}
    open suspend fun willRemove(jobContext: JobContext, result: JobEvent) {}
}
