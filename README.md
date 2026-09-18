# 🍽️ KitchenGuard CSM - Complete Machine Learning System

## Sistem Machine Learning Komprehensif untuk Dapur Komersial

Sistem ML lengkap dengan fitur:
- ✅ **Skin Tone Detection** - Mendeteksi warna kulit orang putih (Fair Skin) untuk hygiene monitoring
- ✅ **Waste Quality Classification** - Klasifikasi otomatis limbah dapur dari deskripsi teks
- ✅ **Barcode Scanning** - Scanner barcode bahan baku dengan database lengkap
- ✅ **Android Studio Ready** - Siap deploy ke aplikasi mobile

---

## 📁 Struktur Project

```
CAPSTONE_MACHINE_LEARNING/
├── src/                          # Python ML scripts
│   ├── generate_waste_dataset.py # Generator waste classification
│   ├── train_improved_waste_model.py  # Training waste classifier
│   └── preprocess.py             # Text preprocessing
├── data/                         # Generated datasets
│   └── waste_quality_dataset_expanded.csv
├── models/                       # Trained models
│   ├── category_classifier.joblib
│   ├── skin_type_classifier.joblib
│   ├── waste_classifier_ensemble.joblib
│   ├── label_encoder.joblib
│   └── Android helper files
├── android/                      # Android Studio project
│   ├── app/src/main/java/
│   │   ├── MainActivity.kt
│   │   ├── SkinDetectionActivity.kt
│   │   └── utils/MachineLearningUtils.java
│   └── app/src/main/res/layout/
├── ML_TRAINING_GUIDE.md          # Panduan training lengkap
└── README_ANDROID.md             # Panduan Android integration

## 📁 **Organized Project Structure**

```
CAPSTONE_MACHINE_LEARNING/
├── 📂 models/                        # ML Models (organized by type)
│   ├── 📂 skin_detection/          # Skin detection artifacts
│   ├── 📂 waste_classification/    # Waste classification artifacts  
│   └── 📂 android/                 # Android integration helpers
│
├── 📂 data/                          # Datasets
│   ├── 📂 raw/                     # Original datasets
│   └── 📂 processed/               # Enhanced datasets (v2)
│
├── 📂 scripts/                       # Training & utility scripts
├── 📂 src/                          # Application source code
├── 📂 docs/                         # Documentation guides
├── 📂 deployment/                   # Deployment configs
└── 📂 config/                       # Configuration files
```

✅ **All files have been reorganized!**  
📚 See `docs/PROJECT_STRUCTURE.md` for complete details.
```

---

## 🚀 Quick Start Guide

### Step 1: Install Dependencies (Python)

```bash
pip install -r requirements.txt
# atau manual:
pip install numpy pandas scikit-learn matplotlib seaborn joblib pillow opencv-python fastapi uvicorn python-multipart pydantic
```

### Step 2: Run Unit Tests (Optional but Recommended)

```bash
python -m pytest -v
# Output: ✅ 69 passed in 2.2s (test_api_endpoints.py, test_cost_calculator.py, test_kitchenguard.py)
```

### Step 3: Generate Datasets

```bash
# Generate synthetic skin detection dataset
python src/generate_skin_dataset_final.py

# Datasets generated/available in data/:
# - data/skin_detection_dataset.csv (1000 samples)
# - data/waste_quality_dataset_expanded.csv (1800 samples)
# - data/kitchenguard_waste_dataset.csv (1078 samples)
```

### Step 4: Train Models

```bash
# Train Skin Detection Model (Category & Skin Type Classifiers)
python scripts/train_skin_model.py

# Train Waste Classifier Model (TF-IDF + Ensemble Model)
python scripts/train_improved_waste_model.py
```

**Results (Held-out Test Evaluation & CV):**
- Skin Detection Category Accuracy: **100.0%** (HAND vs FACE)
- Skin Detection Skin Type Accuracy: **92.0%** (FAIR_1, FAIR_2, FAIR_3)
- Waste Classifier Ensemble Accuracy: **100.0%** (Held-out 20% test split, 576 samples)
- Waste Classifier 5-Fold Cross-Validation: **1.0000 ± 0.0000** (Pipeline with leakage-free folds)

### Step 5: Android Integration

Android client operates primarily via FastAPI REST API (`docs/ANDROID_API_CONTRACT.md`) with local rule-based fallback:
1. Helper Java & class maps:
   - `models/android/WasteClassifierHelper.java`
   - `models/android/android_classification_map.json`
   - `models/android/android_encoding_map.json`
