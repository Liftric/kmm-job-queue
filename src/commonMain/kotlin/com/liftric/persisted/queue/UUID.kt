package com.liftric.persisted.queue

import kotlinx.serialization.KSerializer

expect class UUID

internal expect object UUIDFactory {
    fun create(): UUID
}

expect object UUIDSerializer: KSerializer<UUID>
