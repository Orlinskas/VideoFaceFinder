package com.orlinskas.videofacefinder.extensions

import android.os.Parcel

fun Parcel.writeBooleanValue(value: Boolean) = writeByte(if (value) 1 else 0)

fun Parcel.readBooleanValue(): Boolean = readByte() == 1.toByte()

fun String.removeFirstPathPart(): String {
    return if (this.contains("/")) {
        val startIndex = this.indexOf('/')
        this.removeRange(0, startIndex + 1)
    } else {
        this
    }
}