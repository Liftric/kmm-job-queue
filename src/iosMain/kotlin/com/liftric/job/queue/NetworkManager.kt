package com.liftric.job.queue

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import platform.Network.*
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_queue_attr_make_with_qos_class
import platform.darwin.dispatch_queue_create
import platform.posix.QOS_CLASS_UTILITY

class NetworkManager {
    private val networkChannel = Channel<NetworkState>(Channel.UNLIMITED)
    val network: Flow<NetworkState> = networkChannel.receiveAsFlow()

    private val networkMonitor = object : nw_path_monitor_update_handler_t {
        override fun invoke(network: nw_path_t) {
            checkReachability(network)
        }

    }
    private val nwPathMonitor: nw_path_monitor_t = nw_path_monitor_create().apply {
        val queue = dispatch_queue_create(
            "com.liftric.job.queue",
            dispatch_queue_attr_make_with_qos_class(
                null,
                QOS_CLASS_UTILITY,
                DISPATCH_QUEUE_PRIORITY_DEFAULT
            )
        )
        nw_path_monitor_set_queue(
            this,
            queue
        )
        nw_path_monitor_set_update_handler(this, networkMonitor)
    }

    fun startMonitoring() {
        nw_path_monitor_start(nwPathMonitor)
    }

    fun stopMonitoring() {
        nw_path_monitor_cancel(nwPathMonitor)
    }

    private fun checkReachability(network: nw_path_t) {
        when (nw_path_get_status(network)) {
            nw_path_status_satisfied -> {
                if (nw_path_uses_interface_type(network, nw_interface_type_wifi)) {
                    networkChannel.trySend(NetworkState.WIFI)
                } else if (nw_path_uses_interface_type(network, nw_interface_type_cellular)) {
                    networkChannel.trySend(NetworkState.MOBILE)
                }
            }
            nw_path_status_unsatisfied -> {
                networkChannel.trySend(NetworkState.NONE)
            }
        }
    }
}
