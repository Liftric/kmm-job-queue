package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.JobInfo
import com.liftric.persisted.queue.JobRule
import com.liftric.persisted.queue.RuleInfo
import kotlin.time.Duration

data class TimeoutRule(val timeout: Duration): JobRule() {
    override suspend fun mutating(info: JobInfo) {
        info.timeout = timeout
    }
}

fun RuleInfo.timeout(timeout: Duration): RuleInfo {
    val rule = TimeoutRule(timeout)
    rules.add(rule)
    return this
}
