package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.Context
import com.liftric.persisted.queue.JobRule
import com.liftric.persisted.queue.NetworkConnection

data class NetworkRule(val networkRuleType: NetworkRuleType, val context: Context?) : JobRule() {
    fun hasCorrectNetworkRule(): Boolean {
        if (context == null) {
            return false
        }
        return when (networkRuleType) {
            is NetworkRuleType.Any -> {
                (networkRuleType.value <= NetworkConnection().getConnectionType(context).value)
            }
            else -> {
                (networkRuleType.value == NetworkConnection().getConnectionType(context).value)
            }
        }
    }
}

sealed class NetworkRuleType(val value: Int) {
    class NotRequired(value: Int = 0) : NetworkRuleType(value)
    class Any(value: Int = 1) : NetworkRuleType(value)
    class Cellular(value: Int = 2) : NetworkRuleType(value)
    class Wifi(value: Int = 3) : NetworkRuleType(value)
}

sealed class ConnectionType(val value: Int) {
    class NotConnected(value: Int = 0) : ConnectionType(value)
    class Undefined(value: Int = 1) : ConnectionType(value)
    class Cellular(value: Int = 2) : ConnectionType(value)
    class Wifi(value: Int = 3) : ConnectionType(value)
}


