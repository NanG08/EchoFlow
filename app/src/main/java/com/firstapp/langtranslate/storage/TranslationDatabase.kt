package com.firstapp.langtranslate.storage

import android.content.Context
import com.firstapp.langtranslate.data.TranslationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local storage for translation history and offline data
 * All data stored on-device only
 */
class TranslationDatabase(private val context: Context) {

    private val historyFile: File by lazy {
        File(context.filesDir, "translation_history.json")
    }

    private val settingsFile: File by lazy {
        File(context.filesDir, "settings.json")
    }

    /**
     * Save translation to history
     */
    suspend fun saveTranslation(result: TranslationResult) = withContext(Dispatchers.IO) {
        try {
            val history = loadHistory().toMutableList()
            history.add(0, result) // Add to beginning

            // Keep only last 1000 entries
            if (history.size > 1000) {
                history.subList(1000, history.size).clear()
            }

            saveHistory(history)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load translation history
     */
    suspend fun loadHistory(): List<TranslationResult> = withContext(Dispatchers.IO) {
        try {
            if (!historyFile.exists()) {
                return@withContext emptyList()
            }

            val jsonString = historyFile.readText()
            val jsonArray = JSONArray(jsonString)

            val results = mutableListOf<TranslationResult>()
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                results.add(parseTranslationResult(json))
            }

            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Save history to file
     */
    private fun saveHistory(history: List<TranslationResult>) {
        try {
            val jsonArray = JSONArray()
            history.forEach { result ->
                jsonArray.put(serializeTranslationResult(result))
            }

            historyFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clear all history
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        try {
            if (historyFile.exists()) {
                historyFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Search history
     */
    suspend fun searchHistory(query: String): List<TranslationResult> =
        withContext(Dispatchers.IO) {
            val history = loadHistory()
            history.filter { result ->
                result.originalText.contains(query, ignoreCase = true) ||
                        result.translatedText.contains(query, ignoreCase = true)
            }
        }

    /**
     * Save user settings
     */
    suspend fun saveSettings(settings: Map<String, Any>) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(settings)
            settingsFile.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load user settings
     */
    suspend fun loadSettings(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            if (!settingsFile.exists()) {
                return@withContext getDefaultSettings()
            }

            val jsonString = settingsFile.readText()
            val json = JSONObject(jsonString)

            val settings = mutableMapOf<String, Any>()
            json.keys().forEach { key ->
                settings[key] = json.get(key)
            }

            settings
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultSettings()
        }
    }

    /**
     * Get default settings
     */
    private fun getDefaultSettings(): Map<String, Any> {
        return mapOf(
            "sourceLanguage" to "en",
            "targetLanguage" to "es",
            "autoDetectLanguage" to true,
            "continuousMode" to true,
            "showConfidence" to false,
            "hapticFeedback" to true,
            "darkMode" to false
        )
    }

    /**
     * Serialize TranslationResult to JSON
     */
    private fun serializeTranslationResult(result: TranslationResult): JSONObject {
        return JSONObject().apply {
            put("originalText", result.originalText)
            put("translatedText", result.translatedText)
            put("sourceLanguage", result.sourceLanguage)
            put("targetLanguage", result.targetLanguage)
            put("timestamp", result.timestamp)
            put("mode", result.mode.name)
            put("confidence", result.confidence)
        }
    }

    /**
     * Parse TranslationResult from JSON
     */
    private fun parseTranslationResult(json: JSONObject): TranslationResult {
        return TranslationResult(
            originalText = json.getString("originalText"),
            translatedText = json.getString("translatedText"),
            sourceLanguage = json.getString("sourceLanguage"),
            targetLanguage = json.getString("targetLanguage"),
            timestamp = json.getLong("timestamp"),
            mode = com.firstapp.langtranslate.data.TranslationMode.valueOf(
                json.getString("mode")
            ),
            confidence = json.getDouble("confidence").toFloat()
        )
    }
}