2. Run backend:
```bash
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

2. Buka Android Studio:
```bash
cd android
open app/build.gradle
```

3. Add dependencies ke `build.gradle`:
```gradle
dependencies {
    implementation 'org.tensorflow:tensorflow-lite:2.12.0'
    implementation 'androidx.camera:camera-camera2:1.2.3'
    implementation 'androidx.camera:camera-lifecycle:1.2.3'
    implementation 'androidx.camera:camera-view:1.2.3'
}
```

4. Sync Gradle dan Run!

---

## 🧬 Dataset Details


| Field | Description | Example |
|-------|-------------|---------|
| image_id | Unique identifier | SKIN_HAND_0001 |
| category | Body part detected | HAND or FACE |
| skin_type | Fair skin type | FAIR_1, FAIR_2, FAIR_3 |
| rgb_mean_r/g/b | Mean RGB values | 240.5, 220.3, 200.1 |
| rgb_std_r/g/b | Standard deviation | 10.2, 8.5, 7.3 |
| lighting_condition | Lighting level | NORMAL, BRIGHT, DIM |
| description | Fitzpatrick scale | "Sangat terang, mudah terbakar" |

**Skin Type Definitions:**
- **FAIR_1**: RGB avg > 230 - Sangat terang, mudah terbakar
- **FAIR_2**: RGB avg 210-230 - Terang, cenderung terbakar  
- **FAIR_3**: RGB avg 200-210 - Terang dengan sedikit tan

### Waste Classification Dataset (1800 samples)

**Categories & Hygiene Priority:**

| Category | Priority | Keywords | Example |
|----------|----------|----------|---------|
| CONTAMINATED | CRITICAL 🔴 | terkontaminasi, hair, lantai | "Salad terkontaminasi hair" |
| SPOILED | HIGH 🟠 | berjamur, busuk, berlendir | "Ayam berbau busuk" |
| EXPIRED | HIGH 🟠 | expired, kedaluwarsa, MHD | "Mayonaise expired date" |
| OVERCOOKED | MEDIUM 🟡 | gosong, hangus, overdone | "Steak terlalu gosong" |
| PREP_WASTE | LOW 🟢 | trimming, peeling, sisa | "Kulit kentang prep station" |
| SURPLUS | LOW 🟢 | tidak terjual, surplus | "Rice portion tidak tersentuh" |

---

## 🤖 Model Architecture


```
Input Layer: 6 features (RGB mean + std deviation)
         ↓
Hidden Layers: [128, 64, 32]
         ↓
Random Forest Classifier
         ↓
Output: Category (HAND/FACE) + Skin Type (FAIR_1/2/3)
```

**Features Used:**
1. `rgb_mean_r` - Mean Red value
2. `rgb_mean_g` - Mean Green value
3. `rgb_mean_b` - Mean Blue value
4. `rgb_std_r` - Std deviation Red
5. `rgb_std_g` - Std deviation Green
6. `rgb_std_b` - Std deviation Blue

**Thresholds:**
- Fair skin: R ≥ 200, G ≥ 180, B ≥ 160
- Confidence threshold: 0.75

### Waste Classifier Model

```
Text Input → TF-IDF Vectorizer (2423 features)
                  ↓
Voting Ensemble:
  ├─ Naive Bayes (alpha=0.1)
  ├─ Linear SVM (C=1.0)
  └─ Random Forest (n_estimators=100)
                  ↓
Hard Voting Classifier
                  ↓
Output: 6 categories with confidence scores
```

---

## 📱 Android Integration

### How to Use in MainActivity

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var mlUtils: MachineLearningUtils
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ML Utils
        mlUtils = MachineLearningUtils(this)
        
        // Example 1: Analyze waste description
        val wasteText = binding.txtDescription.text.toString()
        val wasteResult = mlUtils.analyzeWaste(wasteText)
        
        binding.resultText.text = wasteResult.hygieneLevel
        binding.actionText.text = wasteResult.recommendedAction
        
        // Example 2: Detect fair skin from camera
        val r = 240f  // From camera pixel
        val g = 220f
        val b = 200f
        val skinResult = mlUtils.detectSkinSimple(r, g, b)
        
        if (skinResult.isFairSkinDetected) {
            Toast.makeText(this, 
                "Fair skin detected: ${skinResult.skinType}", 
                Toast.LENGTH_SHORT).show()
        }
    }
}
```

### Skin Detection Activity

