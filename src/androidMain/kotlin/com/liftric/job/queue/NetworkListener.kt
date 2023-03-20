package com.liftric.job.queue

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

actual class NetworkListener(
    val context: Context,
    actual var networkState: NetworkState = NetworkState.NONE,
    actual val scope: CoroutineScope = CoroutineScope(context = Dispatchers.Default)
) {
    actual fun observeNetworkState() {
        scope.launch {
            NetworkManager(context)
                .observeNetworkConnection()
                .collectLatest { currentNetworkState ->
                    networkState = when (currentNetworkState) {
                        NetworkState.NONE -> NetworkState.NONE
                        NetworkState.MOBILE -> NetworkState.MOBILE
                        NetworkState.WIFI -> NetworkState.WIFI
                        null -> NetworkState.NONE
                    }
                }
        }
    }

    // not needed for Android
    actual fun stopMonitoring() {}
}
