# LangTranslate - Implementation Guide

This guide explains how to integrate real TensorFlow Lite models into the LangTranslate app to
enable full on-device translation functionality.

## 🎯 Current State

The app is fully structured and functional with placeholder implementations. All components are
ready to accept real TensorFlow Lite models.

## 📋 What's Included

✅ **Complete Architecture**

- Model management system
- On-device inference framework
- Real-time audio processing
- Camera OCR pipeline
- Local storage system
- UI with all modes

✅ **Ready for Integration**

- TFLite interpreter setup
- Input/output tensor handling
- Model loading mechanism
- Performance optimization (GPU delegate)

## 🔧 Integration Steps

### Step 1: Obtain TensorFlow Lite Models

#### Option A: Use Pre-trained Models

**Speech-to-Text:**

```bash
# Download Mozilla DeepSpeech
wget https://github.com/mozilla/DeepSpeech/releases/download/v0.9.3/deepspeech-0.9.3-models.tflite
mv deepspeech-0.9.3-models.tflite app/src/main/assets/models/stt_en.tflite
```

**Translation:**

```python
# Install transformers
pip install transformers tensorflow

# Convert OPUS-MT model to TFLite
from transformers import TFAutoModelForSeq2SeqLM, AutoTokenizer
import tensorflow as tf

model_name = "Helsinki-NLP/opus-mt-en-es"
model = TFAutoModelForSeq2SeqLM.from_pretrained(model_name)
tokenizer = AutoTokenizer.from_pretrained(model_name)

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save
with open("translation_en_es.tflite", "wb") as f:
    f.write(tflite_model)
```

**OCR:**

```bash
# Use Google's ML Kit Text Recognition model
# Or download from TensorFlow Hub
wget https://tfhub.dev/google/lite-model/text-recognition/1
```

**Text-to-Speech:**

```python
# Convert Tacotron 2 to TFLite
# Or use Mozilla TTS
git clone https://github.com/mozilla/TTS.git
cd TTS
# Follow conversion instructions in their docs
```

#### Option B: Train Custom Models

See `TRAINING_GUIDE.md` (to be created) for training instructions.

### Step 2: Integrate STT Model

Update `app/src/main/java/com/firstapp/langtranslate/ml/SpeechRecognizer.kt`:

```kotlin
private var interpreter: Interpreter? = null

init {
    // Load model
    val modelPath = modelManager.getModelPath("stt_$languageCode")
    val model = File(modelPath)
    
    if (model.exists()) {
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            setUseNNAPI(true) // Use NNAPI if available
        }
        interpreter = Interpreter(model, options)
    }
}

private fun processAudioBuffer(
    buffer: ShortArray,
    size: Int,
    languageCode: String
): TranscriptionResult {
    if (interpreter == null) {
        return TranscriptionResult("", languageCode, 0f, false)
    }
    
    // Prepare input tensor
    val inputTensor = Array(1) { FloatArray(size) }
    for (i in 0 until size) {
        inputTensor[0][i] = buffer[i] / 32768.0f // Normalize to [-1, 1]
    }
    
    // Prepare output tensor
    val outputShape = interpreter!!.getOutputTensor(0).shape()
    val output = Array(outputShape[0]) { FloatArray(outputShape[1]) }
    
    // Run inference
    interpreter!!.run(inputTensor, output)
    
    // Decode output (depends on model architecture)
    val text = decodeOutput(output)
    
    return TranscriptionResult(
        text = text,
        language = languageCode,
        confidence = 0.85f,
        isFinal = false
    )
}

private fun decodeOutput(output: Array<FloatArray>): String {
    // Implement CTC decoding or token decoding based on your model
    // For DeepSpeech, use CTC beam search
    // For Wav2Vec, use token-to-text conversion
    return ""
}
```

### Step 3: Integrate Translation Model

Update `app/src/main/java/com/firstapp/langtranslate/ml/TranslationEngine.kt`:

