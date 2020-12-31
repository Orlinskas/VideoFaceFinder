package com.orlinskas.videofacefinder.systems

import android.graphics.Bitmap
import com.orlinskas.videofacefinder.tflite.SimilarityClassifier

object FaceRecognitionSystem {

    fun recognize(bitmap: Bitmap, classifier: SimilarityClassifier): FloatArray {
        return classifier.recognize(bitmap)
    }
}