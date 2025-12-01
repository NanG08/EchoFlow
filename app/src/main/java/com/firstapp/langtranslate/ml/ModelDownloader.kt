package com.firstapp.langtranslate.ml

import android.content.Context
import com.firstapp.langtranslate.data.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages downloading and installation of language models
 * For offline language packs
 */
class ModelDownloader(
    private val context: Context,
    private val modelManager: ModelManager
) {

    data class DownloadProgress(
        val language: String,
        val progress: Float,
        val status: DownloadStatus
    )

    enum class DownloadStatus {
        PENDING,
        DOWNLOADING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Check if language pack is downloaded
     */
    fun isLanguagePackDownloaded(languageCode: String): Boolean {
        val requiredModels = listOf(
            "stt_$languageCode",
            "tts_$languageCode"
        )

        return requiredModels.all { modelName ->
            modelManager.isModelDownloaded(modelName)
        }
    }

    /**
     * Get download size for language pack
     */
    fun getLanguagePackSize(languageCode: String): Long {
        // Approximate sizes in MB
        return when (languageCode) {
            "en", "es", "fr", "de" -> 80_000_000L // 80 MB
            "zh", "ja", "ko" -> 120_000_000L // 120 MB
            "ar", "hi" -> 100_000_000L // 100 MB
            else -> 90_000_000L // 90 MB default
        }
    }

    /**
     * Download language pack
     * Returns Flow of download progress
     */
    fun downloadLanguagePack(languageCode: String): Flow<DownloadProgress> = flow {
        emit(DownloadProgress(languageCode, 0f, DownloadStatus.PENDING))

        try {
            emit(DownloadProgress(languageCode, 0.1f, DownloadStatus.DOWNLOADING))

            // Download STT model
            val sttSuccess = downloadModel("stt_$languageCode")
            emit(DownloadProgress(languageCode, 0.4f, DownloadStatus.DOWNLOADING))

            // Download TTS model
            val ttsSuccess = downloadModel("tts_$languageCode")
            emit(DownloadProgress(languageCode, 0.7f, DownloadStatus.DOWNLOADING))

            // Download translation models (if needed)
            downloadModel("translation_en_$languageCode")
            downloadModel("translation_${languageCode}_en")

            emit(DownloadProgress(languageCode, 1.0f, DownloadStatus.COMPLETED))

        } catch (e: Exception) {
            emit(DownloadProgress(languageCode, 0f, DownloadStatus.FAILED))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Download individual model
     */
    private suspend fun downloadModel(modelName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // In production, this would:
            // 1. Check if model already exists
            // 2. Download from CDN or bundled assets
            // 3. Verify checksum
            // 4. Install to models directory

            // Try to copy from assets first
            modelManager.loadModelFromAssets(modelName)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete language pack
     */
    suspend fun deleteLanguagePack(languageCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var success = true

            // Delete related models
            val modelsToDelete = listOf(
                "stt_$languageCode",
                "tts_$languageCode",
                "translation_en_$languageCode",
                "translation_${languageCode}_en"
            )

            modelsToDelete.forEach { modelName ->
                if (!modelManager.deleteModel(modelName)) {
                    success = false
                }
            }

            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get list of downloaded languages
     */
    fun getDownloadedLanguages(): List<String> {
        val downloadedModels = modelManager.getAvailableModels()
        val languages = mutableSetOf<String>()

        downloadedModels.forEach { modelName ->
            // Extract language code from model name
            when {
                modelName.startsWith("stt_") -> {
                    languages.add(modelName.removePrefix("stt_"))
                }

                modelName.startsWith("tts_") -> {
                    languages.add(modelName.removePrefix("tts_"))
                }
            }
        }

        return languages.toList()
    }

    /**
     * Get total size of downloaded models
     */
    fun getTotalDownloadedSize(): Long {
        val modelsDir = File(context.filesDir, "models")
        return modelsDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }

    /**
     * Check available storage space
     */
    fun getAvailableStorage(): Long {
        val filesDir = context.filesDir
        return filesDir.freeSpace
    }

    /**
     * Verify model integrity
     */
    suspend fun verifyModel(modelName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelPath = modelManager.getModelPath(modelName)
            val modelFile = File(modelPath)

            if (!modelFile.exists()) return@withContext false

            // In production, verify checksum against known hash
            modelFile.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get recommended languages based on device locale
     */
    fun getRecommendedLanguages(): List<String> {
        val deviceLanguage = java.util.Locale.getDefault().language

        return when (deviceLanguage) {
            "en" -> listOf("es", "fr", "de", "zh")
            "es" -> listOf("en", "fr", "pt")
            "fr" -> listOf("en", "es", "de")
            "de" -> listOf("en", "fr", "es")
            "zh" -> listOf("en", "ja", "ko")
            "ja" -> listOf("en", "zh", "ko")
            "ko" -> listOf("en", "zh", "ja")
            else -> listOf("en", "es", "fr")
        }
    }
}
