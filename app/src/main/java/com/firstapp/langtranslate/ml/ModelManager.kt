package com.firstapp.langtranslate.ml

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Manages loading and storage of on-device ML models
 */
class ModelManager(private val context: Context) {

    private val modelsDir: File by lazy {
        File(context.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Check if a model is downloaded
     */
    fun isModelDownloaded(modelName: String): Boolean {
        return File(modelsDir, "$modelName.tflite").exists()
    }

    /**
     * Get model file path
     */
    fun getModelPath(modelName: String): String {
        return File(modelsDir, "$modelName.tflite").absolutePath
    }

    /**
     * Copy model from assets to internal storage
     */
    suspend fun loadModelFromAssets(modelName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val assetFileName = "$modelName.tflite"
            val outputFile = File(modelsDir, assetFileName)

            if (outputFile.exists()) {
                return@withContext true
            }

            // Try to copy from assets if available
            try {
                context.assets.open("models/$assetFileName").use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } catch (e: Exception) {
                // Model not in assets, will use placeholder
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get available models
     */
    fun getAvailableModels(): List<String> {
        return modelsDir.listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
    }

    /**
     * Delete a model
     */
    fun deleteModel(modelName: String): Boolean {
        val file = File(modelsDir, "$modelName.tflite")
        return if (file.exists()) file.delete() else false
    }
}
