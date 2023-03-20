package com.liftric.job.queue

import com.liftric.job.queue.rules.NetworkState
import dev.tmapps.konnection.Konnection
import dev.tmapps.konnection.NetworkConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

actual class NetworkListener(
    actual var networkState: NetworkState = NetworkState.NONE,
    actual val scope: CoroutineScope = CoroutineScope(context = Dispatchers.Default)
) {
    actual fun observeNetworkState() {
        Konnection()
            .observeNetworkConnection()
            .onEach { networkConnection ->
                networkState = when (networkConnection) {
                    NetworkConnection.WIFI -> NetworkState.WIFI
                    NetworkConnection.MOBILE -> NetworkState.MOBILE
                    null -> NetworkState.NONE
                }
            }
            .launchIn(scope)
    }
}
