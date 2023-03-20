package com.liftric.job.queue.rules

import com.liftric.job.queue.JobContext
import com.liftric.job.queue.JobInfo
import com.liftric.job.queue.JobRule
import com.liftric.job.queue.NetworkState

data class NetworkRule(val minRequiredNetworkState: NetworkState) : JobRule() {
    override suspend fun mutating(info: JobInfo) {
        info.minRequiredNetworkState = minRequiredNetworkState
    }

    override suspend fun willRun(context: JobContext, currentNetworkState: NetworkState) {
        if (context.info.minRequiredNetworkState > currentNetworkState) {
            println("NETWORK RULE: unsatisfied")
            throw NetworkException("Network requirement not satisfied!")
        } else {
            println("NETWORK RULE: satisfied")
        }
    }
}

fun JobInfo.minRequiredNetwork(networkState: NetworkState): JobInfo {
    val rule = NetworkRule(networkState)
    rules.add(rule)
    return this
}

class NetworkException(message: String) : Exception(message)

