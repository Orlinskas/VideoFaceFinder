package com.orlinskas.videofacefinder.systems

import android.graphics.Bitmap
import com.orlinskas.videofacefinder.tflite.TFLiteClassifier
import kotlin.math.sqrt

object FaceRecognitionSystem {

    fun recognize(bitmap: Bitmap, classifier: TFLiteClassifier): FloatArray {
        return classifier.recognize(bitmap)
    }

    fun compare(firstData: FloatArray, secondData: FloatArray): Float {
        var ret: Float? = null
        var distance = 0f

        for (i in firstData.indices) {
            val diff: Float = firstData[i] - secondData[i]
            distance += diff * diff
        }

        distance = sqrt(distance.toDouble()).toFloat()

        if (ret == null || distance < ret) {
            ret = distance
        }

        return ret
    }
}