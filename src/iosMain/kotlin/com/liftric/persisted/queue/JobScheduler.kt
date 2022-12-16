package com.liftric.persisted.queue

import com.russhwolf.settings.NSUserDefaultsSettings
import kotlinx.serialization.modules.SerializersModule
import platform.Foundation.NSUserDefaults

actual class JobScheduler(
    serializers: SerializersModule = SerializersModule {},
    configuration: Queue.Configuration? = null
) : AbstractJobScheduler(
    serializers,
    configuration,
    NSUserDefaultsSettings(NSUserDefaults("com.liftric.persisted.queue"))
)
