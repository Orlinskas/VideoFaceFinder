package com.orlinskas.videofacefinder.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.orlinskas.videofacefinder.data.Tables
import java.util.*

@Keep
@Entity(tableName = Tables.FRAME)
data class Frame (
    @PrimaryKey
    val id: Long,
    val absolutePath: String,
    val startSecond: Int,
    val videoName: String = "",
    val videoDescription: String = "",
    val videoCreateDate: Date = Calendar.getInstance().time
)