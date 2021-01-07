package com.orlinskas.videofacefinder.ui.viewstate

import android.os.Bundle
import com.orlinskas.videofacefinder.core.ViewState
import com.orlinskas.videofacefinder.data.enums.Settings
import com.orlinskas.videofacefinder.data.model.UserFile
import com.orlinskas.videofacefinder.systems.FaceDataSimpleClassifier

const val KEY_FILE = "FILE"
const val KEY_FPS = "KEY_FPS"
const val KEY_COMPRESS = "KEY_COMPRESS"
const val KEY_SCALE = "KEY_SCALE"

class FileViewState : ViewState {

    var file: UserFile? = null
    var frameParams: FaceDataSimpleClassifier.Companion.FrameParams? = null

    var fps = Settings.Fps.DEFAULT
    var compress = Settings.Compress.DEFAULT
    var scale = Settings.Scale.DEFAULT

    var isProgress = false

    override fun onRestoreState(bundle: Bundle?) {
        super.onRestoreState(bundle)
        bundle?.let {
            file = it.getParcelable(KEY_FILE)
            fps = Settings.Fps.fromValue(it.getFloat(KEY_FPS))
            compress = Settings.Compress.fromValue(it.getFloat(KEY_COMPRESS))
            scale = Settings.Scale.fromValue(it.getFloat(KEY_SCALE))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        super.onSaveState(bundle)
        bundle?.let {
            with(bundle) {
                putParcelable(KEY_FILE, file)
                putFloat(KEY_FPS, Settings.Fps.getSliderValue(fps))
                putFloat(KEY_COMPRESS, Settings.Compress.getSliderValue(compress))
                putFloat(KEY_SCALE, Settings.Scale.getSliderValue(scale))
            }
        }
    }

    fun getCurrentBundle(): Bundle {
        val bundle = Bundle()

        with(bundle) {
            putParcelable(KEY_FILE, file)
            putFloat(KEY_FPS, Settings.Fps.getSliderValue(fps))
            putFloat(KEY_COMPRESS, Settings.Compress.getSliderValue(compress))
            putFloat(KEY_SCALE, Settings.Scale.getSliderValue(scale))
        }

        return bundle
    }

}
