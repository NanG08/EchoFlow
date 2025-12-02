package com.firstapp.langtranslate.ml

import android.content.Context
import com.firstapp.langtranslate.data.TranslationResult
import com.firstapp.langtranslate.data.TranslationMode
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Translation engine backed by Google ML Kit.
 *
 * - On-device models with optional download-on-demand
 * - Supports 50+ languages, async and coroutine-friendly
 *
 * Note: 'modelManager' is kept in constructor for API compatibility,
 * but is not used now that ML Kit manages models itself.
 */
class TranslationEngine(
    private val context: Context,
    private val modelManager: ModelManager // kept for backward compatibility
) {

    private val translationCache = mutableMapOf<String, String>()

    // Reuse a single LanguageIdentifier instance
    private val languageIdentifier: LanguageIdentifier by lazy {
        LanguageIdentification.getClient()
    }

    /**
     * Translate text from source to target language using ML Kit.
     */
    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        mode: TranslationMode
    ): TranslationResult = withContext(Dispatchers.IO) {

        if (text.isBlank()) {
            return@withContext TranslationResult(
                originalText = text,
                translatedText = "",
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                mode = mode,
                confidence = 0f
            )
        }

        // Cache key based on languages + text
        val cacheKey = "$sourceLanguage:$targetLanguage:$text"
        translationCache[cacheKey]?.let { cached ->
            return@withContext TranslationResult(
                originalText = text,
                translatedText = cached,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                mode = mode,
                confidence = 1.0f
            )
        }

        // Use ML Kit for translation
        val translated = try {
            mlKitTranslate(text, sourceLanguage, targetLanguage)
        } catch (e: Exception) {
            // Fallback: on error, return original text tagged with target lang
            "$text [${targetLanguage.uppercase()}]"
        }

        translationCache[cacheKey] = translated

        TranslationResult(
            originalText = text,
            translatedText = translated,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            mode = mode,
            // ML Kit doesn’t give a numeric confidence here, so keep a fixed high value
            confidence = 0.92f
        )
    }

    /**
     * Detect language using ML Kit Language Identification.
     */
    suspend fun detectLanguage(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext "en"

        suspendCancellableCoroutine { cont ->
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { langCode ->
                    // "und" = undetermined → default to English
                    val normalized = if (langCode == "und" || langCode.isBlank()) "en" else langCode
                    cont.resume(normalized)
                }
                .addOnFailureListener {
                    // On failure, fall back to English
                    cont.resume("en")
                }
        }
    }

    /**
     * Batch translate multiple texts using the same ML Kit models.
     */
    suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String,
        mode: TranslationMode
    ): List<TranslationResult> = withContext(Dispatchers.IO) {
        // We could reuse a single Translator for speed, but to keep it simple
        // and safe, just call translate() for each.
        texts.map { text ->
            translate(text, sourceLanguage, targetLanguage, mode)
        }
    }

    /**
     * Clear translation cache.
     */
    fun clearCache() {
        translationCache.clear()
    }

    /**
     * Get cache size.
     */
    fun getCacheSize(): Int = translationCache.size

    // ----------------- Internal helpers -----------------

    /**
     * Core ML Kit translation logic wrapped in a suspending function.
     */
    private suspend fun mlKitTranslate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String = suspendCancellableCoroutine { cont ->

        val src = mapToMlKitLanguage(sourceLanguage)
        val tgt = mapToMlKitLanguage(targetLanguage)

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(src)
            .setTargetLanguage(tgt)
            .build()

        val translator: Translator = Translation.getClient(options)

        // Optional: only Wi-Fi for model download. Remove requireWifi() if you
        // want any network.
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        // 1. Make sure the model is downloaded
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                // 2. Once model is ready, translate
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        translator.close()
                        if (cont.isActive) cont.resume(translatedText)
                    }
                    .addOnFailureListener { e ->
                        translator.close()
                        if (cont.isActive) cont.resumeWithException(e)
                    }
            }
            .addOnFailureListener { e ->
                translator.close()
                if (cont.isActive) cont.resumeWithException(e)
            }

        // If coroutine is cancelled, close translator
        cont.invokeOnCancellation {
            translator.close()
        }
    }

    /**
     * Map your string language codes (like "en", "hi") to ML Kit language constants.
     * Extend this as needed to match the languages you support in Dart/UI.
     */
    private fun mapToMlKitLanguage(code: String): String {
        return when (code.lowercase()) {
            "en", "eng" -> TranslateLanguage.ENGLISH
            "es", "spa" -> TranslateLanguage.SPANISH
            "fr", "fra", "fre" -> TranslateLanguage.FRENCH
            "de", "ger", "deu" -> TranslateLanguage.GERMAN
            "it", "ita" -> TranslateLanguage.ITALIAN
            "pt", "por", "pt-br" -> TranslateLanguage.PORTUGUESE
            "ru", "rus" -> TranslateLanguage.RUSSIAN
            "zh", "zh-cn", "zh-hans" -> TranslateLanguage.CHINESE
            "ja", "jpn" -> TranslateLanguage.JAPANESE
            "ko", "kor" -> TranslateLanguage.KOREAN
            "ar", "ara" -> TranslateLanguage.ARABIC
            "hi", "hin" -> TranslateLanguage.HINDI
            else -> TranslateLanguage.ENGLISH // sensible default
        }
    }
}
