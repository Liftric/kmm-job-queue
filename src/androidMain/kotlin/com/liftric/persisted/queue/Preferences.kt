package com.liftric.persisted.queue

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

actual class Preferences(context: Context): AbstractPreferences(SharedPreferencesSettings.Factory(context).create("com.liftric.job.scheduler"))
