# LangTranslate - Project Structure

Complete overview of the project architecture and file organization.

## 📁 Directory Structure

```
LangTranslate/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/firstapp/langtranslate/
│   │   │   │   ├── data/                    # Data models and types
│   │   │   │   │   ├── Language.kt          # Language definitions
│   │   │   │   │   ├── TranslationMode.kt   # Translation mode enum
│   │   │   │   │   └── TranslationResult.kt # Result data classes
│   │   │   │   │
│   │   │   │   ├── ml/                      # Machine Learning engines
│   │   │   │   │   ├── ModelManager.kt      # Model storage/loading
│   │   │   │   │   ├── ModelDownloader.kt   # Model download system
│   │   │   │   │   ├── SpeechRecognizer.kt  # STT engine
│   │   │   │   │   ├── TranslationEngine.kt # Translation engine
│   │   │   │   │   ├── OCREngine.kt         # OCR engine
│   │   │   │   │   └── TextToSpeech.kt      # TTS engine
│   │   │   │   │
│   │   │   │   ├── services/                # Background services
│   │   │   │   │   └── TranslationService.kt # Foreground translation service
│   │   │   │   │
│   │   │   │   ├── storage/                 # Local data storage
│   │   │   │   │   └── TranslationDatabase.kt # JSON-based storage
│   │   │   │   │
│   │   │   │   ├── ui/                      # User interface
│   │   │   │   │   ├── MainActivity.kt      # Main activity
│   │   │   │   │   ├── CameraFragment.kt    # Camera OCR fragment
│   │   │   │   │   ├── LanguageSelectorDialog.kt # Language picker
│   │   │   │   │   ├── HistoryDialog.kt     # History viewer
│   │   │   │   │   └── SettingsDialog.kt    # Settings dialog
│   │   │   │   │
│   │   │   │   ├── utils/                   # Utility classes
│   │   │   │   │   ├── VoiceCommandHandler.kt # Voice commands
│   │   │   │   │   ├── BluetoothAudioManager.kt # BT audio
│   │   │   │   │   ├── PermissionHelper.kt  # Permission handling
│   │   │   │   │   └── AudioUtils.kt        # Audio processing
│   │   │   │   │
│   │   │   │   └── LangTranslateApp.kt      # Application class
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── layout/                  # UI layouts
│   │   │   │   │   ├── activity_main.xml    # Main screen
│   │   │   │   │   ├── fragment_camera.xml  # Camera view
│   │   │   │   │   ├── dialog_language_selector.xml
│   │   │   │   │   ├── dialog_history.xml
│   │   │   │   │   ├── dialog_settings.xml
│   │   │   │   │   ├── item_language.xml
│   │   │   │   │   └── item_history.xml
│   │   │   │   │
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml           # Color definitions
│   │   │   │   │   ├── strings.xml          # String resources
│   │   │   │   │   └── themes.xml           # App themes
│   │   │   │   │
│   │   │   │   ├── drawable/                # Vector drawables
│   │   │   │   ├── mipmap-*/                # App icons
│   │   │   │   └── xml/                     # XML configs
│   │   │   │
│   │   │   ├── assets/
│   │   │   │   └── models/                  # TFLite models
│   │   │   │       └── README.md            # Model documentation
│   │   │   │
│   │   │   └── AndroidManifest.xml          # App manifest
│   │   │
│   │   ├── test/                            # Unit tests
│   │   │   └── java/com/firstapp/langtranslate/
│   │   │       └── TranslationEngineTest.kt
│   │   │
│   │   └── androidTest/                     # Instrumented tests
│   │       └── java/com/firstapp/langtranslate/
│   │
│   ├── build.gradle.kts                     # App build config
│   └── proguard-rules.pro                   # ProGuard rules
│
├── gradle/                                  # Gradle wrapper
│   ├── wrapper/
│   └── libs.versions.toml                   # Dependency versions
│
├── build.gradle.kts                         # Project build config
├── settings.gradle.kts                      # Project settings
├── gradle.properties                        # Gradle properties
│
├── README.md                                # Main documentation
├── FEATURES.md                              # Feature documentation
├── IMPLEMENTATION_GUIDE.md                  # Model integration guide
└── PROJECT_STRUCTURE.md                     # This file
```

