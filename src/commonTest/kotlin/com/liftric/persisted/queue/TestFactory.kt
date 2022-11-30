package com.liftric.persisted.queue

import kotlin.reflect.KClass

class TestFactory: TaskFactory {
    override fun <T : Task> create(type: KClass<T>, params: Map<String, Any>): Task = when(type) {
        TestTask::class -> TestTask(params)
        LongRunningTask::class -> LongRunningTask(params)
        TestErrorTask::class -> TestErrorTask(params)
        else -> throw Exception("Unknown job!")
    }
}
