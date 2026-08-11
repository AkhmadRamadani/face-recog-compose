package com.rama.facelitert.ui

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.max

data class DetectedFaceInfo(
    val boundingBox: Rect,
    val name: String?,
    val distance: Float?,
    val imageWidth: Int,
    val imageHeight: Int,
    val isFrontCamera: Boolean = true
)

@Composable
fun FaceOverlayCanvas(
    faceInfo: DetectedFaceInfo?,
    modifier: Modifier = Modifier
) {
    if (faceInfo == null || faceInfo.imageWidth <= 0 || faceInfo.imageHeight <= 0) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val scaleX = canvasWidth / faceInfo.imageWidth.toFloat()
        val scaleY = canvasHeight / faceInfo.imageHeight.toFloat()
        val scale = max(scaleX, scaleY)

        val offsetX = (canvasWidth - faceInfo.imageWidth * scale) / 2f
        val offsetY = (canvasHeight - faceInfo.imageHeight * scale) / 2f

        val box = faceInfo.boundingBox

        val mappedLeft: Float
        val mappedRight: Float
        if (faceInfo.isFrontCamera) {
            mappedLeft = canvasWidth - (box.right * scale + offsetX)
            mappedRight = canvasWidth - (box.left * scale + offsetX)
        } else {
            mappedLeft = box.left * scale + offsetX
            mappedRight = box.right * scale + offsetX
        }

        val mappedTop = box.top * scale + offsetY
        val mappedBottom = box.bottom * scale + offsetY

        val rectWidth = mappedRight - mappedLeft
        val rectHeight = mappedBottom - mappedTop

        if (rectWidth <= 0 || rectHeight <= 0) return@Canvas

        val isRecognized = !faceInfo.name.isNullOrBlank()
        val accentColor = when {
            isRecognized -> Color(0xFF00E676) // Emerald Green
            faceInfo.distance != null -> Color(0xFFFF9100) // Vibrant Orange
            else -> Color(0xFF2979FF) // Electric Blue
        }

        // 1. Semi-transparent box fill
        drawRoundRect(
            color = accentColor.copy(alpha = 0.12f),
            topLeft = Offset(mappedLeft, mappedTop),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(20f, 20f)
        )

        // 2. Main Bounding Box Border
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(mappedLeft, mappedTop),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(width = 5f)
        )

        // 3. Corner Brackets (HUD style)
        val cornerLen = minOf(rectWidth, rectHeight) * 0.2f
        val bracketStroke = 8f

        // Top-Left corner
        drawLine(accentColor, Offset(mappedLeft, mappedTop), Offset(mappedLeft + cornerLen, mappedTop), strokeWidth = bracketStroke)
        drawLine(accentColor, Offset(mappedLeft, mappedTop), Offset(mappedLeft, mappedTop + cornerLen), strokeWidth = bracketStroke)

        // Top-Right corner
        drawLine(accentColor, Offset(mappedRight, mappedTop), Offset(mappedRight - cornerLen, mappedTop), strokeWidth = bracketStroke)
        drawLine(accentColor, Offset(mappedRight, mappedTop), Offset(mappedRight, mappedTop + cornerLen), strokeWidth = bracketStroke)

        // Bottom-Left corner
        drawLine(accentColor, Offset(mappedLeft, mappedBottom), Offset(mappedLeft + cornerLen, mappedBottom), strokeWidth = bracketStroke)
        drawLine(accentColor, Offset(mappedLeft, mappedBottom), Offset(mappedLeft, mappedBottom - cornerLen), strokeWidth = bracketStroke)

        // Bottom-Right corner
        drawLine(accentColor, Offset(mappedRight, mappedBottom), Offset(mappedRight - cornerLen, mappedBottom), strokeWidth = bracketStroke)
        drawLine(accentColor, Offset(mappedRight, mappedBottom), Offset(mappedRight, mappedBottom - cornerLen), strokeWidth = bracketStroke)

        // 4. "Who is it" Label Badge
        val labelText = when {
            isRecognized -> "👤 ${faceInfo.name}" + (faceInfo.distance?.let { " (dist: ${"%.2f".format(it)})" } ?: "")
            faceInfo.distance != null -> "❓ Unknown Person"
            else -> "✨ Face Detected"
        }

        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textWidth = textPaint.measureText(labelText)
        val textHeight = 42f

        val paddingH = 24f
        val paddingV = 16f

        val pillWidth = textWidth + (paddingH * 2)
        val pillHeight = textHeight + (paddingV * 2)

        var pillTop = mappedTop - pillHeight - 12f
        if (pillTop < 20f) {
            pillTop = mappedTop + 12f
        }
        val pillLeft = maxOf(mappedLeft, 16f)

        // Draw Badge Background Pill
        drawRoundRect(
            color = Color(0xEC12131C),
            topLeft = Offset(pillLeft, pillTop),
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // Draw Accent Left Bar on Badge
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(pillLeft, pillTop),
            size = Size(10f, pillHeight),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Draw Badge Text
        drawContext.canvas.nativeCanvas.drawText(
            labelText,
            pillLeft + paddingH + 6f,
            pillTop + paddingV + textHeight - 8f,
            textPaint
        )
    }
}
