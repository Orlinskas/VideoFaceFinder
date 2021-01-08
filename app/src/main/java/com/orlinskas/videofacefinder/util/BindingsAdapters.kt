package com.orlinskas.videofacefinder.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.orlinskas.videofacefinder.systems.ImageSystem

@BindingAdapter("setImageFromBase64")
fun setImageFromBase64(imageView: ImageView, base64: String?) {
    io {
        val bitmap = ImageSystem.decodeBitmapFromBase64(base64)

        main {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }
}
