package com.liftric.persisted.queue

data class Operation(
    val rules: List<JobRule>,
    val job: Job,
    var tag: String? = null
)
