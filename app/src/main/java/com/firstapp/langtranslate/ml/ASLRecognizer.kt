package com.firstapp.langtranslate.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// A listener to receive the recognition results.
// This defines a type alias for a lambda function.
typealias GestureRecognizerListener = (result: GestureRecognizerResult, outputImage: MPImage) -> Unit

class ASLRecognizer(
    private val context: Context,
    private var minHandDetectionConfidence: Float = 0.5f,
    private var minHandPresenceConfidence: Float = 0.5f,
    private var gestureRecognizerListener: GestureRecognizerListener? = null
) : ImageAnalysis.Analyzer {
    private var gestureRecognizer: GestureRecognizer? = null
    private lateinit var backgroundExecutor: ExecutorService

    // LiveData to expose recognized letters to MainActivity
    private val _recognizedLetter = MutableLiveData<String>()
    val recognizedLetter: LiveData<String> = _recognizedLetter

    private val recognizedText = StringBuilder()

    init {
        // Automatically initialize when an instance is created.
        setup()
    }

    private fun setup() {
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Create a BaseOptions object for the recognizer.
        val baseOptionsBuilder = BaseOptions.builder()
            // Make sure the model file 'gesture_recognizer.task' is in your 'app/src/main/assets' folder.
            .setModelAssetPath("gesture_recognizer.task")

        val baseOptions = baseOptionsBuilder.build()

        // Configure the GestureRecognizer with options.
        val optionsBuilder = GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM) // Set to LIVE_STREAM for camera input.
            .setNumHands(1) // We are looking for one hand for ASL fingerspelling.
            .setMinHandDetectionConfidence(minHandDetectionConfidence)
            .setMinHandPresenceConfidence(minHandPresenceConfidence)
            .setResultListener(this::returnLivestreamResult) // Set up the listener for results.
            .setErrorListener(this::returnLivestreamError) // Set up the listener for errors.

        val options = optionsBuilder.build()
        try {
            gestureRecognizer = GestureRecognizer.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "MediaPipe failed to load the model: " + e.message)
        }
    }

    /**
     * Implementation of ImageAnalysis.Analyzer interface for CameraX integration.
     * This method is called for every camera frame.
     */
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        recognizeLiveStream(imageProxy)
    }

    // The main recognition function for live stream camera input.
    fun recognizeLiveStream(imageProxy: ImageProxy) {
        val frameTime = System.currentTimeMillis()

        // Convert the ImageProxy to a MediaPipe MPImage and recognize gestures.
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(it.planes[0].buffer) }

        val matrix = Matrix().apply {
            // Rotate the image to match the device's orientation.
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            // Flip the image horizontally for a mirror-like effect, which is natural for a front-facing camera.
            postScale(-1f, 1f, imageProxy.width / 2f, imageProxy.height / 2f)
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, false
        )

        // Convert the rotated Bitmap to an MPImage and send it for asynchronous recognition.
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        gestureRecognizer?.recognizeAsync(mpImage, frameTime)
    }

    // This private function is called by the GestureRecognizer when a result is available.
    private fun returnLivestreamResult(result: GestureRecognizerResult, input: MPImage) {
        // Pass the result to the listener (which is the ASLFragment).
        gestureRecognizerListener?.invoke(result, input)

        // Also update LiveData for MainActivity
        if (result.gestures().isNotEmpty()) {
            val topGesture = result.gestures()[0][0]
            val categoryName = topGesture.categoryName()
            val confidence = topGesture.score()

            // Handle special characters for building up text
            when (categoryName) {
                "space" -> recognizedText.append(" ")
                "del" -> {
                    if (recognizedText.isNotEmpty()) {
                        recognizedText.deleteCharAt(recognizedText.length - 1)
                    }
                }

                "nothing", "_" -> {
                    // Ignore these gestures
                }

                else -> {
                    // Only add letters if confidence is high enough
                    if (confidence > 0.7f) {
                        recognizedText.append(categoryName)
                    }
                }
            }

            // Post the accumulated text to LiveData
            _recognizedLetter.postValue(recognizedText.toString())
        }
    }

    // This private function is called by the GestureRecognizer when an error occurs.
    private fun returnLivestreamError(error: RuntimeException) {
        Log.e(TAG, "Gesture Recognizer Error: ${error.message}")
    }

    // Call this method from the fragment's onDestroyView to release resources.
    fun close() {
        if (::backgroundExecutor.isInitialized && !backgroundExecutor.isShutdown) {
            backgroundExecutor.shutdown()
        }
        gestureRecognizer?.close()
        gestureRecognizer = null
    }

    companion object {
        const val TAG = "ASLRecognizer"
    }
}
