package com.liftric.persisted.queue

import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

expect class UUID

expect fun KClass<UUID>.instance(): UUID

expect object UUIDSerializer: KSerializer<UUID>
