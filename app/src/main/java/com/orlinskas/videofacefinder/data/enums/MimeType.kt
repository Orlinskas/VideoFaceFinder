package com.orlinskas.videofacefinder.data.enums

import androidx.annotation.Keep

@Keep
enum class VideoMimeType(val value: String) {
    MPEG("video/mpeg"),
    MP4("video/mp4"),
    M3GP("video/3gpp"),
    M3G2("video/3gpp2"),
    MKV("video/x-matroska"),
    WEBM("video/webm"),
    TS("video/mp2ts"),
    AVI("video/avi")
}

@Keep
enum class ImageMimeType(val value: String) {
    PDF("application/pdf"),
    JPEG("image/jpeg"),
    PNG("image/png")
}