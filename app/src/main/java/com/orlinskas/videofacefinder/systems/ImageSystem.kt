package com.orlinskas.videofacefinder.systems

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Base64
import timber.log.Timber
import java.io.ByteArrayOutputStream

object ImageSystem {

    private const val BASE_64_ENCODER_COMPRESS_VALUE = 100

    fun encodeBitmapToBase64(bitmap: Bitmap?): String? {
        if (bitmap == null) {
            return null
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, BASE_64_ENCODER_COMPRESS_VALUE, byteArrayOutputStream)

        val bytes: ByteArray = byteArrayOutputStream.toByteArray()

        byteArrayOutputStream.close()

        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    fun decodeBitmapFromBase64(base64: String?): Bitmap? {
        val decodedString: ByteArray = Base64.decode(base64, Base64.DEFAULT)
        val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        if (decodedBitmap == null) {
            Timber.e("Decoded base64 error")
            return null
        }

        return decodedBitmap
    }

    fun resize(sourceBitmap: Bitmap?, desireHeight: Int, desireWidth: Int): Bitmap? {
        if (sourceBitmap == null) {
            Timber.e("Wrong parameter")
            return null
        }

        val srcRatio: Float
        val resizedWidth: Int
        val resizedHeight: Int

        val srcWidth: Int = sourceBitmap.width
        val srcHeight: Int = sourceBitmap.height

        if (srcWidth <= desireWidth || srcHeight <= desireHeight || srcWidth <= desireWidth && srcHeight <= desireHeight) {
            return sourceBitmap
        }

        srcRatio = srcWidth.toFloat() / srcHeight.toFloat()

        val boxRatio: Float = desireWidth.toFloat() / desireHeight.toFloat()

        if (srcRatio < boxRatio) {
            resizedHeight = desireHeight
            resizedWidth = (resizedHeight.toFloat() * srcRatio).toInt()
        } else {
            resizedWidth = desireWidth
            resizedHeight = (resizedWidth.toFloat() / srcRatio).toInt()
        }
        val resizedBitmap = Bitmap.createScaledBitmap(sourceBitmap, resizedWidth, resizedHeight, true)

        if (sourceBitmap != resizedBitmap) {
            recycleBitmap(sourceBitmap)
        }

        return resizedBitmap
    }

    private fun recycleBitmap(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return
        bitmap.recycle()
        System.gc()
    }

    fun getSubImage(bitmap: Bitmap, copyRect: Rect): Bitmap {
        val subImage = Bitmap.createBitmap(copyRect.width(), copyRect.height(), Bitmap.Config.RGB_565)
        val canvas = Canvas(subImage)
        canvas.drawBitmap(bitmap, copyRect, Rect(0, 0, copyRect.width(), copyRect.height()), null)

        return subImage
    }
}