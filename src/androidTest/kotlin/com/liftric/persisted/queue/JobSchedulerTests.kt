package com.liftric.persisted.queue

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

actual class JobSchedulerTests: AbstractJobSchedulerTests(JobScheduler(
    context = InstrumentationRegistry.getInstrumentation().targetContext,
    serializers = SerializersModule {
        polymorphic(DataTask::class) {
            subclass(TestTask::class, TestTask.serializer())
        }
    }
))
