package com.liftric.job.queue.rules

import com.liftric.job.queue.*
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable

@Serializable
data class DelayRule(val duration: Duration = 0.seconds): JobRule() {
    override suspend fun willRun(context: JobContext) {
        delay(duration)
    }
}

fun JobInfo.delay(duration: Duration = 0.seconds): JobInfo {
    val rule = DelayRule(duration)
    rules.add(rule)
    return this
}
