package com.liftric.persisted.queue

import kotlin.reflect.KClass

interface JobFactory {
    fun <T: Job> create(type: KClass<T>, params: Map<String, Any>): Job
}
