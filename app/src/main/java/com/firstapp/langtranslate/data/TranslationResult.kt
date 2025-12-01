package com.firstapp.langtranslate.data

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: TranslationMode,
    val confidence: Float = 1.0f
)

data class TranscriptionResult(
    val text: String,
    val language: String,
    val confidence: Float,
    val isFinal: Boolean = false
)

data class OCRResult(
    val text: String,
    val boundingBoxes: List<BoundingBox> = emptyList(),
    val confidence: Float
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val text: String
)
