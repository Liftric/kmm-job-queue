package com.liftric.persisted.queue

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.russhwolf.settings.MapSettings
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
actual class JobSchedulerTests: AbstractJobSchedulerTests({
    JobScheduler(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        serializers = SerializersModule {
            polymorphic(Task::class) {
                subclass(TestTask::class, TestTask.serializer())
            }
        },
        settings = MapSettings()
    )
})
