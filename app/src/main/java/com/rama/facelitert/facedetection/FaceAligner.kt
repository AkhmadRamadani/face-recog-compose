package com.rama.facelitert.facedetection

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

/**
 * Alignment + crop + normalization step, standing in for the Flutter app's
 * "Deteksi & Crop Wajah (ML Kit Face Detector)" stage — but goes one step
 * further and rotates the face so the eyes are horizontal before cropping,
 * which meaningfully improves FaceNet embedding quality on off-axis faces.
 */
object FaceAligner {

    private const val TARGET_SIZE = 160
    private const val MARGIN_RATIO = 0.25f // extra crop margin around the bbox

    /**
     * Returns a 160x160 bitmap ready for FaceNetEmbedder, or null if the
     * face has no usable eye landmarks / bbox collapses after rotation.
     */
    fun alignAndCrop(source: Bitmap, face: Face): Bitmap? {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        val rotated: Bitmap
        val rotatedBox: Rect

        if (leftEye != null && rightEye != null) {
            val dy = rightEye.y - leftEye.y
            val dx = rightEye.x - leftEye.x
            val angleDeg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

            val cx = face.boundingBox.exactCenterX()
            val cy = face.boundingBox.exactCenterY()

            val matrix = Matrix().apply { postRotate(-angleDeg, cx, cy) }
            rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            rotatedBox = remapRect(face.boundingBox, matrix, source.width, source.height)
        } else {
            rotated = source
            rotatedBox = face.boundingBox
        }

        val marginX = (rotatedBox.width() * MARGIN_RATIO).toInt()
        val marginY = (rotatedBox.height() * MARGIN_RATIO).toInt()

        val left = max(0, rotatedBox.left - marginX)
        val top = max(0, rotatedBox.top - marginY)
        val right = min(rotated.width, rotatedBox.right + marginX)
        val bottom = min(rotated.height, rotatedBox.bottom + marginY)

        if (right <= left || bottom <= top) return null

        val cropped = Bitmap.createBitmap(rotated, left, top, right - left, bottom - top)
        return Bitmap.createScaledBitmap(cropped, TARGET_SIZE, TARGET_SIZE, true)
    }

    private fun remapRect(rect: Rect, matrix: Matrix, boundW: Int, boundH: Int): Rect {
        val pts = floatArrayOf(
            rect.left.toFloat(), rect.top.toFloat(),
            rect.right.toFloat(), rect.top.toFloat(),
            rect.right.toFloat(), rect.bottom.toFloat(),
            rect.left.toFloat(), rect.bottom.toFloat()
        )
        matrix.mapPoints(pts)
        val xs = floatArrayOf(pts[0], pts[2], pts[4], pts[6])
        val ys = floatArrayOf(pts[1], pts[3], pts[5], pts[7])
        val left = xs.min().toInt().coerceIn(0, boundW)
        val right = xs.max().toInt().coerceIn(0, boundW)
        val top = ys.min().toInt().coerceIn(0, boundH)
        val bottom = ys.max().toInt().coerceIn(0, boundH)
        return Rect(left, top, right, bottom)
    }
}
