package com.orlinskas.videofacefinder.data.enums

import androidx.annotation.Keep

@Keep
enum class FileSystemState {
    OK,
    FILE_TO_BIG,
    MAX_FILES_SUM_TO_BIG,
    NOT_VALID_FORMAT,
    EXCEPTION,
    FILES_COUNT_TO_BIG
}