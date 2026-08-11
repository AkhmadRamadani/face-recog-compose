package com.rama.facelitert.embedding

import kotlin.math.sqrt

/** Port of the reference app's cosineDistance512 (test/check_similarity.dart). */
object SimilarityUtil {

    const val MATCH_THRESHOLD = 0.3f

    /** Cosine distance = 1 - cosine similarity. Lower means more similar. */
    fun cosineDistance(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Embeddings must be the same length" }
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        if (denom == 0f) return 1f
        val cosineSimilarity = dot / denom
        return 1f - cosineSimilarity
    }

    fun isSamePerson(a: FloatArray, b: FloatArray): Boolean =
        cosineDistance(a, b) < MATCH_THRESHOLD
}
