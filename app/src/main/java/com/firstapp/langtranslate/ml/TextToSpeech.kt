package com.firstapp.langtranslate.ml

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech as AndroidTTS
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Hybrid Text-to-Speech implementation
 * PRIMARY: Uses Android TTS (works immediately!)
 * FALLBACK: TFLite models (when available)
 */
class TextToSpeech(
    private val context: Context,
    private val modelManager: ModelManager
) {

    private var audioTrack: AudioTrack? = null
    private var isSpeaking = false
    private var ttsInterpreters = mutableMapOf<String, Interpreter>()
    private var vocoderInterpreter: Interpreter? = null

    // Android TTS - Works out of the box!
    private var androidTTS: AndroidTTS? = null
    private var ttsReady = false
    private var useTFLite = false // Toggle between Android TTS and TFLite

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    init {
        // Initialize Android TTS
        androidTTS = AndroidTTS(context) { status ->
            if (status == AndroidTTS.SUCCESS) {
                ttsReady = true
                println("✅ Android TTS initialized successfully!")
            } else {
                println("❌ Android TTS initialization failed")
            }
        }
    }

    /**
     * Speak text using Android TTS (PRIMARY) or TFLite (FALLBACK)
     */
    suspend fun speak(text: String, languageCode: String): Boolean {
        if (text.isBlank()) return false

        return try {
            isSpeaking = true

            // Check if we should use TFLite
            val modelPath = modelManager.getModelPath("tts_$languageCode")
            val modelFile = File(modelPath)
            val hasTFLiteModel = modelFile.exists()

            if (useTFLite && hasTFLiteModel) {
                // Use TFLite if explicitly enabled and model available
                speakWithTFLite(text, languageCode)
            } else {
                // Use Android TTS (DEFAULT - always works!)
                speakWithAndroidTTS(text, languageCode)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            isSpeaking = false
        }
    }

    /**
     * Speak using Android's built-in TTS (WORKS IMMEDIATELY!)
     */
    private suspend fun speakWithAndroidTTS(text: String, languageCode: String): Boolean =
        suspendCoroutine { continuation ->
            if (!ttsReady) {
                // Wait a bit for TTS to initialize
                Thread.sleep(500)
            }

            val locale = when (languageCode) {
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
                else -> Locale.ENGLISH
            }

            androidTTS?.language = locale

            // Set listener to know when done
            androidTTS?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    println("🔊 Speaking: $text")
                }

                override fun onDone(utteranceId: String?) {
                    println("✅ Speech completed")
                    continuation.resume(true)
                }

                override fun onError(utteranceId: String?) {
                    println("❌ Speech error")
                    continuation.resume(false)
                }
            })

            // Speak the text
            val result =
                androidTTS?.speak(text, AndroidTTS.QUEUE_FLUSH, null, "tts_utterance_$text")

            if (result != AndroidTTS.SUCCESS) {
                println("❌ TTS speak() failed")
                continuation.resume(false)
            }
        }

    /**
     * Speak using TFLite TTS models
     */
    private suspend fun speakWithTFLite(text: String, languageCode: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Load TFLite TTS model
                val interpreter = loadTTSModel(languageCode)

                // Generate audio with TTS model
                val audioData = synthesizeSpeechWithTFLite(text, languageCode, interpreter)

                // Play audio
                playAudio(audioData)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    /**
     * Load TFLite TTS model for language
     */
    private fun loadTTSModel(languageCode: String): Interpreter {
        // Check if already loaded
        if (ttsInterpreters.containsKey(languageCode)) {
            return ttsInterpreters[languageCode]!!
        }

        // Load TFLite model
        val modelPath = modelManager.getModelPath("tts_$languageCode")
        val model = File(modelPath)

        val options = Interpreter.Options().apply {
            setNumThreads(4)
            setUseNNAPI(false)
        }

        val interpreter = Interpreter(model, options)
        ttsInterpreters[languageCode] = interpreter

        return interpreter
    }

    /**
     * Synthesize speech using TFLite TTS model
     */
    private fun synthesizeSpeechWithTFLite(
        text: String,
        languageCode: String,
        interpreter: Interpreter
    ): ShortArray {
        // TODO: Implement actual TFLite inference
        // This requires:
        // 1. Text-to-phoneme conversion
        // 2. TTS model inference (Tacotron/FastSpeech)
        // 3. Vocoder inference (WaveGlow/MelGAN)

        // For now, return silence
        val durationSeconds = text.length / 10.0
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        return ShortArray(numSamples) { 0 }
    }

    /**
     * Play audio data
     */
    private fun playAudio(audioData: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.apply {
            play()
            write(audioData, 0, audioData.size)
            stop()
            release()
        }
        audioTrack = null
    }

    /**
     * Stop current speech playback
     */
    fun stop() {
        isSpeaking = false
        androidTTS?.stop()
        audioTrack?.apply {
            if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                stop()
            }
            release()
        }
        audioTrack = null
    }

    /**
     * Check if currently speaking
     */
    fun isSpeaking(): Boolean = isSpeaking

    /**
     * Toggle between Android TTS and TFLite
     */
    fun setUseTFLite(use: Boolean) {
        useTFLite = use
    }

    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<String> {
        return listOf("en", "es", "fr", "de", "it", "pt", "ru", "zh", "ja", "ko")
    }

    /**
     * Check if language is supported
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return getSupportedLanguages().contains(languageCode)
    }

    /**
     * Clean up resources
     */
    fun shutdown() {
        stop()
        androidTTS?.shutdown()
        androidTTS = null
        ttsInterpreters.values.forEach { it.close() }
        ttsInterpreters.clear()
        vocoderInterpreter?.close()
        vocoderInterpreter = null
    }
}
