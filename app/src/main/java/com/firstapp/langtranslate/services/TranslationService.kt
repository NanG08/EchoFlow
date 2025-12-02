package com.firstapp.langtranslate.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.firstapp.langtranslate.EchoFlowApp
import com.firstapp.langtranslate.R
import com.firstapp.langtranslate.data.TranslationMode
import com.firstapp.langtranslate.data.TranslationResult
import com.firstapp.langtranslate.data.TranscriptionResult
import com.firstapp.langtranslate.ml.AndroidSpeechRecognizer
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

class TranslationService : Service() {

    private val binder = TranslationBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var speechRecognizer: AndroidSpeechRecognizer
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

        val app = application as EchoFlowApp
        val modelManager = app.modelManager

        speechRecognizer = AndroidSpeechRecognizer(this)
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

    fun startVoiceTranslation(srcLang: String, tgtLang: String) {
        sourceLanguage = srcLang
        targetLanguage = tgtLang
        currentMode = TranslationMode.VOICE
        isRunning = true

        speechRecognizer.setWakeWordEnabled(wakeWordEnabled)

        currentJob?.cancel()
        currentJob = serviceScope.launch {
            speechRecognizer.startRecognition(sourceLanguage).collect { transcription ->
                if (transcription.text.isNotBlank()) {
                    _transcriptionFlow.emit(transcription)
                }

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

                    database.saveTranslation(result)
                    _translationFlow.emit(result)

                    if (autoPlayTTS && result.translatedText.isNotBlank()) {
                        textToSpeech.speak(result.translatedText, targetLanguage)
                    }
                }
            }
        }
    }

    suspend fun translateText(text: String, srcLang: String, tgtLang: String): TranslationResult {
        println("TranslationService: Translating '$text' from $srcLang to $tgtLang")

        val result = translationEngine.translate(text, srcLang, tgtLang, TranslationMode.VOICE)

        println("Translation result: '${result.translatedText}' (confidence: ${result.confidence})")

        database.saveTranslation(result)
        return result
    }

    fun stopTranslation() {
        isRunning = false
        currentJob?.cancel()
        speechRecognizer.stopRecognition()
        textToSpeech.stop()
    }

    fun setAutoDetect(enabled: Boolean) {
        autoDetect = enabled
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        wakeWordEnabled = enabled
        speechRecognizer.setWakeWordEnabled(enabled)
    }

    fun setAutoPlayTTS(enabled: Boolean) {
        autoPlayTTS = enabled
    }

    fun isRunning(): Boolean = isRunning

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

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EchoFlow")
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
