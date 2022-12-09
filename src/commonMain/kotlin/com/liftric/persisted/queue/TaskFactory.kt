package com.liftric.persisted.queue

import kotlin.reflect.KClass

interface TaskFactory {
    fun <T: Task> create(type: KClass<T>, params: Map<String, Any>): Task
}
