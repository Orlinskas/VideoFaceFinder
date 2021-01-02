package com.orlinskas.videofacefinder.data

import android.graphics.Rect
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class TypeConverter {

    private val gson = Gson()

    @TypeConverter
    fun valueToDate(json: String): Date = gson.fromJson(json)

    @TypeConverter
    fun dateToValue(date: Date): String = gson.toJson(date)

    @TypeConverter
    fun valueToFloatArray(json: String): FloatArray = gson.fromJson(json)

    @TypeConverter
    fun floatArrayToValue(array: FloatArray): String = gson.toJson(array)

    @TypeConverter
    fun valueToRect(json: String): Rect = gson.fromJson(json)

    @TypeConverter
    fun rectToValue(rect: Rect): String = gson.toJson(rect)

    inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)
}
