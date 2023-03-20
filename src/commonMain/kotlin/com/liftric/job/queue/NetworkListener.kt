package com.liftric.job.queue

import com.liftric.job.queue.rules.NetworkState
import kotlinx.coroutines.CoroutineScope

expect class NetworkListener {
    actual var networkState: NetworkState
    val scope: CoroutineScope
    fun observeNetworkState()
}
