package com.liftric.persisted.queue

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

interface JsonStorage {
    val keys: Set<String>
    fun get(id: String): String
    fun set(id: String, json: String)
    fun clear()
    fun remove(id: String)
}

class MapStorage: JsonStorage {
    private val store = mutableMapOf<String, String>()
    override val keys: Set<String>
        get() = store.keys
    override fun get(id: String): String {
        return store.getValue(id)
    }
    override fun set(id: String, json: String) {
        store[id] = json
    }
    override fun clear() {
        store.clear()
    }
    override fun remove(id: String) {
        store.remove(id)
    }
}

internal class SettingsStorage(private val store: Settings): JsonStorage {
    override val keys: Set<String>
        get() = store.keys
    override fun get(id: String): String {
        return store.getString(id, "")
    }
    override fun set(id: String, json: String) {
        store[id] = json
    }
    override fun clear() {
        store.clear()
    }
    override fun remove(id: String) {
        store.remove(id)
    }
}
