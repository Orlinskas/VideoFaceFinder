package com.orlinskas.videofacefinder.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.orlinskas.videofacefinder.core.BaseViewModel
import com.orlinskas.videofacefinder.data.enums.FileSystemState
import com.orlinskas.videofacefinder.data.model.Frame
import com.orlinskas.videofacefinder.data.repository.FrameRepository
import com.orlinskas.videofacefinder.ui.viewstate.FileViewState
import com.orlinskas.videofacefinder.util.FileSystem.getAbsolutePath
import com.orlinskas.videofacefinder.util.FileSystem.toFileModel
import com.orlinskas.videofacefinder.util.io
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File

class MainViewModel @ViewModelInject constructor(
    @ApplicationContext
    private val context: Context,
    private val frameRepository: FrameRepository
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

    fun splitVideoFile(contentResolver: ContentResolver, storagePath: String): LiveData<Boolean> = MutableLiveData<Boolean>().also { liveData ->
        io {
            Timber.d("Start split video")
            val operationStartTime = System.currentTimeMillis()

            val userFile = state.file ?: error("File is null")
            val filePath = userFile.getAbsolutePath(contentResolver) ?: error("Error convert to file from uri")

            val command = buildSplitCommand(filePath, storagePath, state.fps)
            val rc = FFmpeg.execute(command)

            if (rc == Config.RETURN_CODE_SUCCESS) {
                Timber.d("Finish split video, time - ${(System.currentTimeMillis() - operationStartTime) / 1000}s.")
                liveData.postValue(true)
            } else {
                Timber.e("Split video FAILED, time - ${(System.currentTimeMillis() - operationStartTime) / 1000}s.")
                liveData.postValue(false)
            }
        }
    }

    private fun buildSplitCommand(videoPath: String, storagePath: String, fps: Int): String {
        return "-i $videoPath -vf fps=$fps $storagePath/%d.jpg"
    }

    fun processFrames(storagePath: String): LiveData<Boolean> = MutableLiveData<Boolean>().also { liveData ->
        io {
            Timber.d("Start saving frames to database")

            val operationStartTime = System.currentTimeMillis()
            val frames = mutableListOf<Frame>()
            val directory = File(storagePath)
            val files: Array<File>? = directory.listFiles()
            val sortedFiles = mutableListOf<File>()

            if (files == null) {
                Timber.e("Not found frames in $storagePath")
                liveData.postValue(false)
                return@io
            }

            Timber.d("Found - ${files.size} frames.")

            sortedFiles.addAll(files)
            sortedFiles.sortBy { it.name }

            sortedFiles.forEachIndexed { index, file ->
                val absolutePath = file.absolutePath
                val startSecond = index * state.fps

                frames.add(
                        Frame(
                                id = index.toLong(),
                                absolutePath = absolutePath,
                                startSecond = startSecond
                        )
                )
            }

            frameRepository.insertFrames(frames)
            liveData.postValue(true)

            Timber.d("Finish frames saving, time - ${(System.currentTimeMillis() - operationStartTime) / 1000}s.")
        }
    }

}