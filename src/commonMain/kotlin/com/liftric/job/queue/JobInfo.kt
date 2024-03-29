package com.liftric.job.queue

import kotlin.time.Duration
import kotlinx.serialization.Serializable

@Serializable
data class JobInfo(
    var tag: String? = null,
    var timeout: Duration = Duration.INFINITE,
    var rules: MutableList<JobRule> = mutableListOf(),
    var shouldPersist: Boolean = false
)
