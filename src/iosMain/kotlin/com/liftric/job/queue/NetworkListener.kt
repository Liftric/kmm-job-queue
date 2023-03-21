package com.liftric.job.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

actual class NetworkListener(
    scope: CoroutineScope = CoroutineScope(context = Dispatchers.Default)
) : AbstractNetworkListener(
    scope = scope
) {
    private val networkManager = NetworkManager()

    private val _currentNetworkState = MutableSharedFlow<NetworkState>(replay = 1)
    override val currentNetworkState: SharedFlow<NetworkState>
        get() = _currentNetworkState.asSharedFlow()

    override fun observeNetworkState() {
        networkManager.startMonitoring()
        scope.launch {
            networkManager.network.collectLatest { currentNetworkState ->
                _currentNetworkState.emit(
                    when (currentNetworkState) {
                        NetworkState.NONE -> NetworkState.NONE
                        NetworkState.MOBILE -> NetworkState.MOBILE
                        NetworkState.WIFI -> NetworkState.WIFI
                    }
                )
            }
        }
    }

    override fun stopMonitoring() {
        networkManager.stopMonitoring()
    }
}
