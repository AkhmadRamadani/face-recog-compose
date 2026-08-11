package com.rama.facelitert.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rama.facelitert.camera.ImageUtils
import com.rama.facelitert.data.FaceGallery
import com.rama.facelitert.data.RecognitionResult
import com.rama.facelitert.embedding.FaceNetEmbedder
import com.rama.facelitert.facedetection.FaceAligner
import com.rama.facelitert.facedetection.FaceDetectorHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class FrameState {
    data object NoFace : FrameState()
    data class FaceFound(
        val result: RecognitionResult?,
        val faceInfo: DetectedFaceInfo? = null
    ) : FrameState()
}

class FaceRecognitionViewModel(context: Context) : ViewModel() {

    private val faceDetector = FaceDetectorHelper()
    private val embedder = FaceNetEmbedder(context.applicationContext)
    private val processingLock = Mutex()

    private val _state = MutableStateFlow<FrameState>(FrameState.NoFace)
    val state: StateFlow<FrameState> = _state

    // most recently aligned crop, kept around so it can be enrolled on tap
    private var lastAlignedFace: Bitmap? = null

    /** Call from ImageAnalysis.Analyzer. Drops frames while busy (throttling). */
    fun analyzeFrame(imageProxy: ImageProxy) {
        if (processingLock.isLocked) {
            imageProxy.close()
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            processingLock.withLock {
                try {
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val bitmap = ImageUtils.imageProxyToBitmap(imageProxy)
                    val faces = faceDetector.detect(bitmap, rotation)
                    val rotated = ImageUtils.rotateBitmap(bitmap, rotation)

                    val face = faces.firstOrNull()
                    if (face == null) {
                        _state.value = FrameState.NoFace
                        lastAlignedFace = null
                        return@withLock
                    }

                    val aligned = FaceAligner.alignAndCrop(rotated, face)
                    lastAlignedFace = aligned

                    if (aligned == null) {
                        _state.value = FrameState.NoFace
                        return@withLock
                    }

                    val embedding = embedder.getEmbedding(aligned)
                    val result = if (!FaceGallery.isEmpty()) FaceGallery.recognize(embedding) else null

                    val faceInfo = DetectedFaceInfo(
                        boundingBox = face.boundingBox,
                        name = result?.name,
                        distance = result?.distance,
                        imageWidth = rotated.width,
                        imageHeight = rotated.height,
                        isFrontCamera = true
                    )

                    _state.value = FrameState.FaceFound(result, faceInfo)
                } catch (e: Exception) {
                    _state.value = FrameState.NoFace
                } finally {
                    imageProxy.close()
                }
            }
        }
    }

    /** Enrolls whatever face was last seen in the frame, under [name]. */
    fun enrollCurrentFace(name: String): Boolean {
        val aligned = lastAlignedFace ?: return false
        val embedding = embedder.getEmbedding(aligned)
        FaceGallery.enroll(name, embedding)
        return true
    }

    override fun onCleared() {
        super.onCleared()
        faceDetector.close()
        embedder.close()
    }
}
