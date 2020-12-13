package com.orlinskas.videofacefinder.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.math.BigDecimal

class TypeConverter {

    private val gson = Gson()

//    @TypeConverter
//    fun realizationToJson(products: List<Realization>?): String? {
//        return if (products == null || products.isEmpty()) {
//            null
//        } else {
//            gson.toJson(products)
//        }
//    }
//
//    @TypeConverter
//    fun jsonToRealization(json: String?): List<Realization> {
//        return if (json.isNullOrEmpty()) {
//            listOf()
//        } else {
//            val type = object : TypeToken<List<Realization>>() {}.type
//            gson.fromJson(json, type)
//        }
//    }
}
