package com.orlinskas.videofacefinder.systems

import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFprobe
import com.arthenica.mobileffmpeg.MediaInformation
import com.orlinskas.videofacefinder.data.enums.Settings
import timber.log.Timber

object FFMPEGSystem {

    fun buildSplitCommand(
            videoPath: String,
            storagePath: String,
            fps: Settings.Fps = Settings.Fps.DEFAULT,
            compress: Settings.Compress = Settings.Compress.OFF
    ): String {
        return "-i $videoPath -vf fps=${fps.float.toDouble()} -qscale:v ${compress.int} $storagePath/%d.jpg"
    }

    fun getMediaInfo(absolutePath: String?): MediaInformation? {
        if (absolutePath == null) {
            Timber.e("Path is null")
            return null
        }

        val info = FFprobe.getMediaInformation(absolutePath)
        Timber.d(info.allProperties.toString(4))

        return info
    }

    fun logExecution(code: Int) {
        if (code == Config.RETURN_CODE_SUCCESS) {
            Timber.d("Command execution completed successfully.");
        } else {
            Timber.d("Command execution failed with rc=$code.")
            Config.printLastCommandOutput(Log.DEBUG)
        }
    }
}