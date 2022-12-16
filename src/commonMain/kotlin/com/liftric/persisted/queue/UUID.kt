package com.liftric.persisted.queue

import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

expect class UUID

internal expect object UUIDFactory {
    fun create(): UUID
}

expect object UUIDSerializer: KSerializer<UUID>
