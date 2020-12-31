package com.orlinskas.videofacefinder.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.math.BigDecimal
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

    inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)
}
