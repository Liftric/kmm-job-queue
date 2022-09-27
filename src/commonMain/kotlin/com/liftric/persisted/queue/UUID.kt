package com.liftric.persisted.queue

import kotlin.reflect.KClass

expect class UUID

expect fun KClass<UUID>.instance(): UUID
