package com.orlinskas.videofacefinder.extensions

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.convertToDateStr(format: String): String {
    return SimpleDateFormat(format, Locale.ENGLISH).format(Date(this))
}

fun Long.convertToStringRepresentation(): String {
    val K: Long = 1024
    val M = K * K
    val G = M * K
    val T = G * K

    val dividers = longArrayOf(T, G, M, K, 1)
    val units = arrayOf("TB", "GB", "MB", "KB", "B")

    var result = ""

    try {
        for (i in dividers.indices) {
            val divider = dividers[i]
            if (this >= divider) {
                result = format(this, divider, units[i]) ?: ""
                break
            }
        }
    } finally {
        return result
    }
}

private fun format(
    value: Long,
    divider: Long,
    unit: String
): String? {
    val result = if (divider > 1) value.toDouble() / divider.toDouble() else value.toDouble()
    return DecimalFormat("#,##0.#").format(result).toString() + " " + unit
}
