package com.orlinskas.videofacefinder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DefaultModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long
)