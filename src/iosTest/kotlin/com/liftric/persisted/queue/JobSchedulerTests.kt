package com.liftric.persisted.queue

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

actual class JobSchedulerTests: AbstractJobSchedulerTests(JobScheduler(
    serializers = SerializersModule {
        polymorphic(DataTask::class) {
            subclass(TestTask::class, TestTask.serializer())
        }
    }
))
