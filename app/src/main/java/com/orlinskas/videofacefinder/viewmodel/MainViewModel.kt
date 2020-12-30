package com.orlinskas.videofacefinder.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.SparseArray
import androidx.core.graphics.toRect
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.orlinskas.videofacefinder.core.BaseViewModel
import com.orlinskas.videofacefinder.data.enums.FileSystemState
import com.orlinskas.videofacefinder.data.model.Frame
import com.orlinskas.videofacefinder.data.repository.FrameRepository
import com.orlinskas.videofacefinder.ui.viewstate.FileViewState
import com.orlinskas.videofacefinder.util.FileSystem.getAbsolutePath
import com.orlinskas.videofacefinder.util.FileSystem.toFileModel
import com.orlinskas.videofacefinder.util.FileSystem.toNumber
import com.orlinskas.videofacefinder.util.io
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

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

            deleteFolder(FRAME_IMAGES_FOLDER_PATH)
            createFolder(FRAME_IMAGES_FOLDER_PATH)

            val command = buildSplitCommand(filePath, FRAME_IMAGES_FOLDER_PATH, state.fps)
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

    private fun buildSplitCommand(videoPath: String, storagePath: String, fps: Int): String {
        return "-i $videoPath -vf fps=$fps $storagePath/%d.jpg"
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

            deleteFolder(FACE_IMAGES_FOLDER_PATH)
            createFolder(FACE_IMAGES_FOLDER_PATH)

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

    fun saveFacesFromFrame(frame: Frame, path: String, frameID: Int): Boolean {
        Timber.d("Start find faces on frame")
        val operationStartTime = System.currentTimeMillis()

        try {
            val bitmap = bitmapFrom(frame.absolutePath)

            val faces = findFaces(bitmap)
            val facesRect = mutableListOf<Rect>()
            val facesBitmap = mutableListOf<Bitmap>()

            if (faces == null || faces.isEmpty()) {
                return false
            }

            faces.forEach { key, face ->
                facesRect.add(findFaceRect(face))
            }

            facesRect.forEach { faceRect ->
                facesBitmap.add(getSubImage(bitmap, faceRect))
            }

            facesBitmap.forEachIndexed { index, faceBitmap ->
                createFileFrom(faceBitmap, "$path/$frameID($index).png")
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

    fun bitmapFrom(path: String): Bitmap {
        Timber.d("Start get bitmap from frame file")
        val operationStartTime = System.currentTimeMillis()

        val bitmap = BitmapFactory.decodeFile(path)

        Timber.d("Finish get bitmap from frame file, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")

        return bitmap
    }

    fun findFaces(bitmap: Bitmap): SparseArray<Face>? {
        Timber.d("Start detect faces on bitmap")
        val operationStartTime = System.currentTimeMillis()

        val detectorFrame = com.google.android.gms.vision.Frame.Builder().setBitmap(bitmap).build()
        val faces = faceDetector.detect(detectorFrame)

        Timber.d("Finish detect faces on bitmap, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")

        return faces
    }

    fun findFaceRect(face: Face): Rect {
        val x1 = face.position.x
        val y1 = face.position.y
        val x2 = x1 + face.width
        val y2 = y1 + face.height

        return RectF(x1, y1, x2, y2).toRect()
    }

    fun getSubImage(bitmap: Bitmap, copyRect: Rect): Bitmap {
        Timber.d("Start get subImage")
        val operationStartTime = System.currentTimeMillis()

        val subImage = Bitmap.createBitmap(copyRect.width(), copyRect.height(), Bitmap.Config.RGB_565)
        val canvas = Canvas(subImage)
        canvas.drawBitmap(bitmap, copyRect, Rect(0, 0, copyRect.width(), copyRect.height()), null)

        Timber.d("Finish get subImage, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")

        return subImage
    }

    fun createFileFrom(bitmap: Bitmap, path: String): File {
        Timber.d("Start create file from bitmap")
        val operationStartTime = System.currentTimeMillis()

        val file = File(path)
        val outputStream: OutputStream = BufferedOutputStream(FileOutputStream(file))
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()

        Timber.d("Finish create file from bitmap, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")

        return file
    }

    fun createFolder(path: String) {
        val folder = File(path)

        if (!folder.exists()) {
            if (!folder.mkdir()) {
                Timber.e("Create folder $path error")
            }
        } else {
            Timber.d("Folder $path already exists")
        }
    }

    fun deleteFolder(path: String) {
        val folder = File(path)

        if (folder.exists()) {
            if (folder.deleteRecursively()) {
                Timber.e("Delete $path error")
            }
        }
    }
}