package com.liftric.job.queue.rules

import com.liftric.job.queue.JobInfo
import com.liftric.job.queue.JobRule
import com.liftric.job.queue.NetworkState
import kotlinx.coroutines.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class NetworkRule(
    val minRequiredNetworkState: NetworkState,
    val networkRuleTimeout: Duration
) : JobRule() {
    override suspend fun mutating(jobInfo: JobInfo) {
        jobInfo.minRequiredNetworkState = minRequiredNetworkState
        jobInfo.networkRuleTimeout = networkRuleTimeout
    }
}

fun JobInfo.minRequiredNetwork(
    networkState: NetworkState,
    networkRuleTimeout: Duration = 30.seconds
): JobInfo {
    val rule = NetworkRule(
        minRequiredNetworkState = networkState,
        networkRuleTimeout = networkRuleTimeout
    )
    rules.add(rule)
    return this
}

class NetworkRuleTimeoutException(message: String) : CancellationException(message)

