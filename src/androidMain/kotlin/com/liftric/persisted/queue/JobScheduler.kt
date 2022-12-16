package com.liftric.persisted.queue

import android.content.Context
import kotlinx.serialization.modules.SerializersModule

actual class JobScheduler(
    context: Context,
    serializers: SerializersModule = SerializersModule {},
    configuration: Queue.Configuration? = null,
    filePath: String? = null
) : AbstractJobScheduler(
    serializers,
    configuration,
    filePath ?: context.filesDir.path
)