## 🏗️ Architecture Overview

### Layer Architecture

```
┌─────────────────────────────────────────────┐
│              UI Layer                        │
│  (MainActivity, Fragments, Dialogs)         │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│          Service Layer                       │
│      (TranslationService)                    │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│         Business Logic Layer                 │
│  (ML Engines, Storage, Utilities)           │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│          Data Layer                          │
│  (Models, Local Files, TFLite Models)       │
└─────────────────────────────────────────────┘
```

## 🔄 Data Flow

### Voice Translation Flow

```
User Speech
    ↓
AudioRecord
    ↓
SpeechRecognizer (STT Model)
    ↓
TranslationEngine (Translation Model)
    ↓
TextToSpeech (TTS Model)
    ↓
AudioTrack/Bluetooth
    ↓
Output Audio
```

### Camera OCR Flow

```
Camera Feed
    ↓
CameraX Preview
    ↓
Image Frame
    ↓
OCREngine (Detection + Recognition)
    ↓
TranslationEngine
    ↓
UI Display
```

### Conversation Mode Flow

```
Speaker A (Language 1)
    ↓
Auto Language Detection
    ↓
Translation to Language 2
    ↓
TTS Output
    ↓
Speaker B (Language 2)
    ↓
Auto Language Detection
    ↓
Translation to Language 1
    ↓
TTS Output
    ↓
(Repeat)
```

## 📦 Key Components

### 1. LangTranslateApp

- Application entry point
- Initializes ModelManager
- Singleton instance access

### 2. ModelManager

- Manages TFLite model files
- Handles model loading/caching
- Verifies model integrity

### 3. SpeechRecognizer

- Continuous audio recording
- Voice activity detection
- On-device STT inference
- Streaming transcription

### 4. TranslationEngine

- Neural machine translation
- Language detection
- Translation caching
- Batch processing

### 5. OCREngine

- Text detection in images
- Text recognition
- Bounding box generation
- Real-time processing

### 6. TextToSpeech

- Audio synthesis
- Low-latency playback
- Multi-language support
- Audio streaming

### 7. TranslationService

- Foreground service
- Continuous translation
- Bluetooth integration
- Notification management

### 8. TranslationDatabase

- JSON-based storage
- History management
- Settings persistence
- Search functionality

### 9. MainActivity

- Main UI controller
- Mode switching
- Permission handling
- Fragment management

## 🔌 Dependencies

### Core Android

- `androidx.appcompat:appcompat` - Compatibility
- `androidx.core:core-ktx` - Kotlin extensions
- `androidx.constraintlayout` - Layouts
- `androidx.lifecycle` - Lifecycle management
- `com.google.android.material` - Material Design

### Camera

- `androidx.camera:camera-core` - CameraX core
- `androidx.camera:camera-camera2` - Camera2 implementation
- `androidx.camera:camera-lifecycle` - Lifecycle integration
- `androidx.camera:camera-view` - Preview view

### Machine Learning

- `org.tensorflow:tensorflow-lite` - TFLite runtime
- `org.tensorflow:tensorflow-lite-support` - Support library
- `org.tensorflow:tensorflow-lite-gpu` - GPU acceleration
- `org.tensorflow:tensorflow-lite-task-vision` - Vision tasks
- `org.tensorflow:tensorflow-lite-task-text` - Text tasks

### Async & Storage

- `org.jetbrains.kotlinx:kotlinx-coroutines-android` - Coroutines
- `androidx.room:room-runtime` - Room database
- `androidx.work:work-runtime-ktx` - Background tasks

### Testing

- `junit:junit` - Unit testing
- `kotlinx-coroutines-test` - Coroutine testing
- `io.mockk:mockk` - Mocking
- `androidx.test.ext:junit` - Android testing
- `androidx.test.espresso:espresso-core` - UI testing

## 🎨 UI Components

### Layouts

- **activity_main.xml**: Main screen with mode selection
- **fragment_camera.xml**: Camera preview for OCR
- **dialog_language_selector.xml**: Language picker dialog
- **dialog_history.xml**: Translation history viewer
- **dialog_settings.xml**: Settings configuration
- **item_language.xml**: Language list item
- **item_history.xml**: History list item

