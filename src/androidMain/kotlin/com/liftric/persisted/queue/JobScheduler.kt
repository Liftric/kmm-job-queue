package com.liftric.persisted.queue

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.serialization.modules.SerializersModule

actual class JobScheduler(
    context: Context,
    serializers: SerializersModule = SerializersModule {},
    configuration: Queue.Configuration? = null,
    settings: Settings = SharedPreferencesSettings.Factory(context).create("com.liftric.persisted.queue")
) : AbstractJobScheduler(
    serializers,
    configuration,
    settings
)
