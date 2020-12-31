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
import com.orlinskas.videofacefinder.data.model.FaceModel
import com.orlinskas.videofacefinder.data.model.Frame
import com.orlinskas.videofacefinder.data.repository.FaceRepository
import com.orlinskas.videofacefinder.data.repository.FrameRepository
import com.orlinskas.videofacefinder.systems.*
import com.orlinskas.videofacefinder.tflite.TFLiteObjectDetectionAPIModel
import com.orlinskas.videofacefinder.ui.viewstate.FileViewState
import com.orlinskas.videofacefinder.util.*
import com.orlinskas.videofacefinder.systems.FileSystem.getAbsolutePath
import com.orlinskas.videofacefinder.systems.FileSystem.toFileModel
import com.orlinskas.videofacefinder.systems.FileSystem.toNumber
import com.orlinskas.videofacefinder.tflite.SimilarityClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.*

class MainViewModel @ViewModelInject constructor(
    @ApplicationContext
    private val context: Context,
    private val frameRepository: FrameRepository,
    private val faceRepository: FaceRepository
) : BaseViewModel() {

    val state = FileViewState()

    var onFileManagerRequest: (() -> (Unit))? = null
    var onFileReceived: (() -> (Unit))? = null

    var faceDetector: FaceDetector
    var faceClassifier: SimilarityClassifier

    // FileSystem constants
    private val FRAME_IMAGES_FOLDER_NAME = "frames"
    private val FACE_IMAGES_FOLDER_NAME = "faces"
    private val INTERNAL_STORAGE_PATH = context.filesDir.absolutePath
    private val FRAME_IMAGES_FOLDER_PATH = INTERNAL_STORAGE_PATH + "/" + FRAME_IMAGES_FOLDER_NAME
    private val FACE_IMAGES_FOLDER_PATH = INTERNAL_STORAGE_PATH + "/" + FACE_IMAGES_FOLDER_NAME

    // MobileFaceNet constants
    private val TF_OD_API_INPUT_SIZE = 112
    private val TF_OD_API_IS_QUANTIZED = false
    private val TF_OD_API_MODEL_FILE = "mobile_face_net.tflite"
    private val TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt"

    init {
        addSaveStateHandler(state)

        faceDetector = FaceDetector.Builder(context).apply {
            setTrackingEnabled(false)
        }.build()

        faceClassifier = TFLiteObjectDetectionAPIModel.create(
                context.assets,
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_IS_QUANTIZED
        )
    }

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

    fun splitVideoFile(contentResolver: ContentResolver, callback: (Boolean) -> (Unit)) {
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
                callback.invoke(true)
            } else {
                Timber.e("Split video FAILED, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")
                callback.invoke(false)
            }
        }
    }

    fun processFrames(callback: (Boolean) -> (Unit)) {
        io {
            Timber.d("Start saving frames to database")

            val operationStartTime = System.currentTimeMillis()
            val frames = mutableListOf<Frame>()
            val directory = File(FRAME_IMAGES_FOLDER_PATH)
            val files: Array<File>? = directory.listFiles()
            val sortedFiles = mutableListOf<File>()

            if (files == null) {
                Timber.e("Not found frames in ${directory.absolutePath}")
                callback.invoke(false)
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

            Timber.d("Finish frames saving, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")
            callback.invoke(true)
        }
    }

    fun processFaces(callback: (Boolean) -> (Unit)) {
        io {
            Timber.d("Start faces process")
            val operationStartTime = System.currentTimeMillis()

            FileSystem.deleteFolder(FACE_IMAGES_FOLDER_PATH)
            FileSystem.createFolder(FACE_IMAGES_FOLDER_PATH)

            val frames = frameRepository.getAllFrames()
            val faceModelsToSave = mutableListOf<FaceModel>()

            frames.forEach{ frame ->
                val facesOnFrame = prepareFaceImages(frame)

                if (facesOnFrame.isNotEmpty()) {
                    val faceModels = createFaceModel(frame, facesOnFrame, faceClassifier)
                    faceModelsToSave.addAll(faceModels)
                }
            }

            frameRepository.removeAllFrames()
            frames.toMutableList().clear()

            Timber.d("Saving ${faceModelsToSave.size} faces.")
            faceRepository.insertFaces(faceModelsToSave)
            faceModelsToSave.clear()

            Timber.d("Finish faces process, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")
            callback.invoke(true)
        }
    }

    private fun prepareFaceImages(frame: Frame): List<Bitmap> {
        val operationStartTime = System.currentTimeMillis()

        try {
            val bitmap = FileSystem.bitmapFrom(frame.absolutePath)

            val faces = FaceDetectorSystem.findFaces(bitmap, faceDetector)
            val facesRect = mutableListOf<Rect>()
            val faceBitmaps = mutableListOf<Bitmap>()
            val resizedFaceBitmaps = mutableListOf<Bitmap>()

            if (faces == null || faces.isEmpty()) {
                Timber.d("Frame - ${frame.id} Empty. Time ${(System.currentTimeMillis() - operationStartTime)}ms.")
                return emptyList()
            }

            faces.forEach { _, face ->
                facesRect.add(FaceDetectorSystem.findFaceRect(face))
            }

            facesRect.forEach { faceRect ->
                faceBitmaps.add(ImageSystem.getSubImage(bitmap, faceRect))
            }

            faceBitmaps.forEach { faceBitmap ->
                val resizedBitmap = ImageSystem.resize(faceBitmap, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE)

                if (resizedBitmap != null) {
                    resizedFaceBitmaps.add(resizedBitmap)
                } else {
                    Timber.e("Error bitmap resize")
                }
            }

            Timber.d("Frame - ${frame.id}. Found faces - ${resizedFaceBitmaps.size}, time ${(System.currentTimeMillis() - operationStartTime)}ms.")
            return resizedFaceBitmaps
        } catch (e: Exception) {
            Timber.e("Frame - ${frame.id}. Found faces ${(System.currentTimeMillis() - operationStartTime)}ms. Error - \n $e")
            return emptyList()
        }
    }

    private fun createFaceModel(frame: Frame, bitmaps: List<Bitmap>, classifier: SimilarityClassifier): List<FaceModel> {
        val operationStartTime = System.currentTimeMillis()

        val faceModels = mutableListOf<FaceModel>()

        bitmaps.forEachIndexed { index, bitmap ->
            val base64 = ImageSystem.encodeBitmapToBase64(bitmap)
            val data = FaceRecognitionSystem.recognize(bitmap, classifier)

            if (base64.isNullOrEmpty() || data.isEmpty()) {
                Timber.e("Frame - ${frame.id}. Create face model error on frame id - ${frame.id}")
            } else {
                val faceModel = FaceModel(
                        id = 0,
                        name = "frame - ${frame.id}; face - $index",
                        description = "",
                        data = data,
                        imageBase64 = base64,
                        startSecond = frame.startSecond,
                        videoName = frame.videoName,
                        videoDescription = frame.videoDescription
                )

                faceModels.add(faceModel)
            }
        }

        Timber.d("Frame - ${frame.id}. Created models ${faceModels.size}, time ${(System.currentTimeMillis() - operationStartTime)}ms. ")
        return faceModels
    }
}