```kotlin
private val interpreters = mutableMapOf<String, Interpreter>()
private val tokenizers = mutableMapOf<String, Tokenizer>()

private fun performTranslation(
    text: String,
    sourceLanguage: String,
    targetLanguage: String
): String {
    val modelKey = "${sourceLanguage}_${targetLanguage}"
    
    // Load model if not cached
    if (!interpreters.containsKey(modelKey)) {
        val modelPath = modelManager.getModelPath("translation_$modelKey")
        val model = File(modelPath)
        
        if (model.exists()) {
            interpreters[modelKey] = Interpreter(model)
            tokenizers[modelKey] = loadTokenizer(modelKey)
        } else {
            return text // Model not available
        }
    }
    
    val interpreter = interpreters[modelKey] ?: return text
    val tokenizer = tokenizers[modelKey] ?: return text
    
    // Tokenize input
    val inputTokens = tokenizer.encode(text)
    val inputTensor = Array(1) { inputTokens }
    
    // Prepare output
    val maxLength = 128
    val outputTensor = Array(1) { IntArray(maxLength) }
    
    // Run inference
    interpreter.run(inputTensor, outputTensor)
    
    // Decode output
    val translatedText = tokenizer.decode(outputTensor[0])
    
    return translatedText
}
```

### Step 4: Integrate OCR Model

Update `app/src/main/java/com/firstapp/langtranslate/ml/OCREngine.kt`:

```kotlin
private var detectionInterpreter: Interpreter? = null
private var recognitionInterpreter: Interpreter? = null

init {
    // Load detection model
    val detectionPath = modelManager.getModelPath("ocr_detection")
    if (File(detectionPath).exists()) {
        detectionInterpreter = Interpreter(File(detectionPath))
    }
    
    // Load recognition model
    val recognitionPath = modelManager.getModelPath("ocr_recognition")
    if (File(recognitionPath).exists()) {
        recognitionInterpreter = Interpreter(File(recognitionPath))
    }
}

private fun runOCRInference(bitmap: Bitmap, languageCode: String): OCRResult {
    if (detectionInterpreter == null || recognitionInterpreter == null) {
        return OCRResult("", emptyList(), 0f)
    }
    
    // Step 1: Detect text regions
    val regions = detectTextRegions(bitmap)
    
    // Step 2: Recognize text in each region
    val recognizedTexts = mutableListOf<String>()
    val boundingBoxes = mutableListOf<BoundingBox>()
    
    regions.forEach { region ->
        val croppedBitmap = cropBitmap(bitmap, region)
        val text = recognizeText(croppedBitmap)
        
        if (text.isNotEmpty()) {
            recognizedTexts.add(text)
            boundingBoxes.add(BoundingBox(
                region.left, region.top, region.right, region.bottom, text
            ))
        }
    }
    
    return OCRResult(
        text = recognizedTexts.joinToString(" "),
        boundingBoxes = boundingBoxes,
        confidence = 0.88f
    )
}

private fun detectTextRegions(bitmap: Bitmap): List<Rect> {
    // Run detection model
    val inputBuffer = bitmapToByteBuffer(bitmap)
    val outputBoxes = Array(1) { Array(100) { FloatArray(4) } }
    val outputScores = Array(1) { FloatArray(100) }
    
    detectionInterpreter!!.run(inputBuffer, mapOf(
        0 to outputBoxes,
        1 to outputScores
    ))
    
    // Filter boxes by confidence threshold
    val regions = mutableListOf<Rect>()
    for (i in 0 until 100) {
        if (outputScores[0][i] > 0.5f) {
            val box = outputBoxes[0][i]
            regions.add(Rect(
                (box[0] * bitmap.width).toInt(),
                (box[1] * bitmap.height).toInt(),
                (box[2] * bitmap.width).toInt(),
                (box[3] * bitmap.height).toInt()
            ))
        }
    }
    
    return regions
}
```

### Step 5: Integrate TTS Model

Update `app/src/main/java/com/firstapp/langtranslate/ml/TextToSpeech.kt`:

```kotlin
private val interpreters = mutableMapOf<String, Interpreter>()

private suspend fun synthesizeSpeech(text: String, languageCode: String): ShortArray =
    withContext(Dispatchers.Default) {
        
        // Load model if not cached
        if (!interpreters.containsKey(languageCode)) {
            val modelPath = modelManager.getModelPath("tts_$languageCode")
            val model = File(modelPath)
            
            if (model.exists()) {
                interpreters[languageCode] = Interpreter(model)
            }
        }
        
        val interpreter = interpreters[languageCode]
        if (interpreter == null) {
            // Return silence if model not available
            return@withContext ShortArray(SAMPLE_RATE)
        }
        
        // Prepare input (text to phonemes or tokens)
        val inputTokens = textToTokens(text, languageCode)
        val inputTensor = Array(1) { inputTokens }
        
        // Run inference to get mel-spectrogram
        val melOutputShape = interpreter.getOutputTensor(0).shape()
        val melSpectrogram = Array(melOutputShape[0]) { FloatArray(melOutputShape[1]) }
        
        interpreter.run(inputTensor, melSpectrogram)
        
        // Convert mel-spectrogram to audio with vocoder
        val audioWaveform = melToAudio(melSpectrogram)
        
        audioWaveform
    }
```

