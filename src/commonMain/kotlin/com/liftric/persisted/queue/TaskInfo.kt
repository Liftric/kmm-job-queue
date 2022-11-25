package com.liftric.persisted.queue

import kotlinx.serialization.Serializable

@Serializable
class TaskInfo {
    var tag: String? = null
    var rules: Set<JobRule> = setOf()
        private set
    var params: Map<String, String> = mapOf()
        private set

    fun rules(init: RuleInfo.() -> Unit): TaskInfo {
        val info = RuleInfo()
        info.init()
        rules = info.rules
        return this
    }

    fun params(vararg params: Pair<String, String>): TaskInfo {
        this.params = params.toMap()
        return this
    }
}

@Serializable
class RuleInfo {
    val rules: MutableSet<JobRule> = mutableSetOf()
}
