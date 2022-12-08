package com.liftric.persisted.queue

import com.liftric.persisted.queue.rules.ConnectionType

actual abstract class Context
object IosContext : Context()

actual class NetworkConnection {
    actual fun getConnectionType(context: Context?): ConnectionType{
        // TODO
        return ConnectionType.Undefined()
    }
}