package com.liftric.persisted.queue

import kotlin.time.Duration

data class JobInfo(
    var tag: String? = null,
    var timeout: Duration = Duration.INFINITE
) {
    var rules: List<JobRule> = listOf()
        private set
    var params: Map<String, String> = mapOf()
        private set

    fun rules(init: RuleInfo.() -> Unit): JobInfo {
        val info = RuleInfo()
        info.init()
        rules = info.rules.distinctBy { it::class }
        return this
    }

    fun params(vararg params: Pair<String, String>): JobInfo {
        this.params = params.toMap()
        return this
    }
}

class RuleInfo {
    val rules: MutableList<JobRule> = mutableListOf()
}
