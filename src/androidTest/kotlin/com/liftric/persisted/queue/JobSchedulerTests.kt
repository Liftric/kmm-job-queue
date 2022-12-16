package com.liftric.persisted.queue

import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
actual class JobSchedulerTests: AbstractJobSchedulerTests(JobScheduler(
    context = ApplicationProvider.getApplicationContext(),
    serializers = SerializersModule {
        polymorphic(DataTask::class) {
            subclass(TestTask::class, TestTask.serializer())
        }
    }
))
