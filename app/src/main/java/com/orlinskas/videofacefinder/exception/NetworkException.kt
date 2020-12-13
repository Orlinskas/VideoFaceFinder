package com.orlinskas.videofacefinder.exception

import android.content.Context
import okhttp3.ResponseBody

/**
 * General exception for server responses
 */
open class NetworkException(
    val code: Int,
    private val responseMessage: String?,
    val errorResponse: ErrorResponse?,
    responseBody: ResponseBody?,
    resource: Int = 0
) :
    HandledException(resource) {

    private val contentType = responseBody?.contentType()
    private val source = responseBody?.string()

    val body: ResponseBody?
        get() = if (contentType == null || source == null) {
            null
        } else {
            ResponseBody.create(contentType, source)
        }

    override fun getText(context: Context): String =
        responseMessage ?: "Error"
}

/**
 * Exception indicates server-side errors with code 5XX
 */
class ServerException(code: Int, responseMessage: String?, errorResponse: ErrorResponse?, responseBody: ResponseBody?) :
    NetworkException(code, responseMessage, errorResponse, responseBody)
/**
 * Exception that indicates that internet connection or server not available
 */
class NoConnectionException : HandledException("No connection")
