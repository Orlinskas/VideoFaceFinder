package com.orlinskas.videofacefinder.data.repository

import com.orlinskas.videofacefinder.data.AppDatabase
import com.orlinskas.videofacefinder.data.model.Frame
import javax.inject.Inject

class FrameRepository @Inject constructor(
        appDatabase: AppDatabase
) {

    private val frameDao = appDatabase.frameDao()

    fun insertFrames(frames: List<Frame>) {
        frameDao.removeAllFrames()
        frameDao.insertFrames(frames)
    }

    fun getAllFrames(): List<Frame> {
        return frameDao.getFrames()
    }

    fun removeAllFrames() {
        return frameDao.removeAllFrames()
    }

}