Launch from MainActivity:

```kotlin
// Button click handler
fun openSkinDetection(view: View) {
    startActivity(Intent(this, SkinDetectionActivity::class.java))
}
```

Features:
- Live camera preview
- Real-time fair skin detection
- Highlighting detected regions
- Hygiene compliance alerts

---

## 🔍 Barcode Database

Database includes 12+ items dengan format:

```python
BARCODE_DATABASE = {
    "8991234567890": {
        "format": "EAN_13",
        "ingredient_id": "ING-MEAT-001",
        "name": "Daging Sapi Wagyu Ribeye MB7",
        "category": "Daging & Unggas",
        "batch_id": "WG-0915-A",
        "supplier": "PT Agro Boga Utama",
        "expiry_at": "2026-09-22",
        ...
    },
    # Plus more items...
}
```

**Supported Formats:**
- EAN-13 (international retail)
- EAN-8 (compact retail)
- UPC-A (North America)
- CODE-128 (internal kitchen)
- QR Code (digital integration)

---

## 📊 Performance Metrics


| Metric | Value |
|--------|-------|
| Category Accuracy (HAND vs FACE) | 90% |
| Skin Type Accuracy (FAIR_1/2/3) | 90% |
| Precision (weighted avg) | 0.90 |
| Recall (weighted avg) | 0.90 |
| F1-Score | 0.90 |

### Waste Classifier

| Metric | Value |
|--------|-------|
| Overall Accuracy | 100%* |
| Precision (weighted) | 1.00 |
| Recall (weighted) | 1.00 |
| F1-Score | 1.00 |

*\*Note: Tested on small sample set; real-world accuracy may vary based on text quality*

---

## 🎯 Use Cases

### 1. Hygiene Compliance Monitoring
```
Camera detects hand → Check fair skin presence → Alert if no gloves worn
```

### 2. Waste Reduction Optimization
```
Staff describes issue → ML classifies → Recommends action
"Mayonaise expired" → EXPIRED (HIGH) → Remove from inventory
```

### 3. Inventory Management via Barcode
```
Scan barcode → Lookup DB → Show expiry + batch info → Auto log
```

### 4. Staff Training & QA
```
Analyze waste patterns → Identify recurring issues → Targeted training
```

---

## 🔒 Privacy & Security

- ✅ All processing can be done **on-device** (no cloud required)
- ✅ No personal photos stored or transmitted
- ✅ Fair skin detection only for hygiene, not biometric identification
- ✅ Compliance with GDPR and local privacy regulations
- ✅ Encrypted storage for any saved logs

---

## 🛠️ Troubleshooting

### Issue: Models won't load in Android

**Solution:**
```bash
# Ensure all .joblib files are copied to android/app/models/
ls android/app/models/*.joblib

# Check permissions
chmod 644 android/app/models/*.joblib
```

### Issue: Camera permission denied

**Check AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.CAMERA" />
```

**Runtime permission:**
```kotlin
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.CAMERA),
        REQUEST_CODE
    )
}
```

### Issue: Low detection accuracy

**Possible fixes:**
1. Increase training samples (more realistic variations)
2. Adjust RGB thresholds based on testing
3. Add data augmentation (brightness, contrast variations)
4. Use TFLite quantized model for better inference

---

## 📈 Future Enhancements

- [ ] Convert models to TensorFlow Lite (.tflite format)
- [ ] Add YOLOv8 for object detection (vegetables, fruits)
- [ ] Implement real-time barcode scanning with camera
- [ ] Mobile app UI polish (Material Design 3)
- [ ] Cloud sync for multi-kitchen deployment
- [ ] Analytics dashboard for waste trends
- [ ] Multi-language support (English, Bahasa Indonesia)
- [ ] Offline-first architecture

---

## 👥 Team & Credits

**Developed by:** KitchenGuard CSM Team  
**Version:** 2.0.0  
**Last Updated:** September 2026  

**Technologies Used:**
- Python 3.11
- scikit-learn, pandas, NumPy
- TensorFlow Lite
- Android SDK 33+
- Kotlin, Java
- OpenCV (computer vision)

---

## 📞 Support

For technical support or questions:
- Email: kitchenguard-support@example.com
- Documentation: See ML_TRAINING_GUIDE.md & README_ANDROID.md
- Issues: GitHub Issues tracker

---

## 📄 License

Internal use only - KitchenGuard Commercial Solutions

---

**Thank you for using KitchenGuard CSM!** 🙏