## 🎨 Model Configuration

Create `app/src/main/assets/models/config.json`:

```json
{
  "models": {
    "stt": {
      "en": {
        "file": "stt_en.tflite",
        "sample_rate": 16000,
        "input_shape": [1, 16000],
        "output_classes": 29
      }
    },
    "translation": {
      "en_es": {
        "file": "translation_en_es.tflite",
        "max_length": 128,
        "vocab_size": 50000
      }
    },
    "ocr": {
      "detection": {
        "file": "ocr_detection.tflite",
        "input_size": [320, 320],
        "score_threshold": 0.5
      },
      "recognition": {
        "file": "ocr_recognition.tflite",
        "input_height": 32,
        "max_width": 280
      }
    },
    "tts": {
      "en": {
        "file": "tts_en.tflite",
        "sample_rate": 22050,
        "mel_bins": 80
      }
    }
  }
}
```

## 🧪 Testing

### Unit Tests

Create tests in `app/src/test/java/`:

```kotlin
class SpeechRecognizerTest {
    @Test
    fun testAudioProcessing() {
        // Test audio buffer processing
    }
    
    @Test
    fun testModelInference() {
        // Test model loading and inference
    }
}
```

### Integration Tests

```kotlin
class TranslationIntegrationTest {
    @Test
    fun testEndToEndTranslation() {
        // Test complete translation flow
    }
}
```

## 📊 Performance Benchmarking

Add benchmarking code:

```kotlin
object PerformanceBenchmark {
    fun benchmarkSTT() {
        val start = System.nanoTime()
        // Run STT inference
        val duration = (System.nanoTime() - start) / 1_000_000
        Log.d("Benchmark", "STT inference: ${duration}ms")
    }
}
```

## 🐛 Debugging

Enable TFLite logging:

```kotlin
val options = Interpreter.Options().apply {
    setNumThreads(4)
    setUseNNAPI(true)
}

// Add profiling
val profiler = TfLiteProfiler()
interpreter = Interpreter(model, options)
```

## 🚀 Optimization Tips

1. **Quantization**: Convert models to INT8
2. **GPU Delegate**: Enable for compatible devices
3. **Model Pruning**: Remove unnecessary weights
4. **Batch Processing**: Process multiple inputs together
5. **Memory Mapping**: Use memory-mapped models

## 📱 Device Compatibility

Minimum requirements:

- Android 7.0 (API 24)+
- 2GB RAM
- ARM or x86 CPU
- 500MB storage for models

Recommended:

- Android 10.0 (API 29)+
- 4GB+ RAM
- GPU with NNAPI support
- 1GB+ storage

## 🔒 Security Considerations

- Verify model checksums
- Encrypt sensitive models
- Sandbox model execution
- Monitor memory usage
- Implement model versioning

## 📚 Resources

- [TensorFlow Lite Guide](https://www.tensorflow.org/lite/guide)
- [Model Optimization](https://www.tensorflow.org/lite/performance/model_optimization)
- [NNAPI Documentation](https://developer.android.com/ndk/guides/neuralnetworks)
- [Android ML Documentation](https://developer.android.com/ml)

## 🆘 Troubleshooting

**Model not loading:**

- Check file permissions
- Verify model format
- Check file size
- Validate model signature

**Out of memory:**

- Enable memory mapping
- Use quantized models
- Reduce batch size
- Clear model cache

**Slow inference:**

- Enable GPU delegate
- Use NNAPI
- Optimize model architecture
- Profile with TFLite Profiler

## ✅ Checklist

- [ ] Obtain TFLite models for all languages
- [ ] Test models individually
- [ ] Integrate STT model
- [ ] Integrate Translation model
- [ ] Integrate OCR models
- [ ] Integrate TTS model
- [ ] Add model caching
- [ ] Implement error handling
- [ ] Add performance monitoring
- [ ] Test on multiple devices
- [ ] Optimize for production
- [ ] Add model update mechanism

---

For questions or issues, refer to the main README.md or open a GitHub issue.
