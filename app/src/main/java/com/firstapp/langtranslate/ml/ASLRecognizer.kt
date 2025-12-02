package com.firstapp.langtranslate.ml

import android.content.Context
import android.graphics.Bitmap
import com.firstapp.langtranslate.data.ASLResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ASLRecognizer(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var isInitialized = false

    // ASL character mapping (59 classes including padding)
    private val aslCharacters = listOf(
        "_", // Padding token
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "space", "del", "nothing",
        "hello", "yes", "no", "please", "thank you", "sorry",
        "help", "good", "bad", "more", "stop", "go",
        "I", "you", "me", "we", "love", "like",
        "other" // 59th class
    )

    companion object {
        private const val MODEL_FILE = "C:\\Users\\Nandika\\AndroidStudioProjects\\LangTranslate\\app\\src\\main\\assets\\asl_model.tflite"
        private const val INPUT_SIZE = 64 // 64x64 RGB image
        private const val NUM_CHANNELS = 3 // RGB
        private const val NUM_CLASSES = 59
        private const val CONFIDENCE_THRESHOLD = 0.6f
    }

    fun initialize(): Boolean {
        return try {
            println("🔍 Attempting to load ASL model: $MODEL_FILE")

            val assetFiles = context.assets.list("") ?: emptyArray()
            println("📁 Files in assets root: ${assetFiles.joinToString()}")

            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)
            println("✅ Model file loaded successfully, size: ${modelBuffer.capacity()} bytes")

            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(true)
            }
            interpreter = Interpreter(modelBuffer, options)
            isInitialized = true
            println("✅ ASL model initialized successfully")
            true
        } catch (e: Exception) {
            println("❌ Failed to initialize ASL model: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            isInitialized = false
            false
        }
    }

    fun recognizeSign(bitmap: Bitmap): Flow<ASLResult> = flow {
        if (!isInitialized) {
            emit(
                ASLResult(
                    character = "",
                    confidence = 0f,
                    allProbabilities = emptyMap(),
                    error = "Model not initialized. Please add asl_model.tflite to assets folder."
                )
            )
            return@flow
        }

        try {
            val inputBuffer = preprocessImage(bitmap)

            val outputBuffer = Array(1) { FloatArray(NUM_CLASSES) }
            interpreter?.run(inputBuffer, outputBuffer)

            val probabilities = outputBuffer[0]
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val maxConfidence = probabilities[maxIndex]

            // Build a Map<String, Float> of top 5 predictions
            val allProbs: Map<String, Float> = probabilities
                .mapIndexed { index, prob ->
                    val label = aslCharacters.getOrNull(index) ?: "?"
                    label to prob
                }
                .sortedByDescending { pair -> pair.second }
                .take(5)
                .associate { pair -> pair.first to pair.second }

            if (maxConfidence >= CONFIDENCE_THRESHOLD) {
                val character = aslCharacters.getOrNull(maxIndex) ?: "?"
                emit(
                    ASLResult(
                        character = character,
                        confidence = maxConfidence,
                        allProbabilities = allProbs
                    )
                )
            } else {
                emit(
                    ASLResult(
                        character = "",
                        confidence = maxConfidence,
                        allProbabilities = allProbs,
                        isLowConfidence = true
                    )
                )
            }

        } catch (e: Exception) {
            emit(
                ASLResult(
                    character = "",
                    confidence = 0f,
                    allProbabilities = emptyMap(),
                    error = "Recognition error: ${e.message}"
                )
            )
        }
    }.flowOn(Dispatchers.Default)

    fun recognizeContinuous(frames: Flow<Bitmap>): Flow<ASLResult> = flow {
        frames.collect { frame ->
            recognizeSign(frame).collect { result ->
                emit(result)
            }
        }
    }.flowOn(Dispatchers.Default)

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * NUM_CHANNELS)
        inputBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaledBitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                inputBuffer.putFloat(((value shr 16) and 0xFF) / 255.0f)
                inputBuffer.putFloat(((value shr 8) and 0xFF) / 255.0f)
                inputBuffer.putFloat((value and 0xFF) / 255.0f)
            }
        }

        inputBuffer.rewind()
        return inputBuffer
    }

    fun getCharacter(index: Int): String {
        return aslCharacters.getOrNull(index) ?: "?"
    }

    fun getSupportedCharacters(): List<String> = aslCharacters

    fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
}
