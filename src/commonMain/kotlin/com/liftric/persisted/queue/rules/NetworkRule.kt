package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.JobRule
import com.liftric.persisted.queue.NetworkConnection

data class NetworkRule(val networkType: NetworkType) : JobRule() {
    fun hasCorrectNetworkRule(): Boolean = (networkType == NetworkConnection().getConnectionType())
}

sealed class NetworkType {
    object NoConnection : NetworkType()
    object Cellular : NetworkType()
    object Wifi : NetworkType()
}
