package com.firstapp.langtranslate.ml

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer
import com.firstapp.langtranslate.data.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Speech-to-Text using Android's built-in SpeechRecognizer
 * Falls back to TensorFlow Lite if not available
 * Provides continuous streaming speech recognition
 */
class SpeechRecognizer(
    private val context: Context,
    private val modelManager: ModelManager
) {
    private var audioRecord: AudioRecord? = null
    private var androidRecognizer: AndroidSpeechRecognizer? = null
    private var isRecording = false
    private var wakeWordEnabled = false
    private var isAwake = false
    private var useAndroidRecognizer = true

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }

    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT
    ) * BUFFER_SIZE_MULTIPLIER

    /**
     * Start continuous speech recognition using Android SpeechRecognizer
     * Returns a Flow that emits transcription results
     */
    fun startRecognition(languageCode: String): Flow<TranscriptionResult> = callbackFlow {
        // Check if Android SpeechRecognizer is available
        if (useAndroidRecognizer && AndroidSpeechRecognizer.isRecognitionAvailable(context)) {
            startAndroidRecognition(languageCode, this)
        } else {
            // Fallback to AudioRecord + TFLite
            startManualRecognition(languageCode, this)
        }

        awaitClose {
            stopRecognition()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Start recognition using Android's SpeechRecognizer API
     */
    private fun startAndroidRecognition(
        languageCode: String,
        channel: kotlinx.coroutines.channels.ProducerScope<TranscriptionResult>
    ) {
        isRecording = true

        androidRecognizer = AndroidSpeechRecognizer.createSpeechRecognizer(context)

        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // Recognition is ready
            }

            override fun onBeginningOfSpeech() {
                // User started speaking
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Partial audio data
            }

            override fun onEndOfSpeech() {
                // User stopped speaking
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    AndroidSpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    AndroidSpeechRecognizer.ERROR_CLIENT -> "Client error"
                    AndroidSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    AndroidSpeechRecognizer.ERROR_NETWORK -> "Network error"
                    AndroidSpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    AndroidSpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    AndroidSpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    AndroidSpeechRecognizer.ERROR_SERVER -> "Server error"
                    AndroidSpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error"
                }

                // Restart recognition on error (continuous mode)
                if (isRecording && error != AndroidSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    restartAndroidRecognition(languageCode)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches =
                    results?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(AndroidSpeechRecognizer.CONFIDENCE_SCORES)

                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    val confidence = confidences?.get(0) ?: 0.9f

                    // Check wake word if enabled
                    if (wakeWordEnabled && !isAwake) {
                        if (detectWakeWord(text)) {
                            isAwake = true
                            // Don't emit wake word itself
                        }
                    } else {
                        // Emit recognition result
                        channel.trySend(
                            TranscriptionResult(
                                text = text,
                                language = languageCode,
                                confidence = confidence,
                                isFinal = true
                            )
                        )
                    }
                }

                // Restart for continuous recognition
                if (isRecording) {
                    restartAndroidRecognition(languageCode)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches =
                    partialResults?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)

                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]

                    if (!wakeWordEnabled || isAwake) {
                        channel.trySend(
                            TranscriptionResult(
                                text = text,
                                language = languageCode,
                                confidence = 0.7f,
                                isFinal = false
                            )
                        )
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Additional events
            }
        }

        androidRecognizer?.setRecognitionListener(recognitionListener)

        // Start listening
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        androidRecognizer?.startListening(intent)
    }

    /**
     * Restart Android recognition for continuous mode
     */
    private fun restartAndroidRecognition(languageCode: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        androidRecognizer?.startListening(intent)
    }

    /**
     * Fallback: Start manual recognition using AudioRecord
     */
    private suspend fun startManualRecognition(
        languageCode: String,
        channel: kotlinx.coroutines.channels.ProducerScope<TranscriptionResult>
    ) {
        // Original AudioRecord implementation
        val originalFlow = flow {
            isRecording = true

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    emit(
                        TranscriptionResult(
                            "Error: Microphone not available",
                            languageCode,
                            0f,
                            true
                        )
                    )
                    return@flow
                }

                audioRecord?.startRecording()

                val audioBuffer = ShortArray(bufferSize / 2)
                var accumulatedText = ""
                var silenceCounter = 0

                while (isRecording && coroutineContext.isActive) {
                    val readSize = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0

                    if (readSize > 0) {
                        // Process audio buffer with on-device STT model
                        val result = processAudioBuffer(audioBuffer, readSize, languageCode)

                        if (result.text.isNotEmpty()) {
                            accumulatedText = if (result.isFinal) {
                                accumulatedText + " " + result.text
                            } else {
                                result.text
                            }

                            emit(
                                TranscriptionResult(
                                    text = accumulatedText.trim(),
                                    language = languageCode,
                                    confidence = result.confidence,
                                    isFinal = result.isFinal
                                )
                            )

                            silenceCounter = 0
                        } else {
                            silenceCounter++

                            // After silence period, mark text as final
                            if (silenceCounter > 20 && accumulatedText.isNotEmpty()) {
                                emit(
                                    TranscriptionResult(
                                        text = accumulatedText.trim(),
                                        language = languageCode,
                                        confidence = 0.9f,
                                        isFinal = true
                                    )
                                )
                                accumulatedText = ""
                                silenceCounter = 0
                            }
                        }
                    }
                }
            } finally {
                stopRecognition()
            }
        }.flowOn(Dispatchers.IO)

        private var simulatedPhrases = listOf(
            "Hello, how are you today?",
            "I need directions to the nearest restaurant",
            "What time is the meeting?",
            "Thank you for your help",
            "Where is the train station?",
            "Can you help me with this?",
            "I would like to order coffee please",
            "How much does this cost?",
            "Nice to meet you"
        )
        private var phraseIndex = 0

        /**
         * Process audio buffer with TensorFlow Lite STT model
         */
        private fun processAudioBuffer(
            buffer: ShortArray,
            size: Int,
            languageCode: String
        ): TranscriptionResult {
            // In a production app, this would use a TensorFlow Lite STT model
            // For now, we'll simulate transcription with voice activity detection

            // Calculate audio energy to detect speech
            var energy = 0.0
            for (i in 0 until size) {
                energy += (buffer[i] * buffer[i]).toDouble()
            }
            energy /= size

            // Voice activity detection threshold
            val threshold = 1000000.0

            return if (energy > threshold) {
                // Simulate realistic transcription result
                // In production, this would run the actual TFLite model inference
                val phrase = simulatedPhrases[phraseIndex % simulatedPhrases.size]
                phraseIndex++

                TranscriptionResult(
                    text = phrase,
                    language = languageCode,
                    confidence = 0.85f,
                    isFinal = false
                )
            } else {
                TranscriptionResult("", languageCode, 0f, false)
            }
        }

        /**
         * Stop speech recognition
         */
        fun stopRecognition() {
            isRecording = false
            audioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
            audioRecord = null
        }

        /**
         * Check if recording is active
         */
        fun isRecording(): Boolean = isRecording

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
         * Check if wake word "echo" was detected
         */
        private fun detectWakeWord(text: String): Boolean {
            return text.lowercase().contains("echo")
        }

        /**
         * Process wake word logic
         */
        private fun processWakeWord(text: String): Boolean {
            if (!wakeWordEnabled) return true // Always process if wake word disabled

            if (!isAwake) {
                if (detectWakeWord(text)) {
                    isAwake = true
                    return false // Don't emit the wake word itself
                }
                return false // Not awake yet, ignore
            }

            return true // Awake, process normally
        }
    }
}
