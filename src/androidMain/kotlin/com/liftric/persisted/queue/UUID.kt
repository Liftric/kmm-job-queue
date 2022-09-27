package com.liftric.persisted.queue

import java.util.UUID
import kotlin.reflect.KClass

actual typealias UUID = UUID

actual fun KClass<UUID>.instance(): UUID = UUID.randomUUID()
