package com.orlinskas.videofacefinder.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.google.android.gms.vision.face.FaceDetector
import com.orlinskas.videofacefinder.core.BaseViewModel
import com.orlinskas.videofacefinder.data.enums.FileSystemState
import com.orlinskas.videofacefinder.data.model.Frame
import com.orlinskas.videofacefinder.data.repository.FrameRepository
import com.orlinskas.videofacefinder.systems.FFMPEGSystem
import com.orlinskas.videofacefinder.systems.FaceDetectorSystem
import com.orlinskas.videofacefinder.systems.FileSystem
import com.orlinskas.videofacefinder.tflite.TFLiteObjectDetectionAPIModel
import com.orlinskas.videofacefinder.ui.viewstate.FileViewState
import com.orlinskas.videofacefinder.util.*
import com.orlinskas.videofacefinder.systems.FileSystem.getAbsolutePath
import com.orlinskas.videofacefinder.systems.FileSystem.toFileModel
import com.orlinskas.videofacefinder.systems.FileSystem.toNumber
import com.orlinskas.videofacefinder.systems.ImageSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.*

class MainViewModel @ViewModelInject constructor(
    @ApplicationContext
    private val context: Context,
    private val frameRepository: FrameRepository
) : BaseViewModel() {

    val state = FileViewState()

    var onFileManagerRequest: (() -> (Unit))? = null
    var onFileReceived: (() -> (Unit))? = null

    var faceDetector: FaceDetector

    private val FRAME_IMAGES_FOLDER_NAME = "frames"
    private val FACE_IMAGES_FOLDER_NAME = "faces"

    private val INTERNAL_STORAGE_PATH = context.filesDir.absolutePath
    private val FRAME_IMAGES_FOLDER_PATH = INTERNAL_STORAGE_PATH + "/" + FRAME_IMAGES_FOLDER_NAME
    private val FACE_IMAGES_FOLDER_PATH = INTERNAL_STORAGE_PATH + "/" + FACE_IMAGES_FOLDER_NAME

    // MobileFaceNet
    private val TF_OD_API_INPUT_SIZE = 112
    private val TF_OD_API_IS_QUANTIZED = false
    private val TF_OD_API_MODEL_FILE = "mobile_face_net.tflite"
    private val TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt"

    init {
        addSaveStateHandler(state)

        faceDetector = FaceDetector.Builder(context).apply {
            setTrackingEnabled(false)
        }.build()
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

    fun splitVideoFile(contentResolver: ContentResolver): LiveData<Boolean> = MutableLiveData<Boolean>().also { liveData ->
        io {
            Timber.d("Start split video")
            val operationStartTime = System.currentTimeMillis()

            val userFile = state.file ?: error("File is null")
            val filePath = userFile.getAbsolutePath(contentResolver) ?: error("Error convert to file from uri")

            FileSystem.deleteFolder(FRAME_IMAGES_FOLDER_PATH)
            FileSystem.createFolder(FRAME_IMAGES_FOLDER_PATH)

            val command = FFMPEGSystem.buildSplitCommand(filePath, FRAME_IMAGES_FOLDER_PATH, state.fps)
            val rc = FFmpeg.execute(command)

            if (rc == Config.RETURN_CODE_SUCCESS) {
                Timber.d("Finish split video, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")
                liveData.postValue(true)
            } else {
                Timber.e("Split video FAILED, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")
                liveData.postValue(false)
            }
        }
    }

    fun processFrames(): LiveData<Boolean> = MutableLiveData<Boolean>().also { liveData ->
        io {
            Timber.d("Start saving frames to database")

            val operationStartTime = System.currentTimeMillis()
            val frames = mutableListOf<Frame>()
            val directory = File(FRAME_IMAGES_FOLDER_PATH)
            val files: Array<File>? = directory.listFiles()
            val sortedFiles = mutableListOf<File>()

            if (files == null) {
                Timber.e("Not found frames in ${directory.absolutePath}")
                liveData.postValue(false)
                return@io
            }

            Timber.d("Found - ${files.size} frames.")

            sortedFiles.addAll(files)
            sortedFiles.sortBy { it.toNumber() }

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

            Timber.d("Finish frames saving, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")
        }
    }

    fun processFaces(): LiveData<Boolean> = MutableLiveData<Boolean>().also { liveData ->
        io {
            Timber.d("Start faces process")
            val operationStartTime = System.currentTimeMillis()

            FileSystem.deleteFolder(FACE_IMAGES_FOLDER_PATH)
            FileSystem.createFolder(FACE_IMAGES_FOLDER_PATH)

            val frames = frameRepository.getAllFrames()

            frames.forEachIndexed { index, frame ->
                saveFacesFromFrame(frame, FACE_IMAGES_FOLDER_PATH, index)
            }

            frameRepository.removeAllFrames()
            frames.toMutableList().clear()

            liveData.postValue(true)

            Timber.d("Finish faces process, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")
        }
    }

    private fun saveFacesFromFrame(frame: Frame, path: String, frameID: Int): Boolean {
        Timber.d("Start find faces on frame")
        val operationStartTime = System.currentTimeMillis()

        try {
            val bitmap = FileSystem.bitmapFrom(frame.absolutePath)

            val faces = FaceDetectorSystem.findFaces(bitmap, faceDetector)
            val facesRect = mutableListOf<Rect>()
            val facesBitmap = mutableListOf<Bitmap>()

            if (faces == null || faces.isEmpty()) {
                return false
            }

            faces.forEach { key, face ->
                facesRect.add(FaceDetectorSystem.findFaceRect(face))
            }

            facesRect.forEach { faceRect ->
                facesBitmap.add(ImageSystem.getSubImage(bitmap, faceRect))
            }

            facesBitmap.forEachIndexed { index, faceBitmap ->
                FileSystem.createFileFrom(faceBitmap, "$path/$frameID($index).png")
            }

            Timber.d("Finish find faces, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")
            Timber.d("Find ${faces.size()} faces \n")
            Timber.d("\n")

            return true
        } catch (e: Exception) {
            Timber.e("Finish find faces, with error \n $e")
            return false
        }
    }

    fun recognizeFace() {
        io {
            val detector = TFLiteObjectDetectionAPIModel.create(
                    context.assets,
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_INPUT_SIZE,
                    TF_OD_API_IS_QUANTIZED)

            val bitmap = FileSystem.bitmapFrom(FACE_IMAGES_FOLDER_PATH + "/0(0).png")
            val resizeBitmap = ImageSystem.resize(bitmap, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE)
            val data = detector.recognizeImage(resizeBitmap, true)

            val data2 = detector.recognize(resizeBitmap)

            data.forEach {
                Timber.d(it.toString())
            }
        }
    }
}