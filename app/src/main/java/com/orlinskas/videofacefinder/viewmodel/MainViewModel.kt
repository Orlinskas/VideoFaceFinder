package com.orlinskas.videofacefinder.viewmodel

import android.content.Context
import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.orlinskas.videofacefinder.core.BaseViewModel
import com.orlinskas.videofacefinder.data.enums.FileSystemState
import com.orlinskas.videofacefinder.data.model.FaceModel
import com.orlinskas.videofacefinder.data.model.Person
import com.orlinskas.videofacefinder.data.repository.FaceRepository
import com.orlinskas.videofacefinder.data.repository.FrameRepository
import com.orlinskas.videofacefinder.data.repository.PersonRepository
import com.orlinskas.videofacefinder.service.VideoProcessLiveData
import com.orlinskas.videofacefinder.service.VideoProcessService
import com.orlinskas.videofacefinder.systems.FileSystem.toFileModel
import com.orlinskas.videofacefinder.ui.viewstate.FileViewState
import com.orlinskas.videofacefinder.util.io
import dagger.hilt.android.qualifiers.ApplicationContext


class MainViewModel @ViewModelInject constructor(
    @ApplicationContext
    private val context: Context,
    private val frameRepository: FrameRepository,
    private val faceRepository: FaceRepository,
    private val personRepository: PersonRepository,
    var videoProcessLiveData: VideoProcessLiveData
) : BaseViewModel() {

    val state = FileViewState()

    var onFileManagerRequest: (() -> (Unit))? = null
    var onFileReceived: (() -> (Unit))? = null

    fun fileLiveData(data: Uri): LiveData<FileSystemState> = MutableLiveData<FileSystemState>().also { liveData ->
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

    fun runVideoProcessing() {
        VideoProcessService.start(context, state.getCurrentBundle())
    }

    fun getLastPersons(): LiveData<List<Pair<Person, List<FaceModel>>>>
            = MutableLiveData<List<Pair<Person, List<FaceModel>>>>().also { liveData ->

        io {
            val persons = personRepository.getAllPersons()

            val personPairs = mutableListOf<Pair<Person, List<FaceModel>>>()

            persons.forEach {
                val faces = faceRepository.getFaces(it.faces)

                personPairs.add(Pair(it, faces))
            }

            liveData.postValue(personPairs)
        }
    }
}