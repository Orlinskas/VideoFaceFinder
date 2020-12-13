package com.orlinskas.videofacefinder.exception

import androidx.annotation.Keep

@Keep
data class ErrorResponse(
    val error: String?,
    val message: String?
)