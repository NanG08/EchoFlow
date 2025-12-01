package com.firstapp.langtranslate.ml

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.firstapp.langtranslate.data.TranscriptionResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

/**
 * Speech recognition using Android's built-in SpeechRecognizer
 * Works immediately without any models!
 */
class AndroidSpeechRecognizer(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizing = false
    private var wakeWordEnabled = false
    private var isAwake = false

    /**
     * Check if speech recognition is available
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Start continuous speech recognition
     */
    fun startRecognition(languageCode: String): Flow<TranscriptionResult> = callbackFlow {
        isRecognizing = true

        if (!isAvailable()) {
            trySend(
                TranscriptionResult(
                    text = "Speech recognition not available on this device",
                    language = languageCode,
                    confidence = 0f,
                    isFinal = true
                )
            )
            close()
            return@callbackFlow
        }

        // Create speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLocaleCode(languageCode))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                println("🎤 Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                println("🗣️ Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed - can use for visualization
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }

            override fun onEndOfSpeech() {
                println("🔇 Speech ended")
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error: $error"
                }
                println("❌ Recognition error: $errorMessage")

                // Restart recognition if still active
                if (isRecognizing && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    speechRecognizer?.startListening(recognizerIntent)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    val confidence = scores?.getOrNull(0) ?: 0.9f

                    println("✅ Recognized: $text")

                    // Check wake word if enabled
                    if (wakeWordEnabled && !isAwake) {
                        if (detectWakeWord(text)) {
                            isAwake = true
                            println("🔊 Wake word detected!")
                        }
                    } else {
                        // Emit result
                        trySend(
                            TranscriptionResult(
                                text = text,
                                language = languageCode,
                                confidence = confidence,
                                isFinal = true
                            )
                        )
                    }
                }

                // Restart recognition for continuous listening
                if (isRecognizing) {
                    speechRecognizer?.startListening(recognizerIntent)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    println("📝 Partial: $text")

                    // Emit partial result
                    trySend(
                        TranscriptionResult(
                            text = text,
                            language = languageCode,
                            confidence = 0.5f,
                            isFinal = false
                        )
                    )
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Event received
            }
        })

        // Start listening
        speechRecognizer?.startListening(recognizerIntent)

        awaitClose {
            stopRecognition()
        }
    }

    /**
     * Stop speech recognition
     */
    fun stopRecognition() {
        isRecognizing = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    /**
     * Check if recording is active
     */
    fun isRecording(): Boolean = isRecognizing

    /**
     * Enable/disable wake word detection
     */
    fun setWakeWordEnabled(enabled: Boolean) {
        wakeWordEnabled = enabled
        if (!enabled) {
            isAwake = true // If wake word disabled, always "awake"
        }
    }

    /**
     * Check if wake word "ECHO" was detected
     * Case-insensitive matching
     */
    private fun detectWakeWord(text: String): Boolean {
        val normalizedText = text.lowercase().trim()
        // Check for exact match or word boundary match
        return normalizedText == "echo" ||
                normalizedText.contains(" echo ") ||
                normalizedText.startsWith("echo ") ||
                normalizedText.endsWith(" echo")
    }

    /**
     * Get locale code for language
     */
    private fun getLocaleCode(languageCode: String): String {
        return when (languageCode) {
            "en" -> Locale.ENGLISH.toString()
            "es" -> Locale("es", "ES").toString()
            "fr" -> Locale.FRENCH.toString()
            "de" -> Locale.GERMAN.toString()
            "it" -> Locale.ITALIAN.toString()
            "pt" -> Locale("pt", "PT").toString()
            "ru" -> Locale("ru", "RU").toString()
            "zh" -> Locale.CHINESE.toString()
            "ja" -> Locale.JAPANESE.toString()
            "ko" -> Locale.KOREAN.toString()
            else -> Locale.ENGLISH.toString()
        }
    }
}
