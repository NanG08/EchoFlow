package com.firstapp.langtranslate.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firstapp.langtranslate.LangTranslateApp
import com.firstapp.langtranslate.R
import com.firstapp.langtranslate.ml.OCREngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var ocrEngine: OCREngine
    private var cameraExecutor: ExecutorService? = null

    private var sourceLanguage = "en"
    private var targetLanguage = "es"

    companion object {
        private const val TAG = "CameraFragment"
        private const val ARG_SOURCE_LANG = "source_lang"
        private const val ARG_TARGET_LANG = "target_lang"

        fun newInstance(sourceLang: String, targetLang: String): CameraFragment {
            return CameraFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SOURCE_LANG, sourceLang)
                    putString(ARG_TARGET_LANG, targetLang)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            sourceLanguage = it.getString(ARG_SOURCE_LANG, "en")
            targetLanguage = it.getString(ARG_TARGET_LANG, "es")
        }

        val app = requireActivity().application as LangTranslateApp
        ocrEngine = OCREngine(requireContext(), app.modelManager)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        previewView = view.findViewById(R.id.previewView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Image analysis for OCR
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor!!) { imageProxy ->
                            processImageProxy(imageProxy)
                        }
                    }

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        lifecycleScope.launch {
            try {
                val bitmap = imageProxyToBitmap(imageProxy)

                // Process every 3rd frame to reduce load
                if (imageProxy.imageInfo.timestamp % 3 == 0L) {
                    val ocrResult = ocrEngine.processFrame(bitmap, sourceLanguage)

                    withContext(Dispatchers.Main) {
                        // Update UI with OCR result
                        if (activity is MainActivity) {
                            // Could emit results to activity
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing frame", e)
            } finally {
                imageProxy.close()
            }
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        // Convert ImageProxy to Bitmap
        // In production, use proper YUV to RGB conversion
        val width = imageProxy.width
        val height = imageProxy.height
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
    }
}
