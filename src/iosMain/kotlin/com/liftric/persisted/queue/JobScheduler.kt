package com.liftric.persisted.queue

import kotlinx.serialization.modules.SerializersModule
import platform.Foundation.NSHomeDirectory

actual class JobScheduler(
    serializers: SerializersModule = SerializersModule {},
    configuration: Queue.Configuration? = null,
    filePath: String = NSHomeDirectory()
) : AbstractJobScheduler(
    serializers,
    configuration,
    filePath
)
