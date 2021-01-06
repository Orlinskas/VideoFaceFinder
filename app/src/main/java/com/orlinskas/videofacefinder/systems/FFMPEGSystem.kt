package com.orlinskas.videofacefinder.systems

import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFprobe
import com.arthenica.mobileffmpeg.MediaInformation
import timber.log.Timber

object FFMPEGSystem {

    enum class FramesPerSec(val double: Double) {
        MAX(2.0),
        DEFAULT(1.0),
        ALMOST_MIN(0.5),
        MIN(0.1)
    }

    enum class Scale(val int: Int) {
        MAX(31),
        MEDIUM(10),
        DEFAULT(3),
        OFF(1)
    }

    //-qscale:v 31
    fun buildSplitCommand(videoPath: String, storagePath: String, fps: FramesPerSec = FramesPerSec.DEFAULT, scale: Scale = Scale.OFF): String {
        return "-i $videoPath -vf fps=${fps.double} -qscale:v ${scale.int} $storagePath/%d.jpg"
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