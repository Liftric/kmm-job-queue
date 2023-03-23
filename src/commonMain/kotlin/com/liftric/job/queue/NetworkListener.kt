package com.liftric.job.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

expect class NetworkManager

expect class NetworkListener : AbstractNetworkListener
abstract class AbstractNetworkListener(
    val networkManager: NetworkManager,
    val scope: CoroutineScope
) {
    abstract val currentNetworkState: StateFlow<NetworkState>
    abstract fun observeNetworkState()
    abstract fun stopMonitoring()

    fun isNetworkRuleSatisfied(jobInfo: JobInfo, currentNetworkState: NetworkState): Boolean {
        return currentNetworkState >= jobInfo.minRequiredNetworkState
    }
}

@kotlinx.serialization.Serializable
enum class NetworkState {
    NONE,
    MOBILE,
    WIFI
}
