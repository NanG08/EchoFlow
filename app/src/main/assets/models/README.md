# TensorFlow Lite Models Directory

Place your trained TensorFlow Lite models in this directory for on-device inference.

## Required Models

### 1. Speech-to-Text (STT) Models

- **Filename format**: `stt_[language_code].tflite`
- **Examples**:
    - `stt_en.tflite` - English speech recognition
    - `stt_es.tflite` - Spanish speech recognition
- **Input**: Float32 audio samples (16kHz, mono)
- **Output**: Text transcription

**Recommended Models:**

- Mozilla DeepSpeech (converted to TFLite)
- Wav2Vec 2.0 (quantized)
- Custom trained models on CommonVoice datasets

### 2. Translation Models

- **Filename format**: `translation_[src]_[tgt].tflite`
- **Examples**:
    - `translation_en_es.tflite` - English to Spanish
    - `translation_es_en.tflite` - Spanish to English
- **Input**: Tokenized text (INT32 tokens)
- **Output**: Tokenized translation

**Recommended Models:**

- OPUS-MT models (converted to TFLite)
- Custom BERT-based translation models
- Transformer models (quantized)

### 3. OCR Models

- **Detection Model**: `ocr_detection.tflite`
    - Input: RGB image (320x320)
    - Output: Bounding boxes of text regions

- **Recognition Model**: `ocr_recognition.tflite`
    - Input: Cropped text region images
    - Output: Recognized text

**Recommended Models:**

- CRAFT (Character Region Awareness)
- EAST (Efficient Accurate Scene Text detector)
- TrOCR (Transformer-based OCR)

### 4. Text-to-Speech (TTS) Models

- **Filename format**: `tts_[language_code].tflite`
- **Examples**:
    - `tts_en.tflite` - English TTS
    - `tts_es.tflite` - Spanish TTS
- **Input**: Text string or phonemes
- **Output**: Audio waveform or mel-spectrogram

**Recommended Models:**

- Tacotron 2 + WaveGlow (converted to TFLite)
- FastSpeech 2 + MelGAN
- LPCNet vocoder

### 5. Language Detection Model

- **Filename**: `language_detector.tflite`
- **Input**: Text string
- **Output**: Language code with confidence

## Model Conversion

### Converting PyTorch/ONNX to TFLite

```python
import tensorflow as tf

# Load your model
model = tf.keras.models.load_model('your_model.h5')

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]  # or tf.int8 for quantization

tflite_model = converter.convert()

# Save the model
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)
```

### Quantization for Performance

```python
# INT8 Quantization
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = representative_dataset_gen
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type = tf.int8
converter.inference_output_type = tf.int8

tflite_quant_model = converter.convert()
```

## Model Size Guidelines

- **STT models**: 20-50 MB per language
- **Translation models**: 40-100 MB per direction
- **OCR detection**: 10-20 MB
- **OCR recognition**: 20-40 MB
- **TTS models**: 30-80 MB per language

Total app size with 5 language pairs: ~500MB - 1GB

## Testing Models

Test your models before deployment:

```kotlin
// Test model inference
val modelManager = ModelManager(context)
val modelPath = modelManager.getModelPath("stt_en")

// Load and test
val interpreter = Interpreter(File(modelPath))
// Run inference
```

## Where to Get Models

### Open Source

- **Hugging Face**: https://huggingface.co/models
- **TensorFlow Hub**: https://tfhub.dev/
- **ONNX Model Zoo**: https://github.com/onnx/models

### Pre-trained for Mobile

- **MediaPipe**: https://google.github.io/mediapipe/
- **TensorFlow Lite Models**: https://www.tensorflow.org/lite/models

### Training Custom Models

- Use transfer learning on domain-specific data
- Fine-tune on your target languages
- Optimize for mobile inference

## Legal Considerations

- Check model licenses before commercial use
- Some models require attribution
- Training data licenses may restrict usage
- Consider privacy regulations (GDPR, CCPA)

## Performance Tips

1. **Use quantized models** (INT8) for 4x speed improvement
2. **Enable GPU delegate** for 2-3x speedup on compatible devices
3. **Batch processing** where possible
4. **Model caching** to avoid repeated loading
5. **Pre-warm models** during app startup

## Model Updates

To update models without app updates:

- Implement secure model download mechanism
- Verify model signatures
- Test compatibility before replacing
- Provide rollback mechanism

---

**Note**: This directory is currently empty. Add your TensorFlow Lite models to enable full
functionality.
