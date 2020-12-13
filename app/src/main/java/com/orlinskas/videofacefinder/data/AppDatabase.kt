package com.orlinskas.videofacefinder.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.orlinskas.videofacefinder.data.model.DefaultModel

@Database(
    entities = [
        DefaultModel::class
    ],
    version = 1,
    exportSchema = false
)
//@TypeConverters(TypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    // dao list
}
