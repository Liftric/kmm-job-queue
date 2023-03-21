package com.liftric.job.queue.rules

import com.liftric.job.queue.JobInfo
import com.liftric.job.queue.JobRule
import com.liftric.job.queue.NetworkState

data class NetworkRule(val minRequiredNetworkState: NetworkState) : JobRule() {
    override suspend fun mutating(info: JobInfo) {
        info.minRequiredNetworkState = minRequiredNetworkState
    }
}

fun JobInfo.minRequiredNetwork(networkState: NetworkState): JobInfo {
    val rule = NetworkRule(networkState)
    rules.add(rule)
    return this
}

class NetworkException(message: String) : Exception(message)

