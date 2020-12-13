package com.orlinskas.videofacefinder.extensions

fun StringBuilder.and(block: ((StringBuilder) -> (Unit))) {
    val notEmpty = isNotEmpty()
    if (notEmpty) {
        append(" AND (")
    }
    block.invoke(this)
    if (notEmpty) {
        append(")")
    }
}

fun StringBuilder.or(block: ((StringBuilder) -> (Unit))) {
    val notEmpty = isNotEmpty()
    if (notEmpty) {
        append(" OR (")
    }
    block.invoke(this)
    if (notEmpty) {
        append(")")
    }
}
