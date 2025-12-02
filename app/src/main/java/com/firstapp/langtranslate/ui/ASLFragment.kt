package com.firstapp.langtranslate.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firstapp.langtranslate.R
import com.firstapp.langtranslate.ml.ASLRecognizer
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Fragment for American Sign Language (ASL) recognition
 * Uses camera to detect and recognize ASL fingerspelling
 */
class ASLFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var aslRecognizer: ASLRecognizer
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    private lateinit var previewView: PreviewView
    private lateinit var tvRecognizedText: TextView
    private lateinit var tvConfidence: TextView

    private val recognizedText = StringBuilder()

    companion object {
        fun newInstance(): ASLFragment {
            return ASLFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_asl, container, false)

        previewView = view.findViewById(R.id.cameraPreview)
        tvRecognizedText = view.findViewById(R.id.tvRecognizedText)
        tvConfidence = view.findViewById(R.id.tvConfidence)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aslRecognizer = ASLRecognizer(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize ASL model
        lifecycleScope.launch {
            tvConfidence.text = "Initializing ASL model..."

            val initialized = aslRecognizer.initialize()
            if (initialized) {
                tvConfidence.text = "Model loaded! Make ASL signs..."
                startCamera()
            } else {
                val errorMsg = "Failed to load asl_model.tflite. Check logcat for details."
                tvConfidence.text = errorMsg
                Toast.makeText(
                    requireContext(),
                    errorMsg,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Image analyzer
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ASLImageAnalyzer())
                }

            // Select front camera
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Failed to start camera: ${exc.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private inner class ASLImageAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L

        override fun analyze(imageProxy: ImageProxy) {
            val currentTimestamp = System.currentTimeMillis()
            // Process every 500ms to avoid overload
            if (currentTimestamp - lastAnalyzedTimestamp >= 500) {
                val bitmap = imageProxy.toBitmap()

                lifecycleScope.launch {
                    aslRecognizer.recognizeSign(bitmap).collect { result ->
                        requireActivity().runOnUiThread {
                            if (result.error != null) {
                                tvConfidence.text = result.error
                            } else if (result.character.isNotEmpty()) {
                                // Handle special characters
                                when (result.character) {
                                    "space" -> recognizedText.append(" ")
                                    "del" -> {
                                        if (recognizedText.isNotEmpty()) {
                                            recognizedText.deleteCharAt(recognizedText.length - 1)
                                        }
                                    }

                                    "nothing", "_" -> {
                                        // Ignore padding and nothing gestures
                                    }

                                    else -> recognizedText.append(result.character)
                                }

                                tvRecognizedText.text = recognizedText.toString()
                                tvConfidence.text =
                                    "Confidence: ${(result.confidence * 100).toInt()}%"
                            } else if (result.isLowConfidence) {
                                tvConfidence.text =
                                    "Low confidence: ${(result.confidence * 100).toInt()}%"
                            }
                        }
                    }
                }

                lastAnalyzedTimestamp = currentTimestamp
            }

            imageProxy.close()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        aslRecognizer.close()
    }
}
