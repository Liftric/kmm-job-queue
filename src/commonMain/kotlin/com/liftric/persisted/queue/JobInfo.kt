package com.liftric.persisted.queue

import kotlin.time.Duration

data class JobInfo(
    var tag: String? = null,
    var timeout: Duration = Duration.INFINITE,
    var rules: MutableList<JobRule> = mutableListOf(),
    var shouldPersist: Boolean = false
)
