package com.liftric.persisted.queue

import kotlin.reflect.KClass

class TestFactory: JobFactory {
    override fun <T : Job> create(type: KClass<T>, params: Map<String, Any>): Job = when(type) {
        TestJob::class -> TestJob(params)
        else -> throw Exception("Unknown job!")
    }
}
