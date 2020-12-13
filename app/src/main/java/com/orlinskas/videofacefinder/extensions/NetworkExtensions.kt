package com.orlinskas.videofacefinder.extensions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.orlinskas.videofacefinder.exception.*
import com.orlinskas.videofacefinder.util.Wish
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.HashMap

fun <T> Call<T>.call(): Wish<T> {
    return try {
        val result = execute()
        if (result.isSuccessful) {
            Wish(result.body()!!)
        } else {
            errorResponse(result.code(), result.errorBody())
        }
    } catch (t: Throwable) {
        when (t) {
            is SocketTimeoutException, is UnknownHostException, is ConnectException, is SocketException -> Wish(NoConnectionException())
            is HttpException -> errorResponse(t)
            else -> {
                Timber.w(t)
                Wish(HandledException(t.message ?: "Server connection error"))
            }
        }
    }
}

@Suppress("SENSELESS_COMPARISON")
private fun <T> errorResponse(code: Int, body: ResponseBody?): Wish<T> {
    var errorResponse: ErrorResponse? = null
    var source: String? = null
    val message = if (body == null) {
        null
    } else {
        val raw = body.string()
        source = raw
        if (raw == null) {
            null
        } else {
            try {
                val type = object : TypeToken<ErrorResponse?>() {}.type!!
                errorResponse = Gson().fromJson(raw, type)
                errorResponse?.message
            } catch (e: Exception) {
                null
            }
        }
    }
    val exception = when (code) {
        HttpURLConnection.HTTP_UNAUTHORIZED -> ServerException(code, message, errorResponse, body)
        in HttpURLConnection.HTTP_INTERNAL_ERROR..599 -> ServerException(code, message, errorResponse, body)
        else -> NetworkException(code, message, errorResponse, ResponseBody.create(body?.contentType(), source ?: ""))
    }
    return Wish(exception)
}

@Suppress("UNNECESSARY_SAFE_CALL")
private fun <T> errorResponse(httpException: HttpException): Wish<T> {
    val errorResponse = parseErrorResponse(httpException.response())
    val message = if (errorResponse?.message == null) httpException.message() else errorResponse.message
    val exception = when (httpException.code()) {
        HttpURLConnection.HTTP_UNAUTHORIZED -> ServerException(
            httpException.code(),
            message,
            errorResponse,
            httpException.response()?.errorBody()
        )
        in HttpURLConnection.HTTP_INTERNAL_ERROR..599 -> ServerException(
            httpException.code(),
            message,
            errorResponse,
            httpException.response()?.errorBody()
        )
        else -> NetworkException(httpException.code(), message, errorResponse, httpException.response()?.errorBody())
    }
    return Wish(exception)
}

private fun <T> parseErrorResponse(response: Response<T>): ErrorResponse? {
    val rawError = if (response.errorBody() != null) response.errorBody()!!.string() else null
    return if (rawError == null) null else {
        try {
            val type = object : TypeToken<ErrorResponse?>() {}.type!!
            Gson().fromJson<ErrorResponse?>(rawError, type)
        } catch (e: Exception) {
            null
        }
    }
}

fun HashMap<String, RequestBody>.putTextBody(field: String, value: String?) {
    value?.let {
        this[field] = RequestBody.create(MediaType.parse("text/plain"), it)
    }
}
