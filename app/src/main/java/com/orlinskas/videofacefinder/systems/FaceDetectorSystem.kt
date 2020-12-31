package com.orlinskas.videofacefinder.systems

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.util.SparseArray
import androidx.core.graphics.toRect
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import timber.log.Timber

object FaceDetectorSystem {

    fun findFaces(bitmap: Bitmap, faceDetector: FaceDetector): SparseArray<Face>? {
        val operationStartTime = System.currentTimeMillis()

        val detectorFrame = Frame.Builder().setBitmap(bitmap).build()
        val faces = faceDetector.detect(detectorFrame)

        Timber.d("Detect faces on bitmap, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")

        return faces
    }

    fun findFaceRect(face: Face): Rect {
        val x1 = face.position.x
        val y1 = face.position.y
        val x2 = x1 + face.width
        val y2 = y1 + face.height

        return RectF(x1, y1, x2, y2).toRect()
    }
}