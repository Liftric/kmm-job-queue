package com.liftric.job.queue

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
actual class JobQueueTests : AbstractJobQueueTests(
    JobQueue(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        serializers = SerializersModule {
            polymorphic(Task::class) {
                subclass(TestTask::class, TestTask.serializer())
            }
        },
        store = MapStorage(),
        networkListener = NetworkListener(
            networkManager = NetworkManager(InstrumentationRegistry.getInstrumentation().context)
        )
    )
)

