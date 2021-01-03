package com.orlinskas.videofacefinder.systems

import android.graphics.Rect
import com.orlinskas.videofacefinder.data.model.FaceModel
import com.orlinskas.videofacefinder.data.model.Frame
import timber.log.Timber

class FaceDataSimpleClassifier (
        data: List<FaceModel>,
        frameParams: FrameParams?,
        private val isCalculateFacePosition: Boolean = false,
        private val isCalculateFaceMovement: Boolean = true
) {
    
    private var rootData: MutableMap<Long, MutableList<Face>> = mutableMapOf()
    private val faceToPersonMap: MutableMap<Face, Long> = mutableMapOf()
    private val notFoundFaces: MutableList<Face> = mutableListOf()
    private var personIndex = 0L
    private var iterationCount = 0L

    init {
        val frames = data.map { it.frame }.toSortedSet()
        rootData = frames.associateBy({ it }, { mutableListOf<Face>() }).toMutableMap()

        rootData.forEach { (key, _) ->
            val faces = data.filter { it.frame == key }.map {
                Face(
                    id = it.id,
                    data = it.data,
                    rect = it.faceRect,
                    personId = null
                )
            }

            rootData[key] = faces.toMutableList()
        }
    }

    fun run(): Map<Long, MutableList<Long>> {

        val frameIds = rootData.keys.sortedBy { it }
        frameIds.forEachIndexed { index, currentFrameId ->

            if (index == frameIds.lastIndex) {
                return@forEachIndexed
            }

            val currentFrameFaces = rootData[currentFrameId]
            currentFrameFaces?.forEach { currentFrameFace ->

                if (currentFrameFace.personId == null) {

                    val nextFrames = frameIds.subList(index + 1, frameIds.lastIndex)
                    nextFrames.forEachIndexed { frameIndex, nextFrameId ->

                        val nextFrameFaces = rootData[nextFrameId]
                        nextFrameFaces?.forEachIndexed { faceIndex, nextFrameFace ->

                            if (nextFrameFace.personId == null) {
                                iterationCount++

                                if (isIdentical(currentFrameFace, nextFrameFace)) {
                                    catchIdenticalResult(currentFrameFace, nextFrameFace)
                                } else {
                                    if (frameIndex == nextFrames.lastIndex && faceIndex == nextFrameFaces.lastIndex) {
                                        notFoundFaces.add(currentFrameFace)
                                    }
//                                  if (isCalculateFacePosition) {
//                                      if (isIdenticalForFacePosition(currentFace, nextFace)) {
//                                      Timber.d("Face ${currentFace.id} identical to face ${nextFace.id} for position")
//                                      catchIdenticalResult(currentFace, nextFace)
//                                      }
//                                  }
                                }
                            }
                        }
                    }
                }
            }
        }

        Timber.d("Take ${notFoundFaces.size} not found faces.")

        notFoundFaces.forEach { face ->
            var lastNearestValue = Float.MAX_VALUE
            var lastNearestPerson: Long? = null

            faceToPersonMap.forEach { entry ->
                val value = FaceRecognitionSystem.compare(face.data, entry.key.data)

                if (value < lastNearestValue) {
                    lastNearestValue = value
                    lastNearestPerson = entry.value
                }
            }

            lastNearestPerson?.let {
                faceToPersonMap[face.apply { isPotentialProblem = true }] = it
                Timber.d("Found nearest Person $lastNearestPerson to Face ${face.id}.")
            }
        }

        Timber.d("Total $iterationCount iterations.")

        return collectResult()
    }

    private fun isIdentical(first: Face, second: Face): Boolean {
        val compareValue = FaceRecognitionSystem.compare(first.data, second.data)
        Timber.i("Face ${first.id} came close to face ${second.id} by - $compareValue")

        return compareValue < MAX_IDENTICAL_FACE_VALUE
    }

    private fun catchIdenticalResult(currentFace: Face, nextFace: Face) {
        val currentFacePerson = faceToPersonMap[currentFace]

        if (currentFacePerson == null) {
            currentFace.personId = personIndex
            nextFace.personId = personIndex
            faceToPersonMap[currentFace] = personIndex
            faceToPersonMap[nextFace] = personIndex
            personIndex++
            Timber.d("Create person $personIndex")
        } else {
            nextFace.personId = currentFacePerson
            faceToPersonMap[nextFace] = currentFacePerson
        }

        Timber.d("Face ${currentFace.id} identical to face ${nextFace.id}. Person ${nextFace.personId}")
    }

    private fun isIdenticalForFacePosition(first: Face, second: Face): Boolean {
        val permissibleVariation = first.rect.height() * PERMISSIBLE_POSITION_VARIABLE_MULTIPLIER
        Timber.i("Start identical for position with permissibleVariation - $permissibleVariation")

        val firstLeft = first.rect.left
        val secondLeft = second.rect.left

        val isIdentical = if (firstLeft > secondLeft) {
            firstLeft - secondLeft < permissibleVariation
        } else {
            secondLeft - firstLeft < permissibleVariation
        }

        Timber.i("Face ${first.id} left - $firstLeft. Face ${second.id} left - $secondLeft. IsIdentical - $isIdentical")

        return isIdentical
    }

    private fun collectResult(): Map<Long, MutableList<Long>> {
        val result = faceToPersonMap.values.map { Pair(it, mutableListOf<Long>()) }.toMap()

        faceToPersonMap.forEach { (face, personId) ->
            result[personId]?.add(face.id)
        }

        return result
    }

    companion object {
        const val MAX_IDENTICAL_FACE_VALUE = 1f
        const val PERMISSIBLE_POSITION_VARIABLE_MULTIPLIER = 0.1

        data class Face(
                val id: Long,
                val data: FloatArray,
                var rect: Rect,
                var personId: Long?,
                var isPotentialProblem: Boolean = false
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Face

                if (id != other.id) return false

                return true
            }

            override fun hashCode(): Int {
                return id.hashCode()
            }

        }

        data class FrameParams(
                val dimension: Pair<Int, Int>,
                val frameRate: Int
        )

        fun collectFrameParams(frame: Frame, frameRate: Int): FrameParams {
            return FrameParams(
                    dimension = FileSystem.getImageDimension(frame.absolutePath),
                    frameRate = frameRate
            )
        }
    }
}