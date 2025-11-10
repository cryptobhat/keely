package com.kannada.kavi.data.database.converters

import androidx.room.TypeConverter
import com.kannada.kavi.core.common.ClipboardContentType
import org.json.JSONObject

/**
 * Room TypeConverters for converting complex types to/from database-compatible types.
 * These converters handle enum types and JSON objects used in database entities.
 */
class TypeConverters {

    /**
     * Convert ClipboardContentType enum to String for database storage.
     */
    @TypeConverter
    fun fromClipboardContentType(value: ClipboardContentType): String {
        return value.name
    }

    /**
     * Convert String back to ClipboardContentType enum.
     */
    @TypeConverter
    fun toClipboardContentType(value: String): ClipboardContentType {
        return try {
            ClipboardContentType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ClipboardContentType.TEXT // Default fallback
        }
    }

    /**
     * Convert Map<String, Any> to JSON String for database storage.
     * Used for analytics event properties.
     */
    @TypeConverter
    fun fromPropertiesMap(value: Map<String, Any>?): String? {
        if (value == null) return null
        return try {
            val jsonObject = JSONObject()
            value.forEach { (key, mapValue) ->
                jsonObject.put(key, mapValue)
            }
            jsonObject.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert JSON String back to Map<String, Any>.
     * Used for analytics event properties.
     */
    @TypeConverter
    fun toPropertiesMap(value: String?): Map<String, Any>? {
        if (value == null) return null
        return try {
            val jsonObject = JSONObject(value)
            val map = mutableMapOf<String, Any>()
            jsonObject.keys().forEach { key ->
                val jsonValue = jsonObject.get(key)
                map[key] = jsonValue
            }
            map
        } catch (e: Exception) {
            null
        }
    }
}
