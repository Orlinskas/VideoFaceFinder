package com.orlinskas.videofacefinder.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.orlinskas.videofacefinder.data.dao.FrameDao
import com.orlinskas.videofacefinder.data.model.DefaultModel
import com.orlinskas.videofacefinder.data.model.Frame

@Database(
    entities = [
        DefaultModel::class,
        Frame::class
    ],
    version = 1,
    exportSchema = false
)
//@TypeConverters(TypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun frameDao(): FrameDao
}
