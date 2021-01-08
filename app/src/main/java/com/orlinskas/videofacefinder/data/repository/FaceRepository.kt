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

    fun insertFace(face: FaceModel) {
        faceDao.insertFace(face)
    }

    fun updateFace(face: FaceModel) {
        faceDao.updateFace(face)
    }

    fun getFaces(): List<FaceModel> {
        return faceDao.getFaces()
    }

    fun getFaces(ids: List<Long>): List<FaceModel> {
        return faceDao.getFaces(ids)
    }

    fun removeAllFaces() {
        faceDao.removeAllFaces()
    }

}
