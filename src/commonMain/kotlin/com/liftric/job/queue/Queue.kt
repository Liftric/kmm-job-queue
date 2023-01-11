package com.liftric.job.queue

import kotlinx.coroutines.*

interface Queue {
    val jobs: List<JobContext>
    val numberOfJobs: Int
    val configuration: Configuration

    data class Configuration(
        val scope: CoroutineScope,
        val maxConcurrency: Int,
        val startsAutomatically: Boolean
    )

    companion object {
        val DefaultConfiguration = Configuration(
            scope = CoroutineScope(Dispatchers.Default),
            maxConcurrency = 1,
            startsAutomatically = false
        )
    }
}
