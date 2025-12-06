package com.firstapp.langtranslate.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.firstapp.langtranslate.R
import com.firstapp.langtranslate.ml.ASLRecognizer
import androidx.camera.core.AspectRatio
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ASLFragment : Fragment(), (GestureRecognizerResult, com.google.mediapipe.framework.image.MPImage) -> Unit {

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
        inflater: LayoutInflater, container: ViewGroup?,
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

        cameraExecutor = Executors.newSingleThreadExecutor()
        // Initialize ASLRecognizer and pass this fragment as the listener
        aslRecognizer = ASLRecognizer(requireContext(), gestureRecognizerListener = this)

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) // Or RATIO_16_9
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        aslRecognizer.recognizeLiveStream(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("ASLFragment", "Failed to start camera", exc)
                Toast.makeText(requireContext(), "Failed to start camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // This is the listener callback function from ASLRecognizer
    override fun invoke(result: GestureRecognizerResult, image: com.google.mediapipe.framework.image.MPImage) {
        activity?.runOnUiThread {
            if (result.gestures().isNotEmpty()) {
                val topGesture = result.gestures()[0][0]
                val categoryName = topGesture.categoryName()
                val confidence = topGesture.score()

                // Handle special characters
                when (categoryName) {
                    "space" -> recognizedText.append(" ")
                    "del" -> {
                        if (recognizedText.isNotEmpty()) {
                            recognizedText.deleteCharAt(recognizedText.length - 1)
                        }
                    }
                    "nothing", "_" -> {
                        // Ignore these gestures
                    }
                    else -> recognizedText.append(categoryName)
                }

                tvRecognizedText.text = recognizedText.toString()
                tvConfidence.text = "Confidence: ${(confidence * 100).toInt()}%"

            } else {
                tvConfidence.text = "No gesture detected"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        aslRecognizer.close()
        cameraExecutor.shutdown()
    }
}
