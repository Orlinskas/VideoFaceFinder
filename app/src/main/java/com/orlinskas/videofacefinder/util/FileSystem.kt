package com.orlinskas.videofacefinder.util

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.orlinskas.videofacefinder.data.enums.VideoMimeType
import com.orlinskas.videofacefinder.data.model.UserFile
import com.orlinskas.videofacefinder.extensions.removeFirstPathPart
import java.util.*

object FileSystem {

    fun Uri.toFileModel(resolver: ContentResolver): UserFile? {

        path?.let {

            return UserFile(
                name = getFileName(resolver, this),
                type = getFileMimeType(resolver, this, true),
                mimeType = getFileMimeType(resolver, this, false),
                size = getFileSize(resolver, this),
                path = it,
                uri = this
            )
        }

        return null
    }

    private fun getFileName(resolver: ContentResolver, uri: Uri): String {
        val cursor: Cursor? = resolver.query(uri, null, null, null, null)

        return if (cursor != null) {
            val nameIndex: Int = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val name: String = cursor.getString(nameIndex)
            cursor.close()
            name
        } else {
            "unknown file name"
        }
    }

    private fun getFileMimeType(resolver: ContentResolver, uri: Uri, humanReadable: Boolean = false): String {

        val type = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            resolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase(Locale.ROOT))
        }

        return type?.removeFirstPathPart()?.toUpperCase(Locale.ROOT) ?: "unknown"
    }

    private fun getFileSize(resolver: ContentResolver, uri: Uri): Long {
        val cursor: Cursor? = resolver.query(uri, null, null, null, null)

        return if (cursor != null) {
            val columnIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            val size = cursor.getLong(columnIndex)
            cursor.close()
            size
        } else {
            0L
        }
    }
}