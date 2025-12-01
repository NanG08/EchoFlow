package com.firstapp.langtranslate.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.firstapp.langtranslate.LangTranslateApp
import com.firstapp.langtranslate.R
import com.firstapp.langtranslate.data.TranslationMode
import com.firstapp.langtranslate.data.TranslationResult
import com.firstapp.langtranslate.data.TranscriptionResult
import com.firstapp.langtranslate.ml.SpeechRecognizer
import com.firstapp.langtranslate.ml.TextToSpeech
import com.firstapp.langtranslate.ml.TranslationEngine
import com.firstapp.langtranslate.storage.TranslationDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Foreground service for continuous translation
 * Handles voice recognition, translation, and TTS
 */
class TranslationService : Service() {

    private val binder = TranslationBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var translationEngine: TranslationEngine
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var database: TranslationDatabase

    private val _translationFlow = MutableSharedFlow<TranslationResult>()
    val translationFlow: Flow<TranslationResult> = _translationFlow.asSharedFlow()

    private val _transcriptionFlow = MutableSharedFlow<TranscriptionResult>()
    val transcriptionFlow: Flow<TranscriptionResult> = _transcriptionFlow.asSharedFlow()

    private var currentJob: Job? = null
    private var isRunning = false

    private var sourceLanguage = "en"
    private var targetLanguage = "es"
    private var autoDetect = true
    private var currentMode = TranslationMode.VOICE
    private var autoPlayTTS = true
    private var wakeWordEnabled = false

    companion object {
        private const val CHANNEL_ID = "translation_service_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()

        val app = application as LangTranslateApp
        val modelManager = app.modelManager

        speechRecognizer = SpeechRecognizer(this, modelManager)
        translationEngine = TranslationEngine(this, modelManager)
        textToSpeech = TextToSpeech(this, modelManager)
        database = TranslationDatabase(this)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class TranslationBinder : Binder() {
        fun getService(): TranslationService = this@TranslationService
    }

    /**
     * Start real-time voice translation
     */
    fun startVoiceTranslation(srcLang: String, tgtLang: String) {
        sourceLanguage = srcLang
        targetLanguage = tgtLang
        currentMode = TranslationMode.VOICE
        isRunning = true

        // Set wake word mode on speech recognizer
        speechRecognizer.setWakeWordEnabled(wakeWordEnabled)

        currentJob?.cancel()
        currentJob = serviceScope.launch {
            speechRecognizer.startRecognition(sourceLanguage).collect { transcription ->
                // Emit transcription (only real text, no placeholders)
                if (transcription.text.isNotBlank()) {
                    _transcriptionFlow.emit(transcription)
                }

                // Translate when final
                if (transcription.isFinal && transcription.text.isNotBlank()) {
                    val detected = if (autoDetect) {
                        translationEngine.detectLanguage(transcription.text)
                    } else {
                        sourceLanguage
                    }

                    val result = translationEngine.translate(
                        text = transcription.text,
                        sourceLanguage = detected,
                        targetLanguage = targetLanguage,
                        mode = currentMode
                    )

                    // Save to database
                    database.saveTranslation(result)

                    // Emit translation
                    _translationFlow.emit(result)

                    // Auto-play TTS if enabled
                    if (autoPlayTTS && result.translatedText.isNotBlank()) {
                        textToSpeech.speak(result.translatedText, targetLanguage)
                    }
                }
            }
        }
    }

    /**
     * Start conversation mode (bidirectional)
     */
    fun startConversationMode(lang1: String, lang2: String) {
        currentMode = TranslationMode.CONVERSATION
        isRunning = true

        currentJob?.cancel()
        currentJob = serviceScope.launch {
            var currentSourceLang = lang1
            var currentTargetLang = lang2

            speechRecognizer.startRecognition(currentSourceLang).collect { transcription ->
                _transcriptionFlow.emit(transcription)

                if (transcription.isFinal && transcription.text.isNotBlank()) {
                    // Auto-detect language
                    val detectedLang = translationEngine.detectLanguage(transcription.text)

                    // Determine target language
                    val targetLang = if (detectedLang == lang1) lang2 else lang1

                    val result = translationEngine.translate(
                        text = transcription.text,
                        sourceLanguage = detectedLang,
                        targetLanguage = targetLang,
                        mode = currentMode
                    )

                    database.saveTranslation(result)
                    _translationFlow.emit(result)
                    textToSpeech.speak(result.translatedText, targetLang)

                    // Switch languages for next input
                    currentSourceLang = targetLang
                    currentTargetLang = detectedLang
                }
            }
        }
    }

    /**
     * Translate text directly
     */
    suspend fun translateText(text: String, srcLang: String, tgtLang: String): TranslationResult {
        val result = translationEngine.translate(text, srcLang, tgtLang, TranslationMode.VOICE)
        database.saveTranslation(result)
        return result
    }

    /**
     * Stop translation
     */
    fun stopTranslation() {
        isRunning = false
        currentJob?.cancel()
        speechRecognizer.stopRecognition()
        textToSpeech.stop()
    }

    /**
     * Set auto language detection
     */
    fun setAutoDetect(enabled: Boolean) {
        autoDetect = enabled
    }

    /**
     * Set wake word detection
     */
    fun setWakeWordEnabled(enabled: Boolean) {
        wakeWordEnabled = enabled
        speechRecognizer.setWakeWordEnabled(enabled)
    }

    /**
     * Set auto-play TTS
     */
    fun setAutoPlayTTS(enabled: Boolean) {
        autoPlayTTS = enabled
    }

    /**
     * Check if service is running
     */
    fun isRunning(): Boolean = isRunning

    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Translation Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Real-time translation service"
                setShowBadge(false)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Create notification
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LangTranslate")
            .setContentText("Translation service running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTranslation()
        serviceScope.cancel()
    }
}
