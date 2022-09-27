package com.liftric.persisted.queue

import platform.Foundation.NSUUID
import kotlin.reflect.KClass

actual typealias UUID = NSUUID

actual fun KClass<UUID>.instance(): UUID = NSUUID()
