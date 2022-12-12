package com.liftric.persisted.queue

import kotlin.time.Duration

data class JobInfo(
    var tag: String? = null,
    var timeout: Duration = Duration.INFINITE
) {
    var rules: List<JobRule> = listOf()
    var persister: JobPersister? = null

    fun rules(init: RuleInfo.() -> Unit): JobInfo {
        val info = RuleInfo()
        info.init()
        rules = info.rules.distinctBy { it::class }
        return this
    }

    fun persist(persister: JobPersister): JobInfo {
        this.persister = persister
        return this
    }
}

class RuleInfo {
    val rules: MutableList<JobRule> = mutableListOf()
}
