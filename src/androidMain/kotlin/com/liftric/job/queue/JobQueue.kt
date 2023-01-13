package com.liftric.job.queue

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.serialization.modules.SerializersModule

actual class JobQueue(
    context: Context,
    serializers: SerializersModule = SerializersModule {},
    configuration: Queue.Configuration = Queue.DefaultConfiguration,
    store: JsonStorage = SettingsStorage(SharedPreferencesSettings.Factory(context).create("com.liftric.persisted.queue"))
) : AbstractJobQueue(
    serializers,
    configuration,
    store
)
