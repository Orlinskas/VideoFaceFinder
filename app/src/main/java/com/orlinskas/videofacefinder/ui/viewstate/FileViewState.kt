package com.orlinskas.videofacefinder.ui.viewstate

import android.os.Bundle
import com.orlinskas.videofacefinder.core.ViewState
import com.orlinskas.videofacefinder.data.model.UserFile
import com.orlinskas.videofacefinder.systems.FaceDataSimpleClassifier

private const val KEY_FILE = "FILE"

class FileViewState : ViewState {

    var file: UserFile? = null
    var fps = 1
    var frameParams: FaceDataSimpleClassifier.Companion.FrameParams? = null

    override fun onRestoreState(bundle: Bundle?) {
        super.onRestoreState(bundle)
        bundle?.let {
            file = it.getParcelable(KEY_FILE)
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        super.onSaveState(bundle)
        bundle?.let {
            with(bundle) {
                putParcelable(KEY_FILE, file)
            }
        }
    }

}
