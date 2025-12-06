package com.firstapp.langtranslate.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.firstapp.langtranslate.data.BoundingBox
import com.firstapp.langtranslate.data.OCRResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * On-device OCR engine using TensorFlow Lite.
 * Extracts text from images in real-time.
 * This class now implements ImageAnalysis.Analyzer to work directly with CameraX.
 */
class OCREngine(
    private val context: Context,
    private val modelManager: ModelManager
) : ImageAnalysis.Analyzer { // <<< FIX 1: Implement the interface

    // --- FIX 2: Add LiveData to post results back to the UI ---
    private val _ocrResult = MutableLiveData<String>()
    val ocrResult: LiveData<String> = _ocrResult

    companion object {
        private const val INPUT_SIZE = 320
        private const val CONFIDENCE_THRESHOLD = 0.5f
    }

    /**
     * This is the required method from the ImageAnalysis.Analyzer interface.
     * It will be called for every frame from the camera.
     */
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        // Convert the ImageProxy to a Bitmap
        val bitmap = imageProxy.toBitmap()

        // Launch a coroutine to process the image without blocking the camera thread
        CoroutineScope(Dispatchers.Default).launch {
            val result = recognizeText(bitmap, "en") // Assuming 'en' for live analysis
            _ocrResult.postValue(result.text) // Post the result text to LiveData
        }

        // CRUCIAL: You must close the ImageProxy, or the camera will stop producing images.
        imageProxy.close()
    }

    /**
     * Perform OCR on a bitmap image. This is your existing function.
     */
    suspend fun recognizeText(bitmap: Bitmap, languageCode: String): OCRResult =
        withContext(Dispatchers.Default) {
            try {
                // Preprocess image
                val processedBitmap = preprocessImage(bitmap)

                // Run OCR model inference
                val result = runOCRInference(processedBitmap, languageCode)

                result
            } catch (e: Exception) {
                e.printStackTrace()
                OCRResult(
                    text = "",
                    boundingBoxes = emptyList(),
                    confidence = 0f
                )
            }
        }

    /**
     * Preprocess image for OCR model.
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        // Resize to model input size
        return Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
    }

    /**
     * Run OCR inference using TFLite model.
     */
    private fun runOCRInference(bitmap: Bitmap, languageCode: String): OCRResult {
        // For demonstration, simulate OCR result
        val extractedText = simulateOCR(bitmap, languageCode)

        // Simulate bounding boxes
        val boundingBoxes = listOf(
            BoundingBox(0.1f, 0.1f, 0.9f, 0.3f, extractedText)
        )

        return OCRResult(
            text = extractedText,
            boundingBoxes = boundingBoxes,
            confidence = 0.88f
        )
    }

    private val sampleTexts = listOf(
        "Welcome to our restaurant",
        "Exit this way",
        "Menu: Coffee $3.50",
        "Open 9 AM - 5 PM",
        "Please wash your hands",
        "No parking",
        "Emergency exit",
        "Thank you for visiting"
    )
    private var textIndex = 0

    /**
     * Simulate OCR extraction (replace with actual model inference).
     */
    private fun simulateOCR(bitmap: Bitmap, languageCode: String): String {
        val text = sampleTexts[textIndex % sampleTexts.size]
        textIndex++
        return text
    }

    /**
     * Convert bitmap to ByteBuffer for TFLite input.
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                byteBuffer.putFloat(((value shr 16 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value and 0xFF) - 127.5f) / 127.5f)
            }
        }
        return byteBuffer
    }

    /**
     * Process real-time camera frame.
     */
    suspend fun processFrame(bitmap: Bitmap, languageCode: String): OCRResult {
        return recognizeText(bitmap, languageCode)
    }

    /**
     * Batch process multiple images.
     */
    suspend fun recognizeBatch(bitmaps: List<Bitmap>, languageCode: String): List<OCRResult> =
        withContext(Dispatchers.Default) {
            bitmaps.map { bitmap ->
                recognizeText(bitmap, languageCode)
            }
        }
}

/**
 * Extension function to convert ImageProxy to Bitmap.
 * This handles the YUV_420_888 format from CameraX.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
fun ImageProxy.toBitmap(): Bitmap {
    val image = this.image ?: throw IllegalStateException("ImageProxy image is null")

    // Get the YUV planes
    val yBuffer = image.planes[0].buffer // Y
    val uBuffer = image.planes[1].buffer // U
    val vBuffer = image.planes[2].buffer // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // U and V are swapped
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, this.width, this.height), 100, out)
    val imageBytes = out.toByteArray()

    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    // Rotate bitmap if needed based on rotation degrees
    return if (this.imageInfo.rotationDegrees != 0) {
        val matrix = Matrix().apply {
            postRotate(this@toBitmap.imageInfo.rotationDegrees.toFloat())
        }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}
