package com.orlinskas.videofacefinder.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.orlinskas.videofacefinder.data.Tables

@Keep
@Entity(tableName = Tables.FRAME)
data class Frame (
    @PrimaryKey
    val id: Long,
    val absolutePath: String,
    val startSecond: Int
)