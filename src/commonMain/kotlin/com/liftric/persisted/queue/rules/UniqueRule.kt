package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.*
import kotlinx.serialization.Serializable

@Serializable
data class UniqueRule(private val tag: String? = null): JobRule() {
    override suspend fun mutating(info: JobInfo) {
        info.tag = tag
    }

    override suspend fun willSchedule(queue: Queue, context: JobContext) {
        for (item in queue.jobs) {
            if (item.info.tag == tag || item.id == context.id) {
                throw Error("Job with id=${item.id} already exists")
            }
        }
    }
}

fun JobInfo.unique(tag: String? = null): JobInfo {
    val rule = UniqueRule(tag)
    rules.add(rule)
    return this
}
