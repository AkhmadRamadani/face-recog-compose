package com.rama.facelitert.embedding

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Runs FaceNet512 via LiteRT. Same model contract as the Flutter reference
 * app (assets/tf_models/converted_model.tflite): input [1,160,160,3] float32,
 * output [1,512] float32 — the .tflite file is a plain flatbuffer, so you can
 * reuse the exact same model file here, just drop it into app/src/main/assets.
 *
 * If com.google.ai.edge.litert isn't resolvable in your setup yet, swap the
 * dependency for "org.tensorflow:tensorflow-lite:2.16.1" and change the
 * import below to org.tensorflow.lite.Interpreter — the API is identical.
 */
class FaceNetEmbedder(context: Context, modelPath: String = "converted_model.tflite") {

    companion object {
        private const val INPUT_SIZE = 160
        private const val EMBEDDING_SIZE = 512
        private const val PIXEL_MAX = 255.0f
    }

    private val interpreter: Interpreter

    init {
        val options = Interpreter.Options().apply { setNumThreads(4) }
        interpreter = Interpreter(loadModelFile(context, modelPath), options)
    }

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        val afd = context.assets.openFd(modelPath)
        FileInputStream(afd.fileDescriptor).use { input ->
            val channel = input.channel
            return channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        }
    }

    /**
     * bitmap must already be aligned + cropped + resized to 160x160
     * (see FaceAligner.alignAndCrop). Normalization matches the reference
     * app exactly: pixel value / 255, no mean subtraction.
     */
    fun getEmbedding(bitmap: Bitmap): FloatArray {
        require(bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) {
            "Input bitmap must be ${INPUT_SIZE}x$INPUT_SIZE, got ${bitmap.width}x${bitmap.height}"
        }

        val inputBuffer = ByteBuffer
            .allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / PIXEL_MAX) // R
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / PIXEL_MAX)  // G
            inputBuffer.putFloat((pixel and 0xFF) / PIXEL_MAX)          // B
        }
        inputBuffer.rewind()

        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
        interpreter.run(inputBuffer, output)

        return output[0]
    }

    fun close() = interpreter.close()
}
