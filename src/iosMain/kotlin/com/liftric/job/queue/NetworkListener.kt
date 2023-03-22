package com.liftric.job.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

actual class NetworkListener(
    networkManager: NetworkManager,
    scope: CoroutineScope = CoroutineScope(context = Dispatchers.Default)
) : AbstractNetworkListener(
    networkManager = networkManager,
    scope = scope
) {
    private val _currentNetworkState = MutableStateFlow(NetworkState.NONE)
    override val currentNetworkState: StateFlow<NetworkState>
        get() = _currentNetworkState.asStateFlow()

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
