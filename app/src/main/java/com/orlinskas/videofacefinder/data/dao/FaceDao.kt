package com.orlinskas.videofacefinder.data.dao

import androidx.room.*
import com.orlinskas.videofacefinder.data.Tables
import com.orlinskas.videofacefinder.data.model.FaceModel

@Dao
interface FaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFaces(frames: List<FaceModel>)

    @Update
    fun updateFace(face: FaceModel)

    @Query("SELECT * FROM ${Tables.FACE}")
    fun getFaces(): List<FaceModel>

    @Query("DELETE FROM ${Tables.FACE}")
    fun removeAllFaces()
}