package com.rama.facelitert.data

import com.rama.facelitert.embedding.SimilarityUtil

data class EnrolledFace(val name: String, val embedding: FloatArray)

data class RecognitionResult(val name: String?, val distance: Float)

/**
 * This is the piece that turns the reference app's 1:1 "compare two photos"
 * flow into actual 1:N recognition: enroll several people, then match a
 * live embedding against all of them and keep the nearest one under the
 * threshold. In-memory for now — swap for Room, or your Kiddo-backed store
 * from deepface-rs, if you need persistence across app restarts.
 */
object FaceGallery {

    private val enrolled = mutableListOf<EnrolledFace>()

    fun enroll(name: String, embedding: FloatArray) {
        enrolled.add(EnrolledFace(name, embedding))
    }

    fun clear() = enrolled.clear()
    fun isEmpty() = enrolled.isEmpty()
    fun count() = enrolled.size

    fun recognize(embedding: FloatArray): RecognitionResult {
        if (enrolled.isEmpty()) return RecognitionResult(null, Float.MAX_VALUE)

        var bestName: String? = null
        var bestDistance = Float.MAX_VALUE

        for (face in enrolled) {
            val d = SimilarityUtil.cosineDistance(embedding, face.embedding)
            if (d < bestDistance) {
                bestDistance = d
                bestName = face.name
            }
        }

        return if (bestDistance < SimilarityUtil.MATCH_THRESHOLD) {
            RecognitionResult(bestName, bestDistance)
        } else {
            RecognitionResult(null, bestDistance)
        }
    }
}
