package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.JobInfo
import com.liftric.persisted.queue.JobRule
import kotlin.time.Duration

data class TimeoutRule(val timeout: Duration): JobRule() {
    override suspend fun mutating(info: JobInfo) {
        info.timeout = timeout
    }
}

fun JobInfo.timeout(timeout: Duration): JobInfo {
    val rule = TimeoutRule(timeout)
    rules.add(rule)
    return this
}
