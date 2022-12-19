package com.liftric.persisted.queue

import kotlinx.serialization.KSerializer

expect class UUID

internal expect object UUIDFactory {
    fun create(): UUID
    fun fromString(string: String): UUID
}

expect object UUIDSerializer: KSerializer<UUID>
