package com.liftric.job.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

actual class NetworkListener(
    actual var networkState: NetworkState = NetworkState.NONE,
    actual val scope: CoroutineScope = CoroutineScope(context = Dispatchers.Default)
) {
    private val networkManager = NetworkManager()

    actual fun observeNetworkState() {
        networkManager.startMonitoring()
        scope.launch {
            networkManager.network.collectLatest { currentNetworkState ->
                networkState = when (currentNetworkState) {
                    NetworkState.NONE -> NetworkState.NONE
                    NetworkState.MOBILE -> NetworkState.MOBILE
                    NetworkState.WIFI -> NetworkState.WIFI
                }
            }
        }
    }

    actual fun stopMonitoring() {
        networkManager.stopMonitoring()
    }
}
