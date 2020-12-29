package com.orlinskas.videofacefinder.util

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.orlinskas.videofacefinder.data.enums.VideoMimeType
import com.orlinskas.videofacefinder.data.model.UserFile
import com.orlinskas.videofacefinder.extensions.removeFirstPathPart
import timber.log.Timber
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

    fun UserFile.getAbsolutePath(contentResolver: ContentResolver): String? {
        // TODO other formats
        // when (this.format) {
        //
        // }

        return getVideoPath(this.uri, contentResolver)
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

    private fun getVideoPath(uri: Uri?, contentResolver: ContentResolver): String? {
        if (uri == null) {
            Timber.e("Uri is null")
            return null
        }

        var cursor = contentResolver.query(uri, null, null, null, null)

        if (cursor == null) {
            Timber.e("Cursor is null")
            return null
        }

        cursor.moveToFirst()
        var documentId: String = cursor.getString(0)
        documentId = documentId.substring(documentId.lastIndexOf(":") + 1)
        cursor.close()

        cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            null, MediaStore.Video.Media._ID + " = ? ", arrayOf(documentId), null)

        if (cursor == null) {
            Timber.e("Cursor is null")
            return null
        }

        cursor.moveToFirst()
        val path: String = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        cursor.close()

        return path
    }

    private fun getImagePath(uri: Uri?, contentResolver: ContentResolver): String? {
        if (uri == null) {
            Timber.e("Uri is null")
            return null
        }

        var cursor = contentResolver.query(uri, null, null, null, null)

        if (cursor == null) {
            Timber.e("Cursor is null")
            return null
        }

        cursor.moveToFirst()
        var documentId: String = cursor.getString(0)
        documentId = documentId.substring(documentId.lastIndexOf(":") + 1)
        cursor.close()

        cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", arrayOf(documentId), null)

        if (cursor == null) {
            Timber.e("Cursor is null")
            return null
        }

        cursor.moveToFirst()
        val path: String = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        cursor.close()

        return path
    }
}