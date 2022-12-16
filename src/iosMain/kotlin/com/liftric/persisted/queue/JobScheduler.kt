package com.liftric.persisted.queue

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import kotlinx.serialization.modules.SerializersModule
import platform.Foundation.NSUserDefaults

actual class JobScheduler(
    serializers: SerializersModule = SerializersModule {},
    configuration: Queue.Configuration? = null,
    settings: Settings = NSUserDefaultsSettings(NSUserDefaults("com.liftric.persisted.queue"))
) : AbstractJobScheduler(
    serializers,
    configuration,
    settings
)
