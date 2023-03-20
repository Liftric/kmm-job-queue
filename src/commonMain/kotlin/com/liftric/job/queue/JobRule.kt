package com.liftric.job.queue

import kotlinx.serialization.Serializable

@Serializable
abstract class JobRule {
    open suspend fun mutating(info: JobInfo) {}
    @Throws(Throwable::class)
    open suspend fun willSchedule(queue: Queue, context: JobContext) {}
    open suspend fun willRun(context: JobContext, currentNetworkState: NetworkState) {}
    open suspend fun willRemove(context: JobContext, result: JobEvent) {}
}
