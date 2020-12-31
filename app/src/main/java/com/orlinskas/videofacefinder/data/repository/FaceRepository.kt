package com.orlinskas.videofacefinder.data.repository

import com.orlinskas.videofacefinder.data.AppDatabase
import com.orlinskas.videofacefinder.data.model.FaceModel
import javax.inject.Inject

class FaceRepository @Inject constructor(
        appDatabase: AppDatabase
) {

    private val faceDao = appDatabase.faceDao()

    fun insertFaces(faces: List<FaceModel>) {
        faceDao.insertFaces(faces)
    }

    fun updateFace(face: FaceModel) {
        faceDao.updateFace(face)
    }

    fun getFaces(): List<FaceModel> {
        return faceDao.getFaces()
    }

    fun removeAllFaces() {
        faceDao.removeAllFaces()
    }

}
