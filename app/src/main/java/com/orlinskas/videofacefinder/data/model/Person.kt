package com.orlinskas.videofacefinder.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.orlinskas.videofacefinder.data.Tables

@Keep
@Entity(tableName = Tables.PERSON)
data class Person (
        @PrimaryKey(autoGenerate = true)
        val id: Long,
        val name: String = "Person $id",
        val description: String = "",
        val standardFaceBase64: String? = null,
        val faces: List<Long>
)
