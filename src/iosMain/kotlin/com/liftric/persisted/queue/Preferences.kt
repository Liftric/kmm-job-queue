package com.liftric.persisted.queue

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

actual class Preferences: AbstractPreferences(NSUserDefaultsSettings(NSUserDefaults(suiteName = "com.liftric.job.scheduler")))
