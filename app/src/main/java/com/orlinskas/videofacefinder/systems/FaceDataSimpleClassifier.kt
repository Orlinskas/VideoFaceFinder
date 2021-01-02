package com.orlinskas.videofacefinder.systems

import android.graphics.Rect
import com.orlinskas.videofacefinder.data.model.FaceModel
import com.orlinskas.videofacefinder.data.model.Frame
import timber.log.Timber

class FaceDataSimpleClassifier (
        data: List<FaceModel>,
        frameParams: FrameParams?,
        private val isCalculateFacePosition: Boolean = true,
        private val isCalculateFaceMovement: Boolean = true
) {
    
    private var rootData: MutableList<Face> = mutableListOf()
    private val faceToPersonMap: MutableMap<Long, Long> = mutableMapOf()
    private var personIndex = 0L
    private var iterationCount = 0L
    private val persons: MutableMap<Long, MutableList<Long>> = mutableMapOf()

    init {
        rootData = data.map {
            Face(
                id = it.id,
                data = it.data,
                rect = it.faceRect,
                personId = null
            )
        }.toMutableList()
    }

    fun run(): Map<Long, MutableList<Long>> {

        rootData.forEach { currentFace ->
            val iterator: MutableListIterator<Face> = rootData.listIterator()

            while (iterator.hasNext()) {
                iterationCount++
                val nextFace = iterator.next()

                if (nextFace.personId == null) {
                    if (isIdentical(currentFace, nextFace)) {
                        Timber.d("Face ${currentFace.id} identical to face ${nextFace.id}")
                        catchResult(currentFace, nextFace)
                    } else {
                        if (isCalculateFacePosition) {
                            if (isIdenticalForFacePosition(currentFace, nextFace)) {
                                Timber.d("Face ${currentFace.id} identical to face ${nextFace.id} for position")
                                catchResult(currentFace, nextFace)
                            }
                        }
                    }
                }
            }
        }

        Timber.d("Finished classified with $iterationCount iterations.")

        return collectResult()
    }

    private fun isIdentical(first: Face, second: Face): Boolean {
        val compareValue = FaceRecognitionSystem.compare(first.data, second.data)
        Timber.i("Face ${first.id} came close to face ${second.id} by - $compareValue")

        return compareValue < MAX_IDENTICAL_FACE_VALUE
    }

    private fun catchResult(currentFace: Face, nextFace: Face) {
        val currentFacePerson = faceToPersonMap[currentFace.id]

        if (currentFacePerson == null) {
            currentFace.personId = personIndex
            nextFace.personId = personIndex
            faceToPersonMap[currentFace.id] = personIndex
            faceToPersonMap[nextFace.id] = personIndex
            personIndex++
        } else {
            nextFace.personId = personIndex
            faceToPersonMap[nextFace.id] = currentFacePerson
        }
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

        faceToPersonMap.forEach { (faceId, personId) ->
            result[personId]?.add(faceId)
        }

        return result
    }

    companion object {
        const val MAX_IDENTICAL_FACE_VALUE = 0.9f
        const val PERMISSIBLE_POSITION_VARIABLE_MULTIPLIER = 0.1

        data class Face(
                val id: Long,
                val data: FloatArray,
                var rect: Rect,
                var personId: Long?

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