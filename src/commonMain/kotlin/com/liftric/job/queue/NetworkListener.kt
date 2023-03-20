package com.liftric.job.queue

import kotlinx.coroutines.CoroutineScope

expect class NetworkManager

expect class NetworkListener {
    var networkState: NetworkState
    val scope: CoroutineScope
    fun observeNetworkState()
    fun stopMonitoring()
}

@kotlinx.serialization.Serializable
enum class NetworkState {
    NONE,
    MOBILE,
    WIFI
}
