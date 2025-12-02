package com.firstapp.langtranslate.ui

import android.Manifest
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
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.firstapp.langtranslate.EchoFlowApp
import com.firstapp.langtranslate.R
import com.firstapp.langtranslate.data.TranslationMode
import com.firstapp.langtranslate.databinding.ActivityMainBinding
import com.firstapp.langtranslate.ml.ASLRecognizer // <<< CHANGE HERE
import com.firstapp.langtranslate.ml.OCREngine // Keep for photo mode
import com.firstapp.langtranslate.services.TranslationService
import com.firstapp.langtranslate.storage.TranslationDatabase
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var translationService: TranslationService? = null
    private var isBound = false

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var camera: Camera? = null

    // We now have two engines for different purposes
    private lateinit var aslRecognizer: ASLRecognizer // <<< ADD THIS
    private lateinit var ocrEngine: OCREngine // For single photo OCR

    private lateinit var database: TranslationDatabase
    private lateinit var vibrator: Vibrator
    // ... (rest of your properties are fine)
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
        // Initialize both engines
        aslRecognizer = ASLRecognizer(this) // <<< ADD THIS
        ocrEngine = OCREngine(this, app.modelManager) // Keep this for photo mode

        database = TranslationDatabase(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        setupUI()
        checkPermissions()
        loadSettings()
        animateEntrance()

        bindTranslationService()
    }

    // ... (Your UI setup methods remain the same)
    private fun setupUI() {
        // Voice/Text toggle buttons
        binding.chipVoice.setOnClickListener {
            showVoiceInput()
            performHapticFeedback()
            binding.chipVoice.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.primary)
            binding.chipVoice.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.chipText.backgroundTintList = null
            binding.chipText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }

        binding.chipText.setOnClickListener {
            showTextInput()
            performHapticFeedback()
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

        // Mode selection buttons
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
            switchMode(TranslationMode.SIGN_LANGUAGE) // This will now trigger the ASL Recognizer
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
        binding.btnSourceLanguage.setOnClickListener { showLanguageSelector(true) }
        binding.btnTargetLanguage.setOnClickListener { showLanguageSelector(false) }
        binding.btnSwapLanguages.setOnClickListener { swapLanguages() }

        // Other UI setup
        binding.btnSelectPhoto.setOnClickListener { photoPickerLauncher.launch("image/*") }
        binding.btnHistory.setOnClickListener { showHistory() }
        binding.btnSettings.setOnClickListener { showSettings() }

        // Wake word toggle
        binding.switchWakeWord.setOnCheckedChangeListener { _, isChecked ->
            wakeWordEnabled = isChecked
            translationService?.setWakeWordEnabled(isChecked)
            performHapticFeedback()
            val message = if (isChecked) getString(R.string.msg_wake_word_enabled) else getString(R.string.msg_wake_word_disabled)
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
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // If permissions granted, restart the current camera mode if applicable
                if (currentMode == TranslationMode.LIVE_CAMERA || currentMode == TranslationMode.SIGN_LANGUAGE) {
                    startCameraTranslation()
                }
            } else {
                Toast.makeText(this, "Permissions required for this feature.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showVoiceInput() {
        binding.tvOriginalText.visibility = View.VISIBLE
        binding.layoutTextInput.visibility = View.GONE
        binding.tvCharCount.visibility = View.GONE
        binding.btnStartStop.text = getString(R.string.action_start)
        binding.btnStartStop.icon = ContextCompat.getDrawable(this, R.drawable.ic_mic)
    }

    private fun showTextInput() {
        binding.tvOriginalText.visibility = View.GONE
        binding.layoutTextInput.visibility = View.VISIBLE
        binding.tvCharCount.visibility = View.VISIBLE
        binding.btnStartStop.text = "Translate"
        binding.btnStartStop.icon = null
        binding.etTextInput.requestFocus()
    }

    private fun switchMode(mode: TranslationMode) {
        if (currentMode == mode) return
        currentMode = mode
        updateModeUI()

        binding.cameraPreview.visibility = View.GONE
        binding.layoutPhoto.visibility = View.GONE

        // Unbind camera if switching to a non-camera mode
        if (mode != TranslationMode.LIVE_CAMERA && mode != TranslationMode.SIGN_LANGUAGE) {
            cameraProviderFuture.get().unbindAll()
        }

        when (mode) {
            TranslationMode.LIVE_CAMERA, TranslationMode.SIGN_LANGUAGE -> {
                binding.cameraPreview.visibility = View.VISIBLE
                startCameraTranslation() // A single method can now handle both
            }
            TranslationMode.PHOTO -> {
                binding.layoutPhoto.visibility = View.VISIBLE
            }
            else -> { /* Handled */ }
        }
    }

    private fun updateModeUI() {
        binding.btnCameraMode.isSelected = currentMode == TranslationMode.LIVE_CAMERA
        binding.btnPhotoMode.isSelected = currentMode == TranslationMode.PHOTO
        binding.btnSignLanguageMode.isSelected = currentMode == TranslationMode.SIGN_LANGUAGE
    }
    private fun startTranslation() {
        if (binding.layoutTextInput.visibility == View.VISIBLE) {
            val textToTranslate = binding.etTextInput.text.toString().trim()
            if (textToTranslate.isNotEmpty()) {
                performTextTranslation(textToTranslate)
            } else {
                Toast.makeText(this, "Please enter text to translate", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (!isBound) bindTranslationService()
        isTranslating = true
        binding.btnStartStop.text = getString(R.string.action_stop)
        binding.btnStartStop.backgroundTintList = ContextCompat.getColorStateList(this, R.color.error)
        binding.btnStartStop.icon = ContextCompat.getDrawable(this, R.drawable.ic_stop)
        translationService?.startVoiceTranslation(sourceLanguage, targetLanguage)
    }

    private fun stopTranslation() {
        isTranslating = false
        binding.btnStartStop.text = getString(R.string.action_start)
        binding.btnStartStop.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
        binding.btnStartStop.icon = ContextCompat.getDrawable(this, R.drawable.ic_mic)
        translationService?.stopTranslation()
    }

    private fun performTextTranslation(text: String) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                if (!isBound) {
                    Toast.makeText(this@MainActivity, "Translation service not ready.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val translation = translationService?.translateText(text, sourceLanguage, targetLanguage)
                if (translation != null) {
                    binding.tvTranslatedText.text = translation.translatedText
                    binding.tvConfidence.text = "Confidence: ${(translation.confidence * 100).toInt()}%"
                    binding.tvConfidence.visibility = View.VISIBLE
                    animateTextUpdate(binding.tvTranslatedText)
                } else {
                    binding.tvTranslatedText.text = "Translation not available"
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Translation error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions()
            return
        }
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
        }

        // Use a front-facing camera for sign language
        val cameraSelector = if (currentMode == TranslationMode.SIGN_LANGUAGE) {
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
        } else {
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        }

        // Choose the correct analyzer based on the current mode
        val analyzer = if (currentMode == TranslationMode.SIGN_LANGUAGE) {
            aslRecognizer
        } else {
            ocrEngine
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
            }

        // Observe results from the appropriate engine
        if (currentMode == TranslationMode.SIGN_LANGUAGE) {
            aslRecognizer.recognizedLetter.observe(this) { letter ->
                binding.tvOriginalText.text = letter
                // You might want to translate this letter or append it
                binding.tvTranslatedText.text = letter // Simple echo for now
            }
        } else { // LIVE_CAMERA OCR mode
            ocrEngine.ocrResult.observe(this) { resultText ->
                if (resultText.isNotEmpty()) {
                    binding.tvOriginalText.text = resultText
                    performTextTranslation(resultText)
                }
            }
        }


        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Log.e("MainActivity", "Camera binding failed", exc)
            Toast.makeText(this, "Could not start camera.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImageForOCR(bitmap: Bitmap) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                // Use the ocrEngine specifically for single photos
                val result = ocrEngine.recognizeText(bitmap, sourceLanguage)
                if (result.text.isNotBlank()) {
                    binding.tvOriginalText.text = result.text
                    performTextTranslation(result.text)
                } else {
                    Toast.makeText(this@MainActivity, "No text found.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error processing image: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    // ... (The rest of your MainActivity file is fine)
    private fun showLanguageSelector(isSource: Boolean) { /* Placeholder */ }
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
    private fun showHistory() { /* Placeholder */ }
    private fun showSettings() { /* Placeholder */ }
    private fun loadSettings() { /* Placeholder */ }
    private fun saveSettings() { /* Placeholder */ }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        cameraProviderFuture.get().unbindAll()
    }

    private fun performHapticFeedback() {
        if (hapticFeedbackEnabled && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }

    private fun animateEntrance() {
        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        binding.modeSelector.startAnimation(slideIn)
    }

    private fun animateTextUpdate(view: View) {
        view.animate().alpha(0.5f).setDuration(150).withEndAction {
            view.animate().alpha(1.0f).setDuration(150).start()
        }.start()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}
