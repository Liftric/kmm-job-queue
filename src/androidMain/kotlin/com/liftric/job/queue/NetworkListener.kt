package com.liftric.job.queue

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

actual class NetworkListener(
    val context: Context,
    scope: CoroutineScope = CoroutineScope(context = Dispatchers.Default)
) : AbstractNetworkListener(
    scope = scope
) {
    private val _currentNetworkState = MutableSharedFlow<NetworkState>(replay = 1)
    override val currentNetworkState: SharedFlow<NetworkState>
        get() = _currentNetworkState.asSharedFlow()

    override fun observeNetworkState() {
        scope.launch {
            NetworkManager(context)
                .observeNetworkConnection()
                .collectLatest { currentNetworkState ->
                    _currentNetworkState.emit(
                        when (currentNetworkState) {
                            NetworkState.NONE -> NetworkState.NONE
                            NetworkState.MOBILE -> NetworkState.MOBILE
                            NetworkState.WIFI -> NetworkState.WIFI
                            null -> NetworkState.NONE
                        }
                    )
                }
        }
    }


    // not needed for Android
    override fun stopMonitoring() {}
}
