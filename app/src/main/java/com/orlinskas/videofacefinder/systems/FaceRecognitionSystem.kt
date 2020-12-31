package com.orlinskas.videofacefinder.systems

import android.graphics.Bitmap
import com.orlinskas.videofacefinder.tflite.TFLiteClassifier

object FaceRecognitionSystem {

    fun recognize(bitmap: Bitmap, classifier: TFLiteClassifier): FloatArray {
        return classifier.recognize(bitmap)
    }
}