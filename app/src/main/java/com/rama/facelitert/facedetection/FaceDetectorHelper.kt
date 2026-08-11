package com.rama.facelitert.facedetection

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps ML Kit's on-device Face Detector.
 * Equivalent to google_mlkit_face_detection in the Flutter reference app,
 * but with LANDMARK_MODE_ALL so we get eye positions for alignment.
 */
class FaceDetectorHelper {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    suspend fun detect(bitmap: Bitmap, rotationDegrees: Int = 0): List<Face> =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { faces -> cont.resume(faces) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    fun close() = detector.close()
}
