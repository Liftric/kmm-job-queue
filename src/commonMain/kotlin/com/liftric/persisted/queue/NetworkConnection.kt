package com.liftric.persisted.queue

import com.liftric.persisted.queue.rules.ConnectionType

expect abstract class Context

expect class NetworkConnection() {
    fun getConnectionType(context: Context?): ConnectionType
}