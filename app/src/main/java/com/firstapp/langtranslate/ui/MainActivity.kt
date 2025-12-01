package com.firstapp.langtranslate.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat.performHapticFeedback

import androidx.lifecycle.lifecycleScope
import com.firstapp.langtranslate.EchoFlowApp
import com.firstapp.langtranslate.R
import com.firstapp.langtranslate.data.TranslationMode
import com.firstapp.langtranslate.databinding.ActivityMainBinding
import com.firstapp.langtranslate.ml.OCREngine
import com.firstapp.langtranslate.services.TranslationService
import com.firstapp.langtranslate.storage.TranslationDatabase
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var translationService: TranslationService? = null
    private var isBound = false

    private lateinit var ocrEngine: OCREngine
    private lateinit var database: TranslationDatabase
    private lateinit var vibrator: Vibrator

    private var currentMode = TranslationMode.VOICE
    private var sourceLanguage = "en"
    private var targetLanguage = "es"
    private var isTranslating = false
    private var hapticFeedbackEnabled = true
    private var wakeWordEnabled = false

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TranslationService.TranslationBinder
            translationService = binder.getService()
            isBound = true
            observeTranslations()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            translationService = null
            isBound = false
        }
    }

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                processImageForOCR(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as EchoFlowApp
        ocrEngine = OCREngine(this, app.modelManager)
        database = TranslationDatabase(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        setupUI()
        checkPermissions()
        loadSettings()
        animateEntrance()

        // Bind translation service on startup
        bindTranslationService()
    }

    private fun setupUI() {
        // Voice mode is always active (home screen)
        // Mode buttons switch between additional features

        // Voice/Text toggle buttons
        binding.chipVoice.setOnClickListener {
            showVoiceInput()
            performHapticFeedback()

            // Update button states
            binding.chipVoice.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.primary)
            binding.chipVoice.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.chipText.backgroundTintList = null
            binding.chipText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }

        binding.chipText.setOnClickListener {
            showTextInput()
            performHapticFeedback()

            // Update button states
            binding.chipText.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.primary)
            binding.chipText.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.chipVoice.backgroundTintList = null
            binding.chipVoice.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }

        // Text input character counter
        binding.etTextInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvCharCount.text = "${s?.length ?: 0} / 500"
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Mode selection buttons for additional features
        binding.btnCameraMode.setOnClickListener {
            performHapticFeedback()
            switchMode(TranslationMode.LIVE_CAMERA)
        }

        binding.btnPhotoMode.setOnClickListener {
            performHapticFeedback()
            switchMode(TranslationMode.PHOTO)
        }

        binding.btnSignLanguageMode.setOnClickListener {
            performHapticFeedback()
            switchMode(TranslationMode.SIGN_LANGUAGE)
        }

        // Translation control
        binding.btnStartStop.setOnClickListener {
            if (isTranslating) {
                stopTranslation()
            } else {
                startTranslation()
            }
        }

        // Language selection
        binding.btnSourceLanguage.setOnClickListener {
            showLanguageSelector(true)
        }

        binding.btnTargetLanguage.setOnClickListener {
            showLanguageSelector(false)
        }

        binding.btnSwapLanguages.setOnClickListener {
            swapLanguages()
        }

        // Photo selection for OCR
        binding.btnSelectPhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        // History
        binding.btnHistory.setOnClickListener {
            showHistory()
        }

        // Settings
        binding.btnSettings.setOnClickListener {
            showSettings()
        }

        // Wake word toggle
        binding.switchWakeWord.setOnCheckedChangeListener { _, isChecked ->
            wakeWordEnabled = isChecked
            translationService?.setWakeWordEnabled(isChecked)
            performHapticFeedback()

            val message = if (isChecked) {
                getString(R.string.msg_wake_word_enabled)
            } else {
                getString(R.string.msg_wake_word_disabled)
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        updateLanguageDisplay()
    }

    private fun checkPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(
                    this,
                    "Permissions required for translation features",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showVoiceInput() {
        // Show voice display, hide text input
        binding.tvOriginalText.visibility = View.VISIBLE
        binding.layoutTextInput.visibility = View.GONE
        binding.tvCharCount.visibility = View.GONE

        // Update button text and behavior
        binding.btnStartStop.text = getString(R.string.action_start)
        binding.btnStartStop.icon = ContextCompat.getDrawable(this, R.drawable.ic_mic)
    }

    private fun showTextInput() {
        // Show text input, hide voice display
        binding.tvOriginalText.visibility = View.GONE
        binding.layoutTextInput.visibility = View.VISIBLE
        binding.tvCharCount.visibility = View.VISIBLE

        // Update button text and behavior
        binding.btnStartStop.text = "Translate"
        binding.btnStartStop.icon = null

        // Focus on text input
        binding.etTextInput.requestFocus()
    }

    private fun switchMode(mode: TranslationMode) {
        currentMode = mode
        updateModeUI()

        // Hide all containers first
        binding.cameraPreview.visibility = View.GONE
        binding.layoutPhoto.visibility = View.GONE

        // Show/hide mode-specific UI
        when (mode) {
            TranslationMode.LIVE_CAMERA -> {
                binding.cameraPreview.visibility = View.VISIBLE
                startCameraTranslation()
            }

            TranslationMode.PHOTO -> {
                binding.layoutPhoto.visibility = View.VISIBLE
            }

            TranslationMode.SIGN_LANGUAGE -> {
                // Show ASL camera fragment
                val aslFragment = ASLFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.cameraPreview, aslFragment)
                    .commit()
                binding.cameraPreview.visibility = View.VISIBLE
            }

            TranslationMode.VOICE, TranslationMode.TEXT_ENTRY -> {
                // Voice/Text is the home screen, just hide other modes
                // Already handled by hiding camera/photo above
            }
        }
    }

    private fun updateModeUI() {
        // Reset all button states (only mode buttons, not voice/text)
        binding.btnCameraMode.isSelected = false
        binding.btnPhotoMode.isSelected = false
        binding.btnSignLanguageMode.isSelected = false

        // Highlight selected mode
        when (currentMode) {
            TranslationMode.LIVE_CAMERA -> binding.btnCameraMode.isSelected = true
            TranslationMode.PHOTO -> binding.btnPhotoMode.isSelected = true
            TranslationMode.SIGN_LANGUAGE -> binding.btnSignLanguageMode.isSelected = true
            TranslationMode.VOICE, TranslationMode.TEXT_ENTRY -> {
                // Voice is always active, no button to highlight
            }
        }
    }

    private fun startTranslation() {
        // Check if we're in text mode (check if text input is visible)
        if (binding.layoutTextInput.visibility == View.VISIBLE) {
            // Text mode - translate the typed text
            val textToTranslate = binding.etTextInput.text.toString().trim()
            if (textToTranslate.isEmpty()) {
                Toast.makeText(this, "Please enter text to translate", Toast.LENGTH_SHORT).show()
                return
            }

            // Show the original text
            binding.tvOriginalText.text = textToTranslate
            binding.tvOriginalText.visibility = View.VISIBLE

            // Ensure service is bound
            if (!isBound) {
                bindTranslationService()
                // Wait a moment for service to bind, then try translation
                binding.btnStartStop.postDelayed({
                    performTextTranslation(textToTranslate)
                }, 500)
            } else {
                performTextTranslation(textToTranslate)
            }
            return
        }

        // Voice mode
        if (!isBound) {
            bindTranslationService()
        }

        isTranslating = true
        binding.btnStartStop.text = getString(R.string.action_stop)
        binding.btnStartStop.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.error)
        binding.btnStartStop.icon =
            ContextCompat.getDrawable(this, R.drawable.ic_stop)

        when (currentMode) {
            TranslationMode.VOICE -> {
                translationService?.startVoiceTranslation(sourceLanguage, targetLanguage)
            }

            TranslationMode.LIVE_CAMERA -> {
                startCameraTranslation()
            }

            TranslationMode.SIGN_LANGUAGE, TranslationMode.TEXT_ENTRY, TranslationMode.PHOTO -> {
                // These modes don't need the translation service
                // They handle translation internally or on-demand
            }
        }
    }

    private fun performTextTranslation(text: String) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                if (translationService == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Translation service not ready. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }

                val translation = translationService?.translateText(
                    text,
                    sourceLanguage,
                    targetLanguage
                )

                if (translation != null) {
                    binding.tvTranslatedText.text = translation.translatedText
                    binding.tvConfidence.text =
                        "Confidence: ${(translation.confidence * 100).toInt()}%"
                    binding.tvConfidence.visibility = View.VISIBLE
                    animateTextUpdate(binding.tvTranslatedText)
                } else {
                    binding.tvTranslatedText.text = "Translation not available"
                    Toast.makeText(
                        this@MainActivity,
                        "Translation returned null. Check if translation engine is working.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Translation error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.tvTranslatedText.text = "Error: ${e.message}"
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun stopTranslation() {
        isTranslating = false
        binding.btnStartStop.text = getString(R.string.action_start)
        binding.btnStartStop.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.primary)
        binding.btnStartStop.icon =
            ContextCompat.getDrawable(this, R.drawable.ic_mic)

        translationService?.stopTranslation()
    }

    private fun bindTranslationService() {
        val intent = Intent(this, TranslationService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeTranslations() {
        lifecycleScope.launch {
            translationService?.transcriptionFlow?.collect { transcription ->
                binding.tvOriginalText.text = transcription.text
                animateTextUpdate(binding.tvOriginalText)
                performHapticFeedback()
            }
        }

        lifecycleScope.launch {
            translationService?.translationFlow?.collect { result ->
                binding.tvTranslatedText.text = result.translatedText
                animateTextUpdate(binding.tvTranslatedText)
                performHapticFeedback()

                if (binding.tvConfidence.visibility == View.VISIBLE) {
                    binding.tvConfidence.text = "Confidence: ${(result.confidence * 100).toInt()}%"
                }
            }
        }
    }

    private fun startCameraTranslation() {
        // Initialize camera for real-time OCR
        val cameraFragment = CameraFragment.newInstance(sourceLanguage, targetLanguage)
        supportFragmentManager.beginTransaction()
            .replace(R.id.cameraPreview, cameraFragment)
            .commit()
    }

    private fun processImageForOCR(bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val ocrResult = ocrEngine.recognizeText(bitmap, sourceLanguage)

                if (ocrResult.text.isNotEmpty()) {
                    binding.tvOriginalText.text = ocrResult.text

                    // Translate the OCR result
                    val translation = translationService?.translateText(
                        ocrResult.text,
                        sourceLanguage,
                        targetLanguage
                    )

                    translation?.let {
                        binding.tvTranslatedText.text = it.translatedText
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "No text found in image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error processing image",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showLanguageSelector(isSource: Boolean) {
        val dialog = LanguageSelectorDialog(isSource) { language ->
            if (isSource) {
                sourceLanguage = language.code
            } else {
                targetLanguage = language.code
            }
            updateLanguageDisplay()
            saveSettings()
        }
        dialog.show(supportFragmentManager, "LanguageSelector")
    }

    private fun swapLanguages() {
        val temp = sourceLanguage
        sourceLanguage = targetLanguage
        targetLanguage = temp
        updateLanguageDisplay()
        saveSettings()
    }

    private fun updateLanguageDisplay() {
        binding.btnSourceLanguage.text = sourceLanguage.uppercase()
        binding.btnTargetLanguage.text = targetLanguage.uppercase()
    }

    private fun showHistory() {
        val dialog = HistoryDialog()
        dialog.show(supportFragmentManager, "History")
    }

    private fun showSettings() {
        val dialog = SettingsDialog()
        dialog.show(supportFragmentManager, "Settings")
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            val settings = database.loadSettings()
            sourceLanguage = settings["sourceLanguage"] as? String ?: "en"
            targetLanguage = settings["targetLanguage"] as? String ?: "es"
            updateLanguageDisplay()
        }
    }

    private fun saveSettings() {
        lifecycleScope.launch {
            val settings = mapOf(
                "sourceLanguage" to sourceLanguage,
                "targetLanguage" to targetLanguage
            )
            database.saveSettings(settings)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    // Interactive UI Methods
    private fun performHapticFeedback() {
        if (hapticFeedbackEnabled && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        50,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }

    private fun animateEntrance() {
        // Animate mode buttons
        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        binding.modeSelector.startAnimation(slideIn)

        // Animate cards with stagger
        binding.tvOriginalText.alpha = 0f
        binding.tvTranslatedText.alpha = 0f

        binding.tvOriginalText.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(200)
            .start()

        binding.tvTranslatedText.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(400)
            .start()
    }

    private fun animateButtonSuccess() {
        ObjectAnimator.ofFloat(binding.btnStartStop, "rotation", 0f, 360f).apply {
            duration = 600
            start()
        }
    }

    private fun animateTextUpdate(view: View) {
        view.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}
