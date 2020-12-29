package com.orlinskas.videofacefinder.data.dao

import androidx.room.*
import com.orlinskas.videofacefinder.data.Tables
import com.orlinskas.videofacefinder.data.model.Frame

@Dao
interface FrameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFrames(frames: List<Frame>)

    @Update
    fun updateFrame(frame: Frame)

    @Query("SELECT * FROM ${Tables.FRAME}")
    fun getFrames(): List<Frame>

    @Query("DELETE FROM ${Tables.FRAME}")
    fun removeAllFrames()
}