# KitchenGuard CSM - Android Integration Guide

Panduan lengkap untuk deploy machine learning models ke aplikasi Android dengan fitur:
- ✅ Skin Tone Detection (Fair Skin/Orang Putih)
- ✅ Waste Quality Classification  
- ✅ Barcode Scanning
- ✅ Camera Preview Real-time

## 📱 Project Structure

```
android/app/src/main/java/com/kitchenguard/csm/
├── MainActivity.kt                 # Main activity with barcode & vision scan
├── SkinDetectionActivity.kt       # Skin detection activity
├── utils/
│   ├── MachineLearningUtils.java  # ML inference helper
│   ├── SkinDetectorHelper.java    # Skin detector utilities
│   └── WasteClassifierHelper.java # Waste classifier utilities
├── model/
│   └── Models.kt                   # Data models
└── network/
    └── ApiService.kt               # Network service

res/layout/
├── activity_main.xml              # Main UI layout
└── activity_skin_detection.xml    # Skin detection UI
```

## 🚀 Setup & Installation

### Prerequisites

1. **Android Studio** (Arctic Fox atau lebih baru)
2. **JDK 11+** 
3. **Gradle 7.0+**
4. **Python 3.8+** (untuk training models)

### Step 1: Build Models First

Sebelum build Android, jalankan training di Python:

```bash
cd C:\CAPSTONE_MACHINE_LEARNING
run_training.bat
```

Ini akan generate:
- `models/skin_type_classifier.joblib`
- `models/waste_classifier_ensemble.joblib`
- Java helper classes

### Step 2: Copy Models ke Android

```bash
# Salin semua files dari models folder ke Android
xcopy ..\models\*.joblib android\app\models\ /E /I /Y
xcopy ..\models\*.json android\app\models\ /E /I /Y
```

Atau manual copy ke: `android/app/models/`

### Step 3: Update Dependencies

Pastikan `android/app/build.gradle` sudah ada dependencies ini:

```gradle
dependencies {
    // TensorFlow Lite untuk ML
    implementation 'org.tensorflow:tensorflow-lite:2.12.0'
    implementation 'org.tensorflow:tensorflow-lite-support:0.4.3'
    
    // CameraX untuk camera preview
    def camerax_version = "1.2.3"
    implementation "androidx.camera:camera-camera2:$camerax_version"
    implementation "androidx.camera:camera-lifecycle:$camerax_version"
    implementation "androidx.camera:camera-view:$camerax_version"
    
    // Other dependencies
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // UI Components
    implementation 'com.google.android.material:material:1.8.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
}
```

### Step 4: Sync Gradle

Di Android Studio:
1. Klik **Sync Now** yang muncul di toolbar
2. Atau **File → Sync Project with Gradle Files**
3. Tunggu hingga build selesai tanpa error

### Step 5: Build & Run

1. Sambungkan device Android atau buka emulator
2. Klik **Run** (▶️ button) atau **Shift+F10**
3. Pilih device target

## 🎯 Fitur Utama

### 1. Skin Detection (Fair Skin/Orang Putih)

**Activity:** `SkinDetectionActivity.kt`

Fungsi utama:
- Detect fair skin secara real-time dari camera feed
- Klasifikasi skin type: FAIR_1, FAIR_2, FAIR_3
- Hygiene monitoring dan safety compliance
- Highlight area kulit terdeteksi

Cara pakai:
```kotlin
// Start skin detection from MainActivity
val intent = Intent(this, SkinDetectionActivity::class.java)
startActivity(intent)
```

**Algorithm:**
```java
// MachineLearningUtils.java
public SkinDetectionResult detectSkinSimple(float r, float g, float b) {
    // Fair skin thresholds dari training data
    boolean isFairSkin = r >= 200f && g >= 180f && b >= 160f;
    
    if (!isFairSkin) return createNegativeResult();
    
    // Classify berdasarkan intensity
    String skinType = determineSkinType(r, g, b);
    float confidence = calculateConfidence(...);
    
    return new SkinDetectionResult(detected, skinType, confidence);
}
```

**RGB Thresholds untuk Fair Skin:**
- R ≥ 200
- G ≥ 180  
- B ≥ 160

**Skin Type Categories:**
- **FAIR_1**: RGB avg > 230 (Sangat terang)
- **FAIR_2**: RGB avg 210-230 (Terang)
- **FAIR_3**: RGB avg 200-210 (Sedikit tan)

### 2. Waste Quality Classification

**Helper:** `MachineLearningUtils.analyzeWaste(String text)`

Fungsi utama:
- Deteksi kategori limbah dari deskripsi teks
- Keyword-based matching untuk kontaminasi/rusak/expired
- Rekomendasi action berdasarkan severity
- Hygiene level indicator

Usage example:
```kotlin
val wasteText = binding.txtDescription.text.toString()
val result = mlUtils.analyzeWaste(wasteText)

binding.resultText.text = result.hygieneLevel
binding.actionText.text = result.recommendedAction
```

**Kategori Limbah:**
| Category | Hygiene Level | Example Description |
|----------|--------------|---------------------|
| CONTAMINATED | CRITICAL | "terkontaminasi hair", "jatuh ke lantai" |
| SPOILED | HIGH | "berjamur", "berbau busuk" |
| EXPIRED | HIGH | "expired date", "lewat MHD" |
| OVERCOOKED | MEDIUM | "gosong", "hangus" |
| PREP_WASTE | LOW | "trimming waste", "peeling" |
| SURPLUS | LOW | "tidak terjual", "surplus" |

### 3. Barcode Scanning

**Service:** `barcode_service.py` + `MainActivity.performBarcodeScan()`

Format yang didukung:
- EAN-13 (8991234567890)
- EAN-8
- UPC-A
- CODE-128
- QR Code

Contoh database barcode:
```python
BARCODE_DATABASE = {
    "8991234567890": {
        "name": "Daging Sapi Wagyu Ribeye MB7",
        "category": "Daging & Unggas",
        "batch_id": "WG-0915-A",
        "expiry_at": "2026-09-22",
        ...
    },
    ...
}
```

## 🔧 Customization

### Update Skin Detection Thresholds

Edit `MachineLearningUtils.java`:

```java
private static final int FAIR_SKIN_MIN_R = 200;  // Default
private static final int FAIR_SKIN_MIN_G = 180;  // Default
private static final int FAIR_SKIN_MIN_B = 160;  // Default
```

Untuk adjust sensitivity:
- Naikkan threshold untuk lebih strict (kurang false positives)
- Turunkan threshold untuk lebih sensitive (lebih detections)

### Add New Waste Keywords

Edit `calculateKeywordScore()` method:

```java
// Add custom keyword set
float moldyScore = calculateKeywordScore(cleanText, new String[]{
    "bentuk bulu", "kapang", "jamur hitam", "berbulu putih"
});
scores.put("MOLDY", moldyScore);
```

### Customize UI Colors

Edit `android/app/src/main/res/values/colors.xml`:

```xml
<color name="orange_primary">#FF9800</color>
<color name="background_white">#FFFFFF</color>
<color name="text_primary">#333333</color>
<color name="text_secondary">#666666</color>
```

## 📊 Testing

### Unit Test untuk ML Utilities

Buat test file: `android/app/src/test/java/com/kitchenguard/csm/utils/MachineLearningUtilsTest.java`

```java
@Test
public void testDetectFairSkin() {
    MachineLearningUtils mlUtils = new MachineLearningUtils(context);
    
    // Test FAIR_1
    SkinDetectionResult result1 = mlUtils.detectSkinSimple(240f, 220f, 200f);
    assertEquals(true, result1.isFairSkinDetected);
    assertEquals("FAIR_1", result1.skinType);
    
    // Test non-fair skin
    SkinDetectionResult result2 = mlUtils.detectSkinSimple(150f, 120f, 100f);
    assertEquals(false, result2.isFairSkinDetected);
}

@Test
public void testAnalyzeWasteContaminated() {
    MachineLearningUtils mlUtils = new MachineLearningUtils(context);
    
    WasteAnalysisResult result = mlUtils.analyzeWaste(
        "Salad terkontaminasi hair di plating area"
    );
    
    assertEquals("CONTAMINATED", result.category);
    assertTrue(result.hygieneLevel.contains("CRITICAL"));
}
```

### Manual Testing Steps

1. **Skin Detection Test:**
   - Buka SkinDetectionActivity
   - Tunjukkan tangan atau wajah dengan lighting cukup
   - Press "DETECT SKIN NOW"
   - Verify hasilnya FAIR_1/FAIR_2/FAIR_3

2. **Waste Classification Test:**
   ```
   Test case 1: "Ayam segar berbau busuk di chiller"
   Expected: SPOILED (HIGH priority)
   
   Test case 2: "Mayonaise expired date kemarin"
   Expected: EXPIRED (HIGH priority)
   
   Test case 3: "Potongan wortel trimming prep station"
   Expected: PREP_WASTE (LOW priority)
   ```

3. **Barcode Scan Test:**
   ```
   Test code: 8991234567890
   Expected: Daging Sapi Wagyu Ribeye MB7
   ```

## 🐛 Troubleshooting

### Error: "No such file: skin_model.tflite"

**Solution:** Pastikan file model sudah disalin ke assets folder:
```bash
copy ..\..\..\models\skin_model.tflite android\app\src\main\assets\
```

### Error: Camera permission denied

**Solution:** Check AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.CAMERA" />
```

Dan request permission di runtime:
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

### Performance: Slow detection

**Solution:** 
1. Reduce image resolution di camera preview
2. Gunakan quantized model (.tflite dengan INT8)
3. Limit pixel processing (skip every N pixels)

### Memory Issue

**Solution:** Recycle bitmaps setelah used:
```kotlin
bitmap.recycle()
bitmap = null
System.gc() // Force garbage collection
```

## 📝 Deployment Checklist

- [ ] Training completed successfully (check accuracy metrics)
- [ ] All .joblib files copied to Android/models/
- [ ] Java helper classes in place
- [ ] Permissions declared in AndroidManifest.xml
- [ ] Camera permission requested at runtime
- [ ] Skin detection tested on physical device
- [ ] Waste classification accuracy verified
- [ ] Barcode scanning functional
- [ ] UI responsive dan smooth
- [ ] No crash reports
- [ ] Privacy policy updated (mention ML usage)

## 📞 Support

**Technical Support:** kitchenguard-android@example.com

**Documentation:** See ML_TRAINING_GUIDE.md untuk Python training details

---

**Version:** 2.0.0  
**Last Updated:** September 2026
