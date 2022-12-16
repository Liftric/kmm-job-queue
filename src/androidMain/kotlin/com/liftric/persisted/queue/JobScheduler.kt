package com.liftric.persisted.queue

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.serialization.modules.SerializersModule

actual class JobScheduler(
    context: Context,
    serializers: SerializersModule = SerializersModule {},
    configuration: Queue.Configuration? = null,
) : AbstractJobScheduler(
    serializers,
    configuration,
    SharedPreferencesSettings.Factory(context).create("com.liftric.persisted.queue")
)
