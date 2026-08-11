package com.rama.facelitert.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.rama.facelitert.data.FaceGallery
import java.util.concurrent.Executors

@Composable
fun FaceRecognitionScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = remember { FaceRecognitionViewModel(context) }
    val frameState by viewModel.state.collectAsState()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var nameInput by remember { mutableStateOf("") }
    var enrolledCount by remember { mutableIntStateOf(FaceGallery.count()) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysisUseCase ->
                            analysisUseCase.setAnalyzer(cameraExecutor) { imageProxy ->
                                viewModel.analyzeFrame(imageProxy)
                            }
                        }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysis
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        val faceInfo = (frameState as? FrameState.FaceFound)?.faceInfo
        FaceOverlayCanvas(
            faceInfo = faceInfo,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(16.dp)
        ) {
            val statusText = when (val s = frameState) {
                is FrameState.NoFace -> "No face detected"
                is FrameState.FaceFound -> {
                    val r = s.result
                    when {
                        r == null && FaceGallery.isEmpty() -> "Face detected — gallery empty, enroll someone"
                        r?.name != null -> "Recognized: ${r.name}  (distance ${"%.3f".format(r.distance)})"
                        else -> "Unknown face  (distance ${"%.3f".format(r?.distance ?: 1f)})"
                    }
                }
            }
            Text(statusText, color = Color.White)

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (nameInput.isNotBlank() && viewModel.enrollCurrentFace(nameInput.trim())) {
                        enrolledCount = FaceGallery.count()
                        nameInput = ""
                    }
                }) {
                    Text("Enroll")
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Enrolled people: $enrolledCount", color = Color.White)
        }
    }
}
