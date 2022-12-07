package com.liftric.persisted.queue

import com.liftric.persisted.queue.rules.NetworkType

expect class NetworkConnection() {
     fun getConnectionType(): NetworkType
}