package com.liftric.persisted.queue

class JobInfo {
    var tag: String? = null
    var rules: Set<JobRule> = setOf()
        private set
    var params: Map<String, Any> = mapOf()
        private set

    fun rules(init: RuleInfos.() -> Unit): JobInfo {
        val info = RuleInfos()
        info.init()
        rules = info.rules
        return this
    }

    fun params(vararg params: Pair<String, Any>): JobInfo {
        this.params = params.toMap()
        return this
    }
}

class RuleInfos {
    val rules: MutableSet<JobRule> = mutableSetOf()
}
