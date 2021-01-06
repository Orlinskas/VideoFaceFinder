package com.orlinskas.videofacefinder.systems

import android.graphics.Rect
import androidx.annotation.Keep
import com.orlinskas.videofacefinder.data.model.FaceModel
import com.orlinskas.videofacefinder.data.model.Frame
import timber.log.Timber

class FaceDataSimpleClassifier (
        private val data: List<FaceModel>,
        frameParams: FrameParams?,
        private val isCalculateFacePosition: Boolean = false,
        private val isCalculateFaceMovement: Boolean = true
) {
    
    private var rootData: MutableMap<Long, MutableList<Face>> = mutableMapOf()
    private val potentialProblemFaces: MutableSet<Long> = mutableSetOf()
    private var personIndex = 0L
    private var iterationCount = 0L

    init {
        val frames = data.map { it.frame }.toSortedSet()
        rootData = frames.associateBy({ it }, { mutableListOf<Face>() }).toMutableMap()

        val faceModels = data.map {
            Face(
                id = it.id,
                data = it.data,
                rect = it.faceRect,
                personId = null,
                groupId = null,
                frameId = it.frame
            )
        }

        faceModels.forEach { currentFace ->
            faceModels.forEach { nextFace ->
                if (currentFace != nextFace) {
                    iterationCount++

                    val value = FaceRecognitionSystem.compare(currentFace.data, nextFace.data)
                    currentFace.compareMap[nextFace.id] = value

                    if (value < currentFace.lastNearestValue) {
                        currentFace.lastNearestValue = value
                        currentFace.lastNearestFace = nextFace.id
                    }
                }
            }
        }

        rootData.forEach { (key, _) ->
            val faces = faceModels.filter { it.frameId == key }
            rootData[key] = faces.toMutableList()
        }
    }

    fun run(): Map<Long, List<Long>> {
        val operationStartTime = System.currentTimeMillis()

        var faceGroupIndex = 0L
        val faceGroups = mutableListOf<FaceGroup>()

        val frameIds = rootData.keys.sortedBy { it }
        frameIds.forEachIndexed { index, currentFrameId ->

            if (index == frameIds.lastIndex) {
                return@forEachIndexed
            }

            val currentFrameFaces = rootData[currentFrameId]
            currentFrameFaces?.forEach { currentFrameFace ->

                if (currentFrameFace.groupId == null) {

                    val faceGroup = FaceGroup(faceGroupIndex, mutableListOf(), mutableListOf()).apply {
                        //potentialProblemFaces.remove(currentFrameFace.id)
                        faces.add(currentFrameFace)
                        frames.add(currentFrameId)
                        faceGroupIndex++
                    }

                    currentFrameFace.groupId = faceGroup.id
                    faceGroups.add(faceGroup)

                    val nextFrames = frameIds.subList(index + 1, frameIds.lastIndex)
                    nextFrames.forEachIndexed frame@{ frameIndex, nextFrameId ->

                        val nextFrameFaces = rootData[nextFrameId]
                        nextFrameFaces?.forEachIndexed { faceIndex, nextFrameFace ->

                            if (nextFrameFace.groupId == null) {
                                iterationCount++

                                if (faceGroup.isIdenticalTo(nextFrameFace)) {
                                    potentialProblemFaces.remove(nextFrameFace.id)
                                    nextFrameFace.groupId = faceGroup.id
                                    faceGroup.faces.add(nextFrameFace)
                                    faceGroup.frames.add(nextFrameId)
                                    return@frame
                                } else {
                                    potentialProblemFaces.add(nextFrameFace.id)
                                }
                            }
                        }
                    }
                }
            }
        }

        val sortedFaceGroups = faceGroups.sortedByDescending { it.faces.size }

        val groupNearestPairs = mutableListOf<Pair<Long, Long>>()

        sortedFaceGroups.forEachIndexed { index, group ->
            sortedFaceGroups.forEach { compareGroup ->
                if (group != compareGroup) {
                    iterationCount++

                    if (group.isNearestTo(compareGroup)) {
                        val pair = Pair(group.id, compareGroup.id)

                        if (!groupNearestPairs.any { it == pair || it.isInvertedTo(pair) }) {
                            groupNearestPairs.add(pair)
                        }
                    }
                }
            }
        }

        val joinedGroupPacks = mutableListOf<Set<Long>>()

        groupNearestPairs.forEach { pair ->
            val groups = mutableSetOf<Long>()

            pair.getLinked(groupNearestPairs) { linkedPair ->
                groups.add(linkedPair.first)
                groups.add(linkedPair.second)
            }

            joinedGroupPacks.add(groups.toSortedSet())
        }

        val sortedGroupPacks = joinedGroupPacks.toSet()

        val personToFacesMap = mutableMapOf<Long, List<Long>>()

        getNextPersonFaces(sortedFaceGroups, sortedGroupPacks) {
            personToFacesMap[personIndex] = it
            personIndex++
        }

        Timber.d("Nearest pairs - $groupNearestPairs")
        Timber.d("Joined group packs - $joinedGroupPacks")
        Timber.d("Sorted group packs - $sortedGroupPacks")
        Timber.d("Iterations - $iterationCount")
        Timber.d("Faces wit potential problem - ${potentialProblemFaces.size}, list - $potentialProblemFaces")
        Timber.d("Persons: \n $personToFacesMap")
        Timber.d("Found ${personToFacesMap.keys.size} persons, time - ${(System.currentTimeMillis() - operationStartTime)}ms.")

        return personToFacesMap
    }

    private fun getNextPersonFaces(faceGroups: List<FaceGroup>, groupPacks: Set<Set<Long>>, callback: (List<Long>) -> Unit) {
        val personFaces = mutableListOf<Long>()

        val faceGroup = faceGroups.firstOrNull() ?: return
        val isInPack = groupPacks.any { it.contains(faceGroup.id) }

        if (isInPack) {
            val pack = groupPacks.find { it.contains(faceGroup.id) }

            if (pack != null) {
                pack.forEach { groupId ->
                    val group = faceGroups.find { it.id == groupId }
                    group?.faces?.map { it.id }?.let { groupFaces ->
                        personFaces.addAll(groupFaces)
                    }
                }

                if (personFaces.isNotEmpty()) {
                    callback.invoke(personFaces)
                    getNextPersonFaces(faceGroups.filterNot { pack.contains(it.id) }, groupPacks, callback)
                }
            }
        } else {
            personFaces.addAll(faceGroup.faces.map { it.id })

            if (personFaces.isNotEmpty()) {
                callback.invoke(personFaces)
                getNextPersonFaces(faceGroups.filterNot { it == faceGroup }, groupPacks, callback)
            }
        }
    }

    private fun FaceGroup.isIdenticalTo(face: Face): Boolean {
        var isIdentical = false

        this.faces.forEach { groupFace ->
            val value = groupFace.compareMap[face.id]

            if (value != null && value < MAX_IDENTICAL_FACE_VALUE) {
                isIdentical = true
                return@forEach
            }
        }

        return isIdentical
    }

    private fun FaceGroup.isNearestTo(compareFaceGroup: FaceGroup): Boolean {

        val faces = this.faces.map { it.id }.toSet()
        val nearestFaces = this.faces.mapNotNull { it.lastNearestFace }.toSet()

        val compareFaces = compareFaceGroup.faces.map { it.id }.toSet()
        val compareNearestFaces = compareFaceGroup.faces.mapNotNull { it.lastNearestFace }.toSet()

        val first = faces.filter { it in compareNearestFaces }
        val second = compareFaces.filter { it in nearestFaces }

//        val result = mutableSetOf<Long>()
//        result.addAll(first)
//        result.addAll(second)

        return false//first.size == compareFaces.size
    }

    private fun Pair<Long, Long>.getLinked(list: List<Pair<Long, Long>>, callback: (Pair<Long, Long>) -> Unit) {

        list.forEach { pair ->
            iterationCount++

            if (this.isContainSome(pair)) {
                callback(pair)
                pair.getLinked(list.filterNot { it == pair }, callback)
            }
        }
    }

    private fun Pair<Long, Long>.isContainSome(pair: Pair<Long, Long>): Boolean {
        return (pair.first == this.first || pair.second == this.first
                    || pair.first == this.second || pair.second == this.second)
    }

    private fun Pair<Long, Long>.isInvertedTo(pair: Pair<Long, Long>): Boolean {
        return (pair.first == this.second && pair.second == this.first)
    }

    companion object {
        const val MAX_IDENTICAL_FACE_VALUE = 1F

        @Keep
        data class Face(
                val id: Long,
                val data: FloatArray,
                var rect: Rect,
                var personId: Long?,
                var groupId: Long?,
                var frameId: Long?,
                var isPotentialProblem: Boolean = false,
                var compareMap: MutableMap<Long, Float> = mutableMapOf(),
                var lastNearestValue: Float = Float.MAX_VALUE,
                var lastNearestFace: Long? = null
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

        @Keep
        data class FaceGroup(
                val id: Long,
                val faces: MutableList<Face>,
                val frames: MutableList<Long>
        )

        data class FrameParams(
                val dimension: Pair<Int, Int>,
                val frameRate: Double
        )

        fun collectFrameParams(frame: Frame, frameRate: Double): FrameParams {
            return FrameParams(
                    dimension = FileSystem.getImageDimension(frame.absolutePath),
                    frameRate = frameRate
            )
        }
    }
}