package com.firstapp.langtranslate.data

enum class TranslationMode {
    VOICE,           // Real-time voice translation (handles conversations too)
    LIVE_CAMERA,     // Live camera OCR translation
    PHOTO,           // Photo & Screenshot translation (combined)
    SIGN_LANGUAGE,   // ASL (American Sign Language) recognition
    TEXT_ENTRY       // Manual text input for translation
}