### Resources

- **colors.xml**: Color palette (Material Design)
- **strings.xml**: Localized strings
- **themes.xml**: Light/dark themes
- **icons**: Launcher icons in multiple densities

## 🔐 Permissions

### Runtime Permissions

- `RECORD_AUDIO` - Voice translation
- `CAMERA` - OCR translation
- `BLUETOOTH_CONNECT` - Bluetooth audio (Android 12+)

### Storage Permissions

- `READ_MEDIA_IMAGES` - Photo selection (Android 13+)
- `READ_EXTERNAL_STORAGE` - Photo access (Android 12-)

### Service Permissions

- `FOREGROUND_SERVICE` - Background translation
- `FOREGROUND_SERVICE_MICROPHONE` - Audio in background

## 📊 Performance Characteristics

### Memory Usage

- **Base**: ~50-80 MB
- **With STT Model**: +40-60 MB
- **With Translation Models**: +80-120 MB per pair
- **With OCR Models**: +50-70 MB
- **Total Typical**: 220-330 MB

### Storage Requirements

- **App APK**: ~15-25 MB
- **Code/Resources**: ~10 MB
- **Single Language Pack**: 80-120 MB
- **5 Language Pairs**: 400-600 MB
- **User Data**: <5 MB

### CPU Usage

- **Idle**: <1%
- **Voice Translation**: 15-30%
- **Camera OCR**: 25-40%
- **Background**: <5%

### Battery Impact

- **Voice Mode**: Moderate (audio recording)
- **Camera Mode**: High (camera + processing)
- **Photo Mode**: Low (one-time processing)
- **Background**: Minimal (when not active)

## 🔧 Build Configuration

### Build Types

- **Debug**: Debugging enabled, no minification
- **Release**: ProGuard enabled, optimized

### Product Flavors

- Single flavor (can extend for free/pro versions)

### ABI Filters

- armeabi-v7a (32-bit ARM)
- arm64-v8a (64-bit ARM)
- x86 (32-bit Intel)
- x86_64 (64-bit Intel)

### Min/Target SDK

- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 34 (Android 14)
- **compileSdk**: 34

## 🧪 Testing Strategy

### Unit Tests

- Data model tests
- Utility function tests
- Algorithm tests
- Logic verification

### Integration Tests

- Service communication
- Database operations
- File I/O
- Model loading

### UI Tests

- Activity lifecycle
- Fragment navigation
- Dialog interactions
- Permission flows

### Performance Tests

- Model inference latency
- Memory usage
- Battery consumption
- Storage efficiency

## 📱 Deployment

### Release Checklist

- [ ] Test on multiple devices
- [ ] Verify all permissions
- [ ] Test offline functionality
- [ ] Check battery usage
- [ ] Validate model loading
- [ ] Test error handling
- [ ] Review ProGuard rules
- [ ] Generate signed APK
- [ ] Test on Android 7-14
- [ ] Verify storage requirements

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on device
./gradlew installDebug

# Generate APK
./gradlew bundleRelease
```

## 🔍 Code Quality

### Static Analysis

- Android Lint
- Kotlin compiler warnings
- ProGuard verification

### Code Style

- Kotlin coding conventions
- Material Design guidelines
- Android best practices

### Documentation

- KDoc for public APIs
- Inline comments for complex logic
- README files in key directories

## 🚀 Future Enhancements

### Planned Features

- [ ] Real-time subtitle overlay
- [ ] Widget for quick translation
- [ ] Watch companion app
- [ ] Cloud sync (optional)
- [ ] More language models
- [ ] Custom model training
- [ ] AR translation overlay
- [ ] Offline maps integration

### Technical Improvements

- [ ] Kotlin Multiplatform (iOS support)
- [ ] Jetpack Compose UI
- [ ] Model quantization
- [ ] Better compression
- [ ] Voice recognition improvements
- [ ] GPU optimization
- [ ] Federated learning

---

For implementation details, see IMPLEMENTATION_GUIDE.md
For features, see FEATURES.md
For usage, see README.md
