package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.*

class UniqueRule(private val tag: String? = null): JobRule() {
    override suspend fun mutating(info: TaskInfo) {
        info.tag = tag
    }

    override suspend fun willSchedule(queue: Queue, task: Task) {
        for (item in queue.tasks.value) {
            if (item.tag == tag || item.id == task.id) {
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
