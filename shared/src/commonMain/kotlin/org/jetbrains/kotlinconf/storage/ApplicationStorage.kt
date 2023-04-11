package org.jetbrains.kotlinconf.storage

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.properties.*
import kotlin.reflect.*

expect class ApplicationContext

expect fun ApplicationStorage(context: ApplicationContext): ApplicationStorage

interface ApplicationStorage {
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putString(key: String, value: String)
    fun getString(key: String): String?
}

inline fun <reified T> ApplicationStorage.put(key: String, value: T) {
    putString(key, Json.encodeToString(value))
}

inline fun <reified T> ApplicationStorage.get(key: String): T? {
    val value = getString(key) ?: return null
    return Json.decodeFromString(value)
}

inline fun <reified T> ApplicationStorage.bind(
    serializer: KSerializer<T>,
    crossinline block: () -> T
): ReadWriteProperty<Any, T> = object : ReadWriteProperty<Any, T> {
    private var currentValue: T? = null

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        val key = property.name
        currentValue = value
        putString(key, Json.encodeToString(serializer, value))
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        currentValue?.let { return it }

        val key = property.name

        val result = try {
            getString(key)?.let { Json.decodeFromString(serializer, it) }
        } catch (cause: Throwable) {
            null
        } ?: block()

        setValue(thisRef, property, result)
        return result
    }
}
