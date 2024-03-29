package com.liftric.job.queue

import com.russhwolf.settings.NSUserDefaultsSettings
import kotlinx.serialization.modules.SerializersModule
import platform.Foundation.NSUserDefaults

actual class JobQueue(
    serializers: SerializersModule = SerializersModule {},
    configuration: Queue.Configuration = Queue.DefaultConfiguration,
    store: JsonStorage = SettingsStorage(NSUserDefaultsSettings(NSUserDefaults("com.liftric.persisted.queue")))
) : AbstractJobQueue(
    serializers,
    configuration,
    store
)
