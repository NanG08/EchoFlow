# RunAnywhere SDK Integration Guide

## Overview

EchoFlow is prepared to integrate
with [RunAnywhere SDK](https://github.com/RunanywhereAI/runanywhere-sdks) - a zero-latency,
on-device AI toolkit that brings powerful language models directly to Android applications.

**GitHub Repository**: https://github.com/RunanywhereAI/runanywhere-sdks

## Status

🚧 **Android SDK Coming Soon** - The Android SDK is currently under active development by the
RunAnywhere team.

✅ **iOS SDK Available** - The iOS version is production-ready with full features.

## Features When Available

### 🎙️ Voice AI Workflow

- Real-time voice conversations with transcription and synthesis
- Zero-latency processing
- Fully on-device (Experimental)

### 💬 Text Generation

- High-performance on-device text generation
- Streaming support
- Multi-framework support (GGUF models via llama.cpp)

### 📋 Structured Outputs

- Type-safe JSON generation with schema validation
- Perfect for structured data extraction (Experimental)

### 🔒 Privacy-First Architecture

- All processing happens on-device by default
- No data sent to cloud
- GDPR & privacy compliant

## Integration Steps (When SDK Becomes Available)

### 1. Add Dependency

Add to your `app/build.gradle.kts`:

```kotlin
dependencies {
    // RunAnywhere SDK
    implementation("ai.runanywhere:sdk:0.13.0+")
}
```

### 2. Initialize SDK

In `EchoFlowApp.kt`:

```kotlin
import ai.runanywhere.sdk.RunAnywhereSDK
import ai.runanywhere.sdk.SDKConfiguration

class EchoFlowApp : Application() {
    
    lateinit var runAnywhereSDK: RunAnywhereSDK
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize RunAnywhere SDK
        lifecycleScope.launch {
            runAnywhereSDK = RunAnywhereSDK.shared
            runAnywhereSDK.initialize(
                apiKey = "your-api-key-here", // Get from www.runanywhere.ai
                configuration = SDKConfiguration(
                    privacyMode = PrivacyMode.STRICT, // On-device only
                    debugMode = BuildConfig.DEBUG
                )
            )
        }
    }
}
```

### 3. Use Voice AI

In your activity or service:

```kotlin
// Start voice session
val voiceSession = runAnywhereSDK.startVoiceSession()

// Listen for transcriptions
lifecycleScope.launch {
    voiceSession.transcriptionFlow.collect { result ->
        // Handle transcription
        binding.tvOriginalText.text = result.text
        
        // Translate if needed
        val translation = translateText(result.text, sourceLanguage, targetLanguage)
        binding.tvTranslatedText.text = translation
    }
}

// Start listening
voiceSession.startListening()
```

### 4. Generate Text

```kotlin
val result = runAnywhereSDK.generateText(
    "Translate 'Hello' to Spanish",
    options = GenerationOptions(
        maxTokens = 100,
        temperature = 0.7,
        stream = true
    )
)

lifecycleScope.launch {
    result.collect { chunk ->
        print(chunk.text)
    }
}
```

### 5. Structured Outputs

```kotlin
data class Translation(
    val originalText: String,
    val translatedText: String,
    val confidence: Float
)

val result = runAnywhereSDK.generateStructuredOutput(
    prompt = "Translate 'Hello' to Spanish",
    type = Translation::class.java
)

println("Translated: ${result.translatedText}")
```

## System Requirements

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Kotlin**: 2.0.21+
- **Gradle**: 8.11.1+

## Configuration Options

```kotlin
SDKConfiguration(
    privacyMode = PrivacyMode.STRICT,     // All on-device
    enableVoiceAI = true,                  // Enable voice features
    enableStructuredOutputs = true,        // Enable JSON generation
    debugMode = false,                     // Debug logging
    modelCacheSize = 1024,                 // MB for model cache
    maxConcurrentSessions = 3              // Concurrent AI sessions
)
```

## Privacy Modes

### STRICT (Recommended for EchoFlow)

- All processing happens on-device
- No network calls
- Maximum privacy
- Requires model downloads

### BALANCED

- Intelligent routing between on-device and cloud
- Optimize for performance vs privacy
- Fallback to cloud if needed

### CLOUD

- Cloud-based processing
- Fastest results
- Requires internet connection

## Current Implementation

EchoFlow currently uses:

- Android's built-in `SpeechRecognizer` for voice input
- TensorFlow Lite for on-device ML
- Custom translation engine

**When RunAnywhere SDK becomes available**, we will:

1. Replace `AndroidSpeechRecognizer` with RunAnywhere Voice AI
2. Use on-device LLM for translation
3. Enable structured outputs for better results
4. Maintain zero-latency performance

## Integration File

See `app/src/main/java/com/firstapp/langtranslate/ml/RunAnywhereIntegration.kt` for the prepared
integration structure.

## Resources

- **Website**: https://www.runanywhere.ai
- **GitHub**: https://github.com/RunanywhereAI/runanywhere-sdks
- **iOS Documentation**: https://github.com/RunanywhereAI/runanywhere-sdks/tree/main/sdk/ios
- **Discord**: Join the community for updates
- **Email**: founders@runanywhere.ai

## Stay Updated

Follow the GitHub repository for announcements about the Android SDK release:

```bash
# Star the repository to get notified
git clone https://github.com/RunanywhereAI/runanywhere-sdks
```

## Notes

- The integration structure is already in place
- Code is ready to use the SDK once available
- Current implementation provides full functionality
- Migration will be seamless when SDK is released

---

**EchoFlow** - Zero-Latency Voice Translation
