package com.orlinskas.videofacefinder.data.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.orlinskas.videofacefinder.extensions.convertToStringRepresentation
import com.orlinskas.videofacefinder.extensions.readBooleanValue
import com.orlinskas.videofacefinder.extensions.writeBooleanValue
import kotlinx.android.parcel.Parceler
import kotlinx.android.parcel.Parcelize

@Keep
@Parcelize
data class UserFile(
    val name: String,
    val type: String,
    val mimeType: String,
    val size: Long,
    val path: String,
    val uri: Uri?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        name = if (parcel.readBooleanValue()) parcel.readString() ?: "" else "",
        type = if (parcel.readBooleanValue()) parcel.readString() ?: "" else "",
        mimeType = if (parcel.readBooleanValue()) parcel.readString() ?: "" else "",
        size = if (parcel.readBooleanValue()) parcel.readLong() else 0L,
        path = if (parcel.readBooleanValue()) parcel.readString() ?: "" else "",
        uri = if (parcel.readBooleanValue()) parcel.readParcelable<Uri>(Uri::class.java.classLoader) else null
    )

    override fun describeContents(): Int = 0

    fun sizeInText(): String {
        return size.convertToStringRepresentation()
    }

    companion object : Parceler<UserFile> {

        override fun UserFile.write(parcel: Parcel, flags: Int) {

            with(parcel) {
                writeBooleanValue(true)
                writeString(name)
                writeBooleanValue(true)
                writeString(type)
                writeBooleanValue(true)
                writeString(mimeType)
                writeBooleanValue(true)
                writeLong(size)
                writeBooleanValue(true)
                writeString(path)
                writeBooleanValue(uri != null)
                uri?.let {
                    writeParcelable(uri, 0)
                }
            }
        }

        override fun create(parcel: Parcel): UserFile {
            return UserFile(parcel)
        }
    }
}



