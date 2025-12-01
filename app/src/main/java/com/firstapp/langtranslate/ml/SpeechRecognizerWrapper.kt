package com.firstapp.langtranslate.ml

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer
import com.firstapp.langtranslate.data.TranscriptionResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Wrapper for Android's built-in SpeechRecognizer
 * Provides real continuous speech recognition
 */
class SpeechRecognizerWrapper(private val context: Context) {

    private var androidRecognizer: AndroidSpeechRecognizer? = null
    private var isRecording = false
    private var wakeWordEnabled = false
    private var isAwake = false

    /**
     * Start continuous speech recognition using Android SpeechRecognizer
     */
    fun startRecognition(languageCode: String): Flow<TranscriptionResult> = callbackFlow {
        if (!AndroidSpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(TranscriptionResult("Speech recognition not available", languageCode, 0f, true))
            close()
            return@callbackFlow
        }

        isRecording = true
        androidRecognizer = AndroidSpeechRecognizer.createSpeechRecognizer(context)

        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // Restart on most errors for continuous recognition
                if (isRecording && error != AndroidSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    restartRecognition(languageCode)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches =
                    results?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(AndroidSpeechRecognizer.CONFIDENCE_SCORES)

                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    val confidence = confidences?.get(0) ?: 0.9f

                    // Check wake word
                    if (wakeWordEnabled && !isAwake) {
                        if (text.lowercase().contains("echo")) {
                            isAwake = true
                            // Don't emit wake word
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

                // Continue listening
                if (isRecording) {
                    restartRecognition(languageCode)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches =
                    partialResults?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)

                if (!matches.isNullOrEmpty() && (!wakeWordEnabled || isAwake)) {
                    trySend(
                        TranscriptionResult(
                            text = matches[0],
                            language = languageCode,
                            confidence = 0.7f,
                            isFinal = false
                        )
                    )
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}

            private fun restartRecognition(lang: String) {
                val intent = createRecognitionIntent(lang)
                androidRecognizer?.startListening(intent)
            }
        }

        androidRecognizer?.setRecognitionListener(recognitionListener)

        // Start listening
        val intent = createRecognitionIntent(languageCode)
        androidRecognizer?.startListening(intent)

        awaitClose {
            stopRecognition()
        }
    }

    private fun createRecognitionIntent(languageCode: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        wakeWordEnabled = enabled
        if (!enabled) {
            isAwake = true
        }
    }

    fun stopRecognition() {
        isRecording = false
        androidRecognizer?.stopListening()
        androidRecognizer?.destroy()
        androidRecognizer = null
        isAwake = false
    }

    fun isRecording(): Boolean = isRecording
}
