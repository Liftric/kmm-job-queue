package com.liftric.job.queue.rules

import com.liftric.job.queue.JobInfo
import com.liftric.job.queue.JobRule
import kotlin.time.Duration
import kotlinx.serialization.Serializable

@Serializable
data class TimeoutRule(val timeout: Duration): JobRule() {
    override suspend fun mutating(info: JobInfo) {
        info.jobTimeout = timeout
    }
}

fun JobInfo.timeout(timeout: Duration): JobInfo {
    val rule = TimeoutRule(timeout)
    rules.add(rule)
    return this
}
