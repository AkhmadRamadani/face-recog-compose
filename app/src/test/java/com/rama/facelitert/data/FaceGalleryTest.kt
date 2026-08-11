package com.rama.facelitert.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FaceGalleryTest {

    @Before
    fun setUp() {
        FaceGallery.clear()
    }

    @Test
    fun testEnrollAndRecognize() {
        val embeddingAlice = FloatArray(512) { 0.1f }
        FaceGallery.enroll("Alice", embeddingAlice)

        assertEquals(1, FaceGallery.count())

        // Same or close embedding should match Alice
        val result = FaceGallery.recognize(embeddingAlice)
        assertNotNull(result)
        assertEquals("Alice", result.name)
    }

    @Test
    fun testRecognizeEmptyGallery() {
        val testEmbedding = FloatArray(512) { 0.5f }
        val result = FaceGallery.recognize(testEmbedding)
        assertNull(result.name)
    }
}
