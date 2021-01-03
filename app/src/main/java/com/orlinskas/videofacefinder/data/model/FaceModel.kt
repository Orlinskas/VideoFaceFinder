package com.orlinskas.videofacefinder.data.model

import android.graphics.Rect
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.orlinskas.videofacefinder.data.Tables

@Keep
@Entity(tableName = Tables.FACE)
data class FaceModel (
        @PrimaryKey(autoGenerate = true)
        val id: Long,
        val name: String,
        val description: String,
        val data: FloatArray,
        val faceRect: Rect,
        val imageBase64: String,
        val frame: Long,
        val startSecond: Int,
        val videoName: String,
        val videoDescription: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceModel

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}