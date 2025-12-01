package com.firstapp.langtranslate.ml

import android.content.Context
import android.speech.tts.TextToSpeech as AndroidTTS
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Wrapper for Android's built-in TextToSpeech
 * Provides high-quality speech synthesis
 */
class TextToSpeechWrapper(private val context: Context) {

    private var androidTTS: AndroidTTS? = null
    private var isSpeaking = false
    private var ttsReady = false

    init {
        androidTTS = AndroidTTS(context) { status ->
            if (status == AndroidTTS.SUCCESS) {
                ttsReady = true
            }
        }
    }

    /**
     * Speak text using Android TTS
     */
    suspend fun speak(text: String, languageCode: String): Boolean = withContext(Dispatchers.Main) {
        if (text.isBlank()) return@withContext false

        // Wait for TTS to be ready
        var waited = 0
        while (!ttsReady && waited < 3000) {
            delay(100)
            waited += 100
        }

        if (!ttsReady) {
            return@withContext false
        }

        try {
            isSpeaking = true

            // Set language
            val locale = getLocaleForLanguage(languageCode)
            val result = androidTTS?.setLanguage(locale)

            if (result == AndroidTTS.LANG_MISSING_DATA || result == AndroidTTS.LANG_NOT_SUPPORTED) {
                // Language not available, use default
                androidTTS?.setLanguage(Locale.ENGLISH)
            }

            // Set listener
            androidTTS?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                }

                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                }
            })

            // Speak
            val params = android.os.Bundle()
            params.putString(AndroidTTS.Engine.KEY_PARAM_UTTERANCE_ID, "tts_$languageCode")

            androidTTS?.speak(text, AndroidTTS.QUEUE_FLUSH, params, "tts_$languageCode")

            true
        } catch (e: Exception) {
            e.printStackTrace()
            isSpeaking = false
            false
        }
    }

    private fun getLocaleForLanguage(languageCode: String): Locale {
        return when (languageCode) {
            "en" -> Locale.ENGLISH
            "es" -> Locale("es", "ES")
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "it" -> Locale.ITALIAN
            "pt" -> Locale("pt", "PT")
            "ru" -> Locale("ru", "RU")
            "zh" -> Locale.CHINESE
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "ar" -> Locale("ar", "SA")
            "hi" -> Locale("hi", "IN")
            "nl" -> Locale("nl", "NL")
            "pl" -> Locale("pl", "PL")
            "tr" -> Locale("tr", "TR")
            "vi" -> Locale("vi", "VN")
            "th" -> Locale("th", "TH")
            "id" -> Locale("id", "ID")
            "uk" -> Locale("uk", "UA")
            "ro" -> Locale("ro", "RO")
            else -> Locale.ENGLISH
        }
    }

    fun stop() {
        androidTTS?.stop()
        isSpeaking = false
    }

    fun isSpeaking(): Boolean = isSpeaking

    fun getSupportedLanguages(): List<String> {
        return listOf(
            "en", "es", "fr", "de", "it", "pt", "ru", "zh", "ja", "ko",
            "ar", "hi", "nl", "pl", "tr", "vi", "th", "id", "uk", "ro"
        )
    }

    fun isLanguageSupported(languageCode: String): Boolean {
        return getSupportedLanguages().contains(languageCode)
    }

    fun shutdown() {
        stop()
        androidTTS?.shutdown()
        androidTTS = null
    }
}
