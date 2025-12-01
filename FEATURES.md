# LangTranslate - Feature Documentation

Complete feature breakdown and usage guide for the LangTranslate app.

## 🎤 Voice Translation Mode

### Overview

Real-time speech-to-text translation with instant audio playback.

### Features

- **Continuous Recognition**: Constantly listening for speech
- **Voice Activity Detection**: Automatically detects when you're speaking
- **Low Latency**: < 500ms from speech to translation
- **Auto Punctuation**: Intelligent sentence boundary detection
- **Confidence Scoring**: Optional display of translation accuracy

### Usage

1. Tap "Voice" mode button
2. Select source and target languages
3. Tap "Start" to begin translation
4. Speak naturally - translation appears in real-time
5. Tap "Stop" to pause

### Voice Commands

- "Start translation" - Begin translating
- "Stop translation" - Stop translating
- "Swap languages" - Switch source/target
- "Repeat" - Replay last translation

### Technical Details

- **Sample Rate**: 16kHz mono
- **Buffer Size**: 100ms chunks
- **Processing**: On-device TFLite STT
- **Latency**: 200-500ms end-to-end

## 📷 Live Camera OCR Mode

### Overview

Point your camera at text for instant translation overlay.

### Features

- **Real-Time Detection**: Text detected in live camera feed
- **Multi-Language Support**: Detects text in any supported language
- **Bounding Boxes**: Visual highlighting of detected text regions
- **Auto-Focus**: Optimized for text clarity
- **Frame Optimization**: Processes every 3rd frame for performance

### Usage

1. Tap "Live Camera" mode
2. Point camera at text (signs, menus, documents)
3. Text automatically detected and translated
4. Translated text appears as overlay

### Best Practices

- Good lighting conditions
- Steady camera position
- Clear, printed text (not handwritten)
- Text fills at least 20% of frame
- Distance: 10-50cm from text

### Technical Details

- **Resolution**: 1280x720 preview
- **Detection**: CRAFT/EAST algorithm
- **Recognition**: CRNN model
- **Processing**: 100-300ms per frame

## 🖼️ Photo Translation Mode

### Overview

Translate text from existing photos or screenshots.

### Features

- **Gallery Integration**: Select any photo
- **Batch Processing**: Translate multiple images
- **High Quality**: Full resolution processing
- **Text Extraction**: Complete text capture
- **Export Results**: Save translations

### Usage

1. Tap "Photo" mode
2. Tap "Select Photo"
3. Choose image from gallery
4. Text is extracted and translated
5. View both original and translated text

### Supported Formats

- JPEG/JPG
- PNG
- WebP
- BMP

### Tips

- Use high-resolution images
- Ensure text is legible
- Avoid blurry or low-light photos

## 💬 Conversation Mode

### Overview

Bidirectional translation for natural conversations between two people.

### Features

- **Auto Language Detection**: Automatically identifies speaker's language
- **Turn-Taking**: Seamless switching between speakers
- **Context Awareness**: Maintains conversation context
- **Speaker Identification**: Visual indication of source language
- **Continuous Flow**: No manual switching needed

### Usage

1. Tap "Conversation" mode
2. Select two languages (e.g., English ↔ Spanish)
3. Tap "Start"
4. First person speaks in Language A
5. Translation plays in Language B
6. Second person responds in Language B
7. Translation plays in Language A
8. Continue naturally

### Use Cases

- Tourist conversations
- Business meetings
- Medical consultations
- Customer service
- Educational settings

### Technical Details

- **Detection**: Language identification model
- **Latency**: < 1 second between speakers
- **Accuracy**: 90%+ for common language pairs

## 🎧 Bluetooth Audio Support

### Overview

Route audio to Bluetooth headsets for hands-free translation.

### Features

- **Auto-Detection**: Automatically detects paired devices
- **Low Latency**: Optimized SCO audio routing
- **Mic Input**: Use Bluetooth headset microphone
- **Audio Output**: Translation plays through headset
- **Multi-Device**: Supports multiple Bluetooth profiles

### Supported Devices

- Bluetooth headphones
- Bluetooth earbuds
- Car Bluetooth systems
- Bluetooth speakers (output only)

### Setup

1. Pair Bluetooth device in Android settings
2. Open LangTranslate
3. Audio automatically routes to Bluetooth when connected
4. Use device controls for volume

### Troubleshooting

- Ensure device is paired and connected
- Grant Bluetooth permission
- Check device compatibility
- Restart app if audio doesn't route

## 📚 Translation History

### Overview

Access and search all past translations.

### Features

- **Automatic Saving**: All translations saved locally
- **Search**: Find translations by text
- **Filter**: Sort by date, language, or mode
- **Export**: Share or export history
- **Privacy**: Stored only on device

### Usage

1. Tap History button (clock icon)
2. Browse past translations
3. Tap entry to view details
4. Use search to find specific translations

### Data Management

- **Storage**: Up to 1000 recent translations
- **Size**: ~1MB typical storage
- **Clear**: Delete history in Settings
- **Backup**: Manual export available

## ⚙️ Settings & Customization

### Available Settings

#### Language Settings

