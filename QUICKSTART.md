# EchoFlow - Quick Start Guide

## 🚀 5-Minute Setup

### Step 1: Prerequisites

- ✅ Android Studio Arctic Fox or newer
- ✅ Android device or emulator (Android 7.0+, API 24+)
- ✅ JDK 17+

### Step 2: Clone & Open

```bash
git clone https://github.com/yourusername/EchoFlow.git
cd EchoFlow
```

Open the project in Android Studio.

### Step 3: Build

```bash
./gradlew clean build
```

Or use Android Studio's "Build" menu → "Make Project"

### Step 4: Run

Connect your device or start an emulator, then:

```bash
./gradlew installDebug
```

Or click the "Run" button (▶️) in Android Studio.

## 🎤 Using EchoFlow

### First Launch

1. **Grant Permissions**
    - Microphone (required for voice)
    - Camera (required for OCR)
    - Bluetooth (optional)

2. **Select Languages**
    - Tap source language (e.g., "EN")
    - Tap target language (e.g., "ES")
    - Or tap swap icon (⇄) to reverse

3. **Choose Mode**
    - **Voice**: Speak to translate
    - **Camera**: Point at text
    - **Photo**: Select from gallery
    - **Conversation**: Two-way translation

### Voice Translation with Wake Word

#### Option A: Wake Word Mode (Hands-Free)

1. Enable "Wake Word" toggle
2. Tap "Start" button
3. Say **"ECHO"**
4. Speak your phrase
5. Listen to translation

#### Option B: Manual Mode

1. Keep "Wake Word" disabled
2. Tap "Start" button
3. Speak your phrase (no wake word needed)
4. Tap "Stop" when done

### Camera Translation

1. Select "Camera" mode
2. Tap "Start"
3. Point camera at text
4. See translation in real-time

### Photo Translation

1. Select "Photo" mode
2. Tap "Select Photo"
3. Choose image from gallery
4. View translated text

## ⚙️ Settings

Access via Settings button (⚙️) at bottom:

- **Languages**: Add or remove supported languages
- **Wake Word**: Enable/disable "ECHO"
- **Haptic Feedback**: Vibration on actions
- **Dark Mode**: Follow system or force
- **Confidence Scores**: Show translation confidence

## 🎯 Pro Tips

### Wake Word Tips

- Speak clearly: "ECHO"
- Works with or without punctuation
- Say in any position: "ECHO translate this" or "translate ECHO this"
- Toggle off for continuous listening

### Voice Quality

- Use in quiet environment
- Speak naturally (not too fast/slow)
- Bluetooth headset supported
- Pause between phrases for better accuracy

### Camera OCR

- Good lighting improves accuracy
- Hold device steady
- Clear, high-contrast text works best
- Multiple text regions detected automatically

### Battery Optimization

- Stop translation when not needed
- Camera mode uses more battery
- Wake word mode is optimized for battery

## 🐛 Troubleshooting

### Microphone Not Working

```
Problem: No voice input
Solutions:
1. Check microphone permission granted
2. Test microphone in other apps
3. Restart app
4. Check volume is not muted
```

### Wake Word Not Detecting

```
Problem: "ECHO" not activating
Solutions:
1. Ensure toggle is ON
2. Speak clearly and loudly
3. Try different pronunciations
4. Check microphone permission
```

### Camera Not Showing

```
Problem: Black screen in camera mode
Solutions:
1. Check camera permission granted
2. Close other apps using camera
3. Restart device
4. Test camera in other apps
```

### Translation Not Accurate

```
Problem: Poor translation quality
Solutions:
1. Check correct languages selected
2. Speak clearly and slowly
3. Use simpler phrases
4. Ensure quiet environment
```

### App Crashes

```
Problem: App closes unexpectedly
Solutions:
1. Update to latest version
2. Clear app cache (Settings → Apps)
3. Restart device
4. Check available storage
5. Report issue with logs
```

## 📱 Feature Overview

### Modes

| Mode | Use Case | Best For |
|------|----------|----------|
| 🎤 Voice | Speaking | Conversations, hands-free |
| 📷 Camera | Live text | Signs, menus, documents |
| 🖼️ Photo | Saved images | Photos, screenshots |
| 💬 Conversation | Two-way | Meetings, travel |

### Languages

Currently supporting **20+ languages**:

- European: English, Spanish, French, German, Italian, Portuguese, Dutch, Polish, Romanian,
  Ukrainian
- Asian: Chinese, Japanese, Korean, Hindi, Vietnamese, Thai, Indonesian, Turkish
- Middle Eastern: Arabic
- And more coming soon!

## 🔒 Privacy

EchoFlow respects your privacy:

- ✅ **All on-device**: No cloud processing
- ✅ **No data collection**: Nothing sent to servers
- ✅ **Local storage**: All data stays on your device
- ✅ **Offline**: Works without internet
- ✅ **Open source**: Transparent and auditable

## 🆘 Need Help?

### Resources

- **README**: Detailed documentation
- **Issues**: [GitHub Issues](https://github.com/yourusername/EchoFlow/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/EchoFlow/discussions)
- **Email**: support@echoflow.app

### Report a Bug

1. Go to GitHub Issues
2. Click "New Issue"
3. Describe the problem
4. Include steps to reproduce
5. Add screenshots if possible

### Request a Feature

1. Go to GitHub Discussions
2. Create new discussion
3. Describe your idea
4. Explain use case
5. Vote on existing ideas

## 🎉 You're Ready!

Start translating with zero latency:

1. Enable wake word
2. Say **"ECHO"**
3. Speak naturally
4. Get instant translation

**Pro Tip**: Use conversation mode for real-time two-way translation!

---

**Welcome to EchoFlow** 🎤✨

Say "ECHO" to start your journey in seamless communication.

[Back to README](README.md) • [RunAnywhere Integration](RUNANYWHERE_INTEGRATION.md) • [Transformation Guide](ECHOFLOW_TRANSFORMATION.md)
