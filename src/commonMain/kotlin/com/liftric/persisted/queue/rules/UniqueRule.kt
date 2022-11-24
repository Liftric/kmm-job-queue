package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.*

class UniqueRule(private val tag: String? = null): JobRule() {
    override suspend fun mapping(info: JobInfo): JobInfo {
        info.tag = tag
        return info
    }
    @Throws(Exception::class)
    override suspend fun willSchedule(queue: Queue, task: Task) {
        for (item in queue.tasks) {
            if (item.job.tag == tag || item.job.id == task.job.id) {
                throw Exception("Should be unique")
            }
        }
    }
}

fun RuleInfos.unique(tag: String? = null): RuleInfos {
    val rule = UniqueRule(tag)
    rules.add(rule)
    return this
}