- **Auto-Detect Language**: Automatically identify source language
- **Default Languages**: Set preferred language pair
- **Downloaded Languages**: Manage offline language packs

#### Translation Settings

- **Continuous Mode**: Keep translating without stopping
- **Show Confidence**: Display translation accuracy scores
- **Auto-Play Audio**: Automatically speak translations

#### Interface Settings

- **Dark Mode**: Toggle dark theme
- **Haptic Feedback**: Vibration on actions
- **Font Size**: Adjust text size
- **Keep Screen On**: Prevent screen timeout during translation

#### Privacy Settings

- **Clear History**: Delete all translations
- **Auto-Delete**: Automatically clear old translations
- **Analytics**: No tracking (always disabled)

### Data & Storage

- **Model Management**: Download/delete language packs
- **Cache Size**: View storage usage
- **Clear Cache**: Free up space

## 🔒 Privacy & Security

### Data Handling

- **100% Offline**: No cloud processing
- **Local Storage**: All data stays on device
- **No Tracking**: Zero analytics or telemetry
- **No Accounts**: No login required
- **Encrypted Storage**: Optional encryption

### Permissions

- **Microphone**: Voice translation only
- **Camera**: OCR translation only
- **Storage**: Photo access only
- **Bluetooth**: Audio routing only

### Data Deletion

- Delete history anytime
- Uninstall removes all data
- No cloud backups
- Complete data ownership

## 🌍 Supported Languages

### Full Support (STT + Translation + TTS)

- English (en)
- Spanish (es)
- French (fr)
- German (de)
- Italian (it)
- Portuguese (pt)
- Russian (ru)
- Chinese (zh)
- Japanese (ja)
- Korean (ko)

### Translation Only

- Arabic (ar)
- Hindi (hi)
- Dutch (nl)
- Polish (pl)
- Turkish (tr)
- Vietnamese (vi)
- Thai (th)
- Indonesian (id)
- Ukrainian (uk)
- Romanian (ro)

### Adding New Languages

See IMPLEMENTATION_GUIDE.md for adding language models.

## 📊 Performance Metrics

### Device Requirements

**Minimum:**

- Android 7.0+
- 2GB RAM
- 500MB free storage
- Dual-core CPU

**Recommended:**

- Android 10.0+
- 4GB RAM
- 1GB free storage
- Quad-core CPU
- GPU with NNAPI

### Benchmark Results

| Feature | Latency | Accuracy |
|---------|---------|----------|
| STT | 200-400ms | 85-95% |
| Translation | 50-150ms | 88-96% |
| OCR | 100-300ms | 82-92% |
| TTS | 100-200ms | 90-98% |
| End-to-End | 500-900ms | 80-90% |

*Results vary by device and language pair

## 🎯 Use Cases

### Travel

- Restaurant menus
- Street signs
- Hotel conversations
- Shopping assistance
- Emergency communication

### Business

- Client meetings
- Conference calls
- Document translation
- Email drafting
- Presentations

### Education

- Language learning
- Research papers
- Study materials
- Lecture notes
- Homework help

### Healthcare

- Patient consultations
- Medical records
- Prescription labels
- Emergency situations
- Care instructions

### Personal

- Video subtitles
- Book reading
- Social media
- Messaging
- Entertainment

## 🆘 Troubleshooting

### Common Issues

**Audio not recording:**

- Check microphone permission
- Test with other apps
- Restart device
- Check volume levels

**OCR not detecting text:**

- Improve lighting
- Hold camera steady
- Move closer to text
- Ensure text is clear
- Try different angle

**Translation inaccurate:**

- Speak clearly
- Use proper grammar
- Check source language
- Verify model downloaded
- Report issue

**App crashes:**

- Free up device memory
- Clear app cache
- Reinstall app
- Update Android OS
- Check device compatibility

**Bluetooth issues:**

- Re-pair device
- Check permissions
- Restart Bluetooth
- Update device firmware

## 💡 Tips & Best Practices

### For Voice Translation

1. Speak naturally at normal pace
2. Pause between sentences
3. Minimize background noise
4. Use clear pronunciation
5. Speak complete thoughts

### For Camera OCR

1. Ensure good lighting
2. Keep camera steady
3. Position text centrally
4. Avoid glare and shadows
5. Use landscape orientation

### For Conversation Mode

1. One speaker at a time
2. Wait for translation to complete
3. Speak clearly
4. Use natural pauses
5. Avoid interrupting

### Battery Optimization

- Close when not in use
- Use power saving mode
- Lower screen brightness
- Disable unused features
- Update to latest version

## 🚀 Advanced Features

### Power User Tips

- Voice commands for hands-free control
- Bluetooth headset for privacy
- Batch photo translation
- Custom language shortcuts
- Gesture controls (coming soon)

### Keyboard Shortcuts

- Volume up: Start/stop translation
- Volume down: Swap languages
- Power button 2x: Launch app

### Experimental Features

Enable in Settings > Advanced:

- GPU acceleration
- Higher quality models
- Extended language detection
- Real-time subtitle overlay

---

For more information, see README.md and IMPLEMENTATION_GUIDE.md
