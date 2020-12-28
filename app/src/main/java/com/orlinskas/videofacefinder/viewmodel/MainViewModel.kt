package com.orlinskas.videofacefinder.viewmodel

import android.content.Context
import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.orlinskas.videofacefinder.core.BaseViewModel
import com.orlinskas.videofacefinder.data.enums.FileSystemState
import com.orlinskas.videofacefinder.ui.viewstate.FileViewState
import com.orlinskas.videofacefinder.util.io
import com.orlinskas.videofacefinder.util.FileSystem.toFileModel
import dagger.hilt.android.qualifiers.ApplicationContext

class MainViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context
) : BaseViewModel() {

    val state = FileViewState()

    var onFileManagerRequest: (() -> (Unit))? = null
    var onFileReceived: (() -> (Unit))? = null

    init {
        addSaveStateHandler(state)
    }

    fun processFile(data: Uri): LiveData<FileSystemState> = MutableLiveData<FileSystemState>().also { liveData ->
        io {
            try {
                val userFile = data.toFileModel(context.contentResolver)

                if (userFile == null) {
                    liveData.postValue(FileSystemState.EXCEPTION)
                    return@io
                }

                state.file = userFile
                liveData.postValue(FileSystemState.OK)
            } catch (e: Exception) {
                liveData.postValue(FileSystemState.EXCEPTION)
            }
        }
    }

}