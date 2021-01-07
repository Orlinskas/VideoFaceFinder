package com.orlinskas.videofacefinder.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.vision.face.FaceDetector
import com.orlinskas.videofacefinder.data.enums.Settings
import com.orlinskas.videofacefinder.data.model.UserFile
import com.orlinskas.videofacefinder.data.repository.FaceRepository
import com.orlinskas.videofacefinder.data.repository.FrameRepository
import com.orlinskas.videofacefinder.interceptor.VideoProcessInterceptor
import com.orlinskas.videofacefinder.tflite.TFLiteClassifier
import com.orlinskas.videofacefinder.ui.viewstate.KEY_COMPRESS
import com.orlinskas.videofacefinder.ui.viewstate.KEY_FILE
import com.orlinskas.videofacefinder.ui.viewstate.KEY_FPS
import com.orlinskas.videofacefinder.ui.viewstate.KEY_SCALE
import com.orlinskas.videofacefinder.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@AndroidEntryPoint
class VideoProcessService: LifecycleService() {

    @Inject
    lateinit var videoProcessLiveData: VideoProcessLiveData
    @Inject
    lateinit var frameRepository: FrameRepository
    @Inject
    lateinit var faceRepository: FaceRepository

    private var isForegroundStarted: Boolean = false

    private lateinit var faceDetector: FaceDetector
    private lateinit var faceClassifier: TFLiteClassifier

    private lateinit var videoProcessInterceptor: VideoProcessInterceptor
    private lateinit var file: UserFile
    private lateinit var fps: Settings.Fps
    private lateinit var compress: Settings.Compress
    private lateinit var scale: Settings.Scale

    // MobileFaceNet constants
    private val TF_OD_API_INPUT_SIZE = 112
    private val TF_OD_API_IS_QUANTIZED = false
    private val TF_OD_API_MODEL_FILE = "mobile_face_net.tflite"
    private val TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt"

    override fun onCreate() {
        super.onCreate()

        faceDetector = FaceDetector.Builder(this).apply {
            setTrackingEnabled(false)
            setMode(FaceDetector.FAST_MODE)
        }.build()

        faceClassifier = TFLiteClassifier.create(
                assets,
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_IS_QUANTIZED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.extras?.let {
            file = it.getParcelable(KEY_FILE) ?: throw IllegalArgumentException()
            fps = Settings.Fps.fromValue(it.getFloat(KEY_FPS))
            compress = Settings.Compress.fromValue(it.getFloat(KEY_COMPRESS))
            scale = Settings.Scale.fromValue(it.getFloat(KEY_SCALE))

            Timber.d("Try start service with ${file.path}, fps ${fps.float}, compress ${compress.int}, scale ${scale.int}")
        }

        videoProcessInterceptor = VideoProcessInterceptor(
            context = this,
            file = file,
            fps = fps,
            compress = compress,
            scale = scale,
            faceDetector = faceDetector,
            faceClassifier = faceClassifier,
            frameRepository = frameRepository,
            faceRepository = faceRepository
        )

        if (intent != null) {
            startLoading()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startLoading() {
        isForegroundStarted = true
        val notification = NotificationHelper.createDownloadNotification(this)
        val notificationManager = NotificationManagerCompat.from(this)
        startForeground(NOTIFICATION_DOWNLOAD_ID, notification)

        videoProcessLiveData.postValue(State.LOADING)

        videoProcessInterceptor.run {
            isForegroundStarted = false

            videoProcessLiveData.postValue(
                    if (it) {
                        State.SUCCESS
                    } else {
                        State.FAIL
                    }
            )

            stopForeground(true)
            try {
                faceDetector.release()
                faceClassifier.close()
            } catch (e: Exception) {
                Timber.e(e)
            }

            if (it) {
                notificationManager.notify(NOTIFICATION_RESULT_ID, NotificationHelper.createDownloadDoneNotification(this@VideoProcessService))
            } else {
                notificationManager.notify(NOTIFICATION_RESULT_ID, NotificationHelper.createDownloadFailNotification(this@VideoProcessService))
            }

            stopSelf()
        }
    }

    override fun onDestroy() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_DOWNLOAD_ID)
        try {
            faceDetector.release()
            faceClassifier.close()
        } catch (e: Exception) {
            Timber.e(e)
        }
        super.onDestroy()
    }

    companion object {

        private const val NOTIFICATION_DOWNLOAD_ID = 201
        private const val NOTIFICATION_RESULT_ID = 202

        fun start(context: Context?, bundle: Bundle) {
            context?.let {
                val intent = Intent(context, VideoProcessService::class.java)
                intent.putExtras(bundle)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }

    enum class State {
        LOADING,
        FAIL,
        SUCCESS
    }
}

@Singleton
class VideoProcessLiveData @Inject constructor() : MutableLiveData<VideoProcessService.State>()
