package com.liftric.job.queue

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

actual class JobQueueTests : AbstractJobQueueTests(
    JobQueue(
        serializers = SerializersModule {
            polymorphic(Task::class) {
                subclass(TestTask::class, TestTask.serializer())
            }
        },
        store = MapStorage(),
        networkListener = NetworkListener(networkManager = NetworkManager())
    )
)
