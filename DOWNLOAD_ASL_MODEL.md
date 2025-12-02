# 📥 Step-by-Step Guide: Download & Install ASL Model

## 🎯 **Quick Steps**

1. **Download the model** from HuggingFace
2. **Rename it** to `asl_model.tflite`
3. **Copy it** to your project's `assets` folder
4. **Build and run** your app

---

## 📝 **Detailed Instructions**

### **Step 1: Download the Model File**

#### Option A: Direct Download (Easiest) ✅

1. **Open your web browser** and go to:
   ```
   https://huggingface.co/ColdSlim/ASL-TFLite-Edge/resolve/main/model.tflite
   ```

2. **The file will start downloading** (36.9 MB)
    - File name: `model.tflite`
    - Location: Usually in your `Downloads` folder

#### Option B: Via HuggingFace Website

1. Go to: https://huggingface.co/ColdSlim/ASL-TFLite-Edge

2. Click on **"Files and versions"** tab

3. Find `model.tflite` (36.9 MB)

4. Click the **download icon** (⬇️) next to it

---

### **Step 2: Rename the File**

1. **Go to your Downloads folder**:
   ```
   Windows: C:\Users\Nandika\Downloads
   ```

2. **Find the file**: `model.tflite`

3. **Rename it to**: `asl_model.tflite`
    - Right-click → Rename
    - Or press F2

---

### **Step 3: Copy to Your Project**

#### Via File Explorer (Windows)

1. **Open File Explorer**

2. **Navigate to**:
   ```
   C:\Users\Nandika\AndroidStudioProjects\LangTranslate\app\src\main\assets
   ```

3. **Copy/Paste** the `asl_model.tflite` file here

4. **Verify the structure**:
   ```
   app/src/main/assets/
   ├── asl_model.tflite  ← Your file (36.9 MB)
   └── models/
       └── README.md
   ```

#### Via PowerShell

```powershell
# Navigate to assets folder
cd C:\Users\Nandika\AndroidStudioProjects\LangTranslate\app\src\main\assets

# Copy and rename in one command
copy C:\Users\Nandika\Downloads\model.tflite asl_model.tflite
```

#### Via Android Studio

1. In Android Studio, right-click on `app/src/main/assets`

2. Select **"Show in Explorer"**

3. **Paste** the `asl_model.tflite` file into the opened folder

4. Go back to Android Studio and **right-click on "app"**

5. Select **"Sync Project with Gradle Files"**

---

### **Step 4: Build & Run**

#### Clean & Build

```bash
cd C:\Users\Nandika\AndroidStudioProjects\LangTranslate

# Clean previous builds
.\gradlew clean

# Build the app
.\gradlew assembleDebug

# Install on device
.\gradlew installDebug
```

#### Or in Android Studio

1. **Build** → **Clean Project**
2. **Build** → **Rebuild Project**
3. **Run** → **Run 'app'**

---

### **Step 5: Test ASL Recognition**

1. **Open EchoFlow** on your device

2. **Select "Sign Language" mode** (button with 🤟 icon)

3. **Grant camera permission** when prompted

4. **Point camera at your hand**

5. **Make ASL signs** (A, B, C, etc.)

6. **See characters appear** in real-time! ✨

---

## ✅ **Verification Checklist**

- [ ] Downloaded `model.tflite` (36.9 MB)
- [ ] Renamed to `asl_model.tflite`
- [ ] Placed in `app/src/main/assets/` folder
- [ ] File size is 36.9 MB (verify!)
- [ ] Synced project in Android Studio
- [ ] Clean build completed
- [ ] App installed on device
- [ ] Sign Language mode works ✓

---

## 📊 **File Information**

| Property | Value |
|----------|-------|
| **Download URL** | https://huggingface.co/ColdSlim/ASL-TFLite-Edge/resolve/main/model.tflite |
| **Original Name** | `model.tflite` |
| **Rename To** | `asl_model.tflite` |
| **File Size** | 36.9 MB |
| **Target Location** | `app/src/main/assets/asl_model.tflite` |
| **Model Type** | TensorFlow Lite (.tflite) |
| **Input Size** | 64x64 RGB image |
| **Output Classes** | 59 ASL characters |

---

## 🎨 **Visual Guide**

### Where to Put the File

```
C:\Users\Nandika\AndroidStudioProjects\LangTranslate
└── app
    └── src
        └── main
            └── assets
                ├── asl_model.tflite  ← PUT IT HERE!
                └── models
                    └── README.md
```

### Folder Structure After Adding

```
assets/
├── asl_model.tflite      (36.9 MB) ← NEW FILE
└── models/
    └── README.md         (4.7 KB)
```

---

## ❓ **Troubleshooting**

### Problem 1: "Can't find the file"

**Solution**: Make sure you've renamed it from `model.tflite` to `asl_model.tflite`

### Problem 2: "App crashes on Sign Language mode"

**Solution**:

- Check file size (should be exactly 36.9 MB)
- Verify location: `app/src/main/assets/asl_model.tflite`
- Rebuild: `./gradlew clean assembleDebug`

### Problem 3: "Model not initialized" error

**Solution**:

- File must be in `assets/` NOT `assets/models/`
- Name must be exactly `asl_model.tflite`

### Problem 4: File won't download

**Alternative direct link**:

```
Right-click and "Save link as":
https://huggingface.co/ColdSlim/ASL-TFLite-Edge/resolve/main/model.tflite
```

---

## 🔗 **Direct Download Link**

**Click here to download directly**:
👉 [Download model.tflite (36.9 MB)](https://huggingface.co/ColdSlim/ASL-TFLite-Edge/resolve/main/model.tflite)

Then rename to `asl_model.tflite` and copy to `app/src/main/assets/`

---

## 🚀 **Summary**

1. ✅ **Download**: https://huggingface.co/ColdSlim/ASL-TFLite-Edge/resolve/main/model.tflite
2. ✅ **Rename**: `model.tflite` → `asl_model.tflite`
3. ✅ **Copy to**: `app/src/main/assets/asl_model.tflite`
4. ✅ **Build**: `./gradlew clean assembleDebug`
5. ✅ **Install**: `./gradlew installDebug`
6. ✅ **Test**: Open app → Sign Language mode → Make ASL signs!

---

**That's it! Your ASL recognition will work after these steps!** 🎉🤟✨

If you run into any issues, check the troubleshooting section or ask for help!
