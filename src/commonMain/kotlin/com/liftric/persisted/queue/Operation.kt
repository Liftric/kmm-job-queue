package com.liftric.persisted.queue

data class Operation(
    val rules: Set<JobRule>,
    val job: Job,
    var tag: String? = null
)
