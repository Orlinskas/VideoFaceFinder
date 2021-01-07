package com.orlinskas.videofacefinder.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.orlinskas.videofacefinder.data.dao.FaceDao
import com.orlinskas.videofacefinder.data.dao.FrameDao
import com.orlinskas.videofacefinder.data.dao.PersonsDao
import com.orlinskas.videofacefinder.data.model.FaceModel
import com.orlinskas.videofacefinder.data.model.Frame
import com.orlinskas.videofacefinder.data.model.Person

@Database(
    entities = [
        Frame::class,
        FaceModel::class,
        Person::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(TypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun frameDao(): FrameDao

    abstract fun faceDao(): FaceDao

    abstract fun personDao(): PersonsDao
}
