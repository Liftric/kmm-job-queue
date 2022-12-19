package com.liftric.persisted.queue

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.serialization.modules.SerializersModule

actual class JobScheduler(
    context: Context,
    serializers: SerializersModule = SerializersModule {},
    configuration: Queue.Configuration? = null,
    store: JsonStorage = SettingsStorage(SharedPreferencesSettings.Factory(context).create("com.liftric.persisted.queue"))
) : AbstractJobScheduler(
    serializers,
    configuration,
    store
)
