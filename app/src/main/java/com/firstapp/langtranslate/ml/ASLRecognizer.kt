package com.firstapp.langtranslate.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
//import androidx.privacysandbox.tools.core.generator.build
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ASLRecognizer(
    private val context: Context,
    private val modelPath: String = "asl_model.tflite" // Your TFLite model in assets
) : ImageAnalysis.Analyzer {

    private val _recognizedLetter = MutableLiveData<String>()
    val recognizedLetter: LiveData<String> = _recognizedLetter

    private var handLandmarker: HandLandmarker? = null
    private var tfliteInterpreter: Interpreter? = null

    init {
        setupHandLandmarker()
        setupTFLiteInterpreter()
    }

    private fun setupHandLandmarker() {
        try {
            val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath("hand_landmarker.task")
            val baseOptions = baseOptionsBuilder.build()
            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setRunningMode(RunningMode.IMAGE) // Use IMAGE mode for frame-by-frame analysis

            val options = optionsBuilder.build()
            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e("ASLRecognizer", "Error setting up HandLandmarker: ${e.message}")
        }
    }

    private fun setupTFLiteInterpreter() {
        try {
            val model = loadModelFile(context, modelPath)
            tfliteInterpreter = Interpreter(model)
        } catch (e: Exception) {
            Log.e("ASLRecognizer", "Error setting up TFLite Interpreter: ${e.message}")
        }
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (handLandmarker == null || tfliteInterpreter == null) {
            imageProxy.close()
            return
        }

        // Convert ImageProxy to a rotated Bitmap
        val bitmap = imageProxy.toBitmap()
        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // Detect hand landmarks using MediaPipe
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        val handLandmarkerResult = handLandmarker?.detect(mpImage)

        // Process landmarks and run inference
        handLandmarkerResult?.let { result ->
            if (result.landmarks().isNotEmpty()) {
                // Assuming the model expects a flattened array of landmark coordinates
                // This will need to be adjusted based on your model's specific input shape
                val landmarks = result.landmarks()[0]
                val inputData = FloatArray(landmarks.size * 3) // Example: 21 landmarks * 3 coords (x, y, z)
                var index = 0
                for (landmark in landmarks) {
                    inputData[index++] = landmark.x()
                    inputData[index++] = landmark.y()
                    inputData[index++] = landmark.z()
                }

                // Run inference with your TFLite model
                // The output shape and processing will depend on your model
                // Example for a 26-class (A-Z) model
                val output = Array(1) { FloatArray(26) }
                tfliteInterpreter?.run(arrayOf(inputData), output)

                // Get the letter with the highest confidence
                val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
                if (maxIndex != -1) {
                    val predictedLetter = ('A' + maxIndex).toString()
                    _recognizedLetter.postValue(predictedLetter) // Post value to LiveData
                }
            }
        }

        imageProxy.close()
    }
}
