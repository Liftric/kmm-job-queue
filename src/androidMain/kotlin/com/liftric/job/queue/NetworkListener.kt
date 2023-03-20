package com.liftric.job.queue

import android.content.Context
import com.liftric.job.queue.rules.NetworkState
import dev.tmapps.konnection.Konnection
import dev.tmapps.konnection.NetworkConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual class NetworkListener(
    val context: Context,
    actual var networkState: NetworkState = NetworkState.NONE,
    actual val scope: CoroutineScope = CoroutineScope(context = Dispatchers.Default)
) {
    actual fun observeNetworkState() {
        scope.launch {
            Konnection(context = context)
                .observeNetworkConnection()
                .collect { networkConnection ->
                    networkState = when (networkConnection) {
                        NetworkConnection.WIFI -> NetworkState.WIFI
                        NetworkConnection.MOBILE -> NetworkState.MOBILE
                        null -> NetworkState.NONE
                    }
                }
        }
    }
}
