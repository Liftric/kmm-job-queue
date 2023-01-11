package com.liftric.job.queue.rules

import com.liftric.job.queue.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class PeriodicRule(val interval: Duration = 0.seconds): JobRule() {
    override suspend fun willRemove(context: JobContext, result: JobEvent) {
        if (result is JobEvent.DidSucceed) {
            context.repeat(startTime = Clock.System.now().plus(interval))
        }
    }
}

fun JobInfo.repeat(interval: Duration = 0.seconds): JobInfo {
    val rule = PeriodicRule(interval)
    rules.add(rule)
    return this
}
