package com.liftric.job.queue.rules

import com.liftric.job.queue.JobInfo
import com.liftric.job.queue.JobRule
import kotlinx.serialization.Serializable

@Serializable
data class PersistenceRule(val shouldPersist: Boolean): JobRule() {
    override suspend fun mutating(info: JobInfo) {
        info.shouldPersist = shouldPersist
    }
}

fun JobInfo.persist(shouldPersist: Boolean = true): JobInfo {
    val rule = PersistenceRule(shouldPersist)
    rules.add(rule)
    return this
}
