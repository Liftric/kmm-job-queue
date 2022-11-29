package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.*
import kotlinx.serialization.Serializable

@Serializable
data class UniqueRule(private val tag: String? = null): JobRule() {
    override suspend fun mutating(info: JobInfo) {
        info.tag = tag
    }

    override suspend fun willSchedule(queue: Queue, context: JobContext) {
        for (item in queue.operations) {
            if (item.tag == tag || item.id == context.id) {
                throw Error("Should be unique")
            }
        }
    }
}

fun RuleInfo.unique(tag: String? = null): RuleInfo {
    val rule = UniqueRule(tag)
    rules.add(rule)
    return this
}
