package com.example.recognition_app.utils

import androidx.room.TypeConverter
import java.text.SimpleDateFormat
import java.util.*

object Converters {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    @TypeConverter
    @JvmStatic
    fun fromTimestamp(value: String?): Date? {
        return value?.let { dateFormat.parse(it) }
    }

    @TypeConverter
    @JvmStatic
    fun dateToTimestamp(date: Date?): String? {
        return date?.let { dateFormat.format(it) }
    }
}