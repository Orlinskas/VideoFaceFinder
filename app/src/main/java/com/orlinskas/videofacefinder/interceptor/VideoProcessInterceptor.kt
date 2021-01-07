package com.orlinskas.videofacefinder.interceptor

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.google.android.gms.vision.face.FaceDetector
import com.orlinskas.videofacefinder.data.enums.Settings
import com.orlinskas.videofacefinder.data.model.FaceModel
import com.orlinskas.videofacefinder.data.model.Frame
import com.orlinskas.videofacefinder.data.model.UserFile
import com.orlinskas.videofacefinder.data.repository.FaceRepository
import com.orlinskas.videofacefinder.data.repository.FrameRepository
import com.orlinskas.videofacefinder.systems.*
import com.orlinskas.videofacefinder.systems.FileSystem.getAbsolutePath
import com.orlinskas.videofacefinder.systems.FileSystem.toNumber
import com.orlinskas.videofacefinder.tflite.TFLiteClassifier
import com.orlinskas.videofacefinder.util.io
import timber.log.Timber
import java.io.File


class VideoProcessInterceptor(
    private val context: Context,
    private val file: UserFile?,
    private val fps: Settings.Fps,
    private val compress: Settings.Compress,
    private val scale: Settings.Scale,
    private val faceDetector: FaceDetector,
    private val faceClassifier: TFLiteClassifier,
    private val frameRepository: FrameRepository,
    private val faceRepository: FaceRepository
) {

    private var frameParams: FaceDataSimpleClassifier.Companion.FrameParams? = null

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

    fun run(callback: (Boolean) -> Unit) {
        splitVideoFile(context.contentResolver) { isSplitVideoDone ->
            if (isSplitVideoDone) {
                processFrames { isProcessFramesCallback ->
                    if (isProcessFramesCallback) {
                        processFaces { isProcessFacesCallback ->
                            if (isProcessFacesCallback) {
                                searchNearest { isSearchNearestCallback ->
                                    callback.invoke(isSearchNearestCallback)
                                }
                            } else {
                                callback.invoke(false)
                            }
                        }
                    } else {
                        callback.invoke(false)
                    }
                }
            } else {
                callback.invoke(false)
            }
        }
    }

    private fun splitVideoFile(contentResolver: ContentResolver, callback: (Boolean) -> (Unit)) {
        io {
            Timber.d("Start split video")
            val operationStartTime = System.currentTimeMillis()

            val userFile = file ?: error("File is null")
            val filePath = userFile.getAbsolutePath(contentResolver) ?: error("Error convert to file from uri")

            FileSystem.deleteFolder(FRAME_IMAGES_FOLDER_PATH)
            FileSystem.createFolder(FRAME_IMAGES_FOLDER_PATH)

            val command = FFMPEGSystem.buildSplitCommand(filePath, FRAME_IMAGES_FOLDER_PATH, fps, compress)
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

    private fun processFrames(callback: (Boolean) -> (Unit)) {
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
                val startSecond = index * fps.float.toDouble()

                frames.add(
                    Frame(
                        id = index.toLong(),
                        absolutePath = absolutePath,
                        startSecond = startSecond,
                        faces = listOf()
                    )
                )
            }

            val params = FaceDataSimpleClassifier.collectFrameParams(frames[0], fps.float.toDouble())
            frameParams = params

            frameRepository.insertFrames(frames)

            Timber.d("Finish frames saving, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")
            callback.invoke(true)
        }
    }

    private fun processFaces(callback: (Boolean) -> (Unit)) {
        io {
            Timber.d("Start faces process")
            val operationStartTime = System.currentTimeMillis()

            FileSystem.deleteFolder(FACE_IMAGES_FOLDER_PATH)
            FileSystem.createFolder(FACE_IMAGES_FOLDER_PATH)

            val frames = frameRepository.getAllFrames()
            val faceModelsToSave = mutableListOf<FaceModel>()

            frames.forEach { frame ->
                val facesOnFrame = prepareFaceImages(frame)

                if (facesOnFrame.isNotEmpty()) {
                    val faceModels = createFaceModel(frame, facesOnFrame, faceClassifier)
                    frameRepository.updateFrame(frame.apply { faces = faceModels.map { it.id } })
                    faceModelsToSave.addAll(faceModels)
                }
            }

            frameRepository.removeAllFrames()
            frames.toMutableList().clear()

            Timber.d("Saving ${faceModelsToSave.size} faces.")
            faceRepository.removeAllFaces()
            faceRepository.insertFaces(faceModelsToSave)
            faceModelsToSave.clear()

            Timber.d("Finish faces process, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")
            callback.invoke(true)
        }
    }

    private fun prepareFaceImages(frame: Frame): List<Pair<Rect, Bitmap>> {
        val operationStartTime = System.currentTimeMillis()

        try {
            val bitmap = FileSystem.bitmapFrom(frame.absolutePath, scale.int)

            val faces = FaceDetectorSystem.findFaces(bitmap, faceDetector)
            val facesRect = mutableListOf<Rect>()

            val faceBitmaps = mutableListOf<Pair<Rect, Bitmap>>()
            val resizedFaceBitmaps = mutableListOf<Pair<Rect, Bitmap>>()

            if (faces == null || faces.isEmpty()) {
                Timber.d("Frame - ${frame.id} Empty. Time ${(System.currentTimeMillis() - operationStartTime)}ms.")
                return emptyList()
            }

            faces.forEach { _, face ->
                facesRect.add(FaceDetectorSystem.findFaceRect(face))
            }

            facesRect.forEach { faceRect ->
                val subBitmap = ImageSystem.getSubImage(bitmap, faceRect)
                faceBitmaps.add(Pair(faceRect, subBitmap))
            }

            faceBitmaps.forEach { faceBitmap ->
                val resizedBitmap = ImageSystem.resize(faceBitmap.second, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE)

                if (resizedBitmap != null) {
                    resizedFaceBitmaps.add(Pair(faceBitmap.first, resizedBitmap))
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

    private fun createFaceModel(frame: Frame, bitmaps: List<Pair<Rect, Bitmap>>, classifier: TFLiteClassifier): List<FaceModel> {
        val operationStartTime = System.currentTimeMillis()

        val faceModels = mutableListOf<FaceModel>()

        bitmaps.forEachIndexed { index, bitmap ->
            val base64 = ImageSystem.encodeBitmapToBase64(bitmap.second)
            val data = FaceRecognitionSystem.recognize(bitmap.second, classifier)

            if (base64.isNullOrEmpty() || data.isEmpty()) {
                Timber.e("Frame - ${frame.id}. Create face model error on frame id - ${frame.id}")
            } else {
                val faceModel = FaceModel(
                    id = 0,
                    name = "frame - ${frame.id}; face - $index",
                    description = "",
                    data = data,
                    faceRect = bitmap.first,
                    imageBase64 = base64,
                    frame = frame.id,
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

    private fun searchNearest(callback: (Boolean) -> (Unit)) {
        io {
            val operationStartTime = System.currentTimeMillis()

            val faceModels = faceRepository.getFaces()

            FileSystem.deleteFolder(FACE_IMAGES_FOLDER_PATH)
            FileSystem.createFolder(FACE_IMAGES_FOLDER_PATH)

            faceModels.forEach { current ->
                val bitmap = ImageSystem.decodeBitmapFromBase64(current.imageBase64)
                FileSystem.createFileFrom(bitmap!!, FACE_IMAGES_FOLDER_PATH + "/${current.id}.png")
            }

            val classifier = FaceDataSimpleClassifier(faceModels, frameParams)
            val result = classifier.run()

            Timber.d("Found persons - ${result.keys.size}, time ${(System.currentTimeMillis() - operationStartTime)}ms. ")

            result.forEach {
                Timber.d("Person - ${it.key}. \n Faces - ${it.value}")
            }

            // need save result

            callback.invoke(true)
        }
    }
}