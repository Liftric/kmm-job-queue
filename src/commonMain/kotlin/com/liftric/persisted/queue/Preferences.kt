package com.liftric.persisted.queue

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

expect class Preferences: AbstractPreferences
abstract class AbstractPreferences(private val settings: Settings): JobSerializer {
    override val tag: String = ""
    override fun retrieve(id: String): Job? {
        val jsonString = settings.get<String>(id) ?: return null
        return Json.decodeFromString(jsonString)
    }

    override fun retrieveAll(): List<Job> {
        return settings.keys.mapNotNull { retrieve(it) }
    }

    override fun store(job: Job) {
        settings[job.id.toString()] = Json.encodeToString(job)
    }
}
