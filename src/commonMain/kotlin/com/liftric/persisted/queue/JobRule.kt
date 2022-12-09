package com.liftric.persisted.queue

import kotlinx.serialization.Serializable

@Serializable
abstract class JobRule {
    open suspend fun mutating(info: JobInfo) {}
    @Throws(Throwable::class)
    open suspend fun willSchedule(queue: Queue, context: JobContext) {}
    open suspend fun willRun(context: JobContext) {}
    open suspend fun willRemove(context: JobContext, result: JobEvent) {}
}
