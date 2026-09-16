# KitchenGuard CSM - Machine Learning Training Guide

Panduan lengkap untuk training dan deployment machine learning models di KitchenGuard CSM dengan fitur:
- **Skin Tone Detection** (Fair Skin/Orang Putih)
- **Waste Quality Classification**
- **Barcode Scanning Integration**
- **Android Studio Deployment**

## 📋 Prerequisites

### Requirements Software
- Python 3.8+
- Android Studio Arctic Fox atau lebih baru
- JDK 11+
- Gradle 7.0+

### Libraries yang Diperlukan

```bash
pip install numpy pandas scikit-learn matplotlib seaborn joblib pillow opencv-python
```

Untuk Android:
```gradle
// app/build.gradle
dependencies {
    implementation 'org.tensorflow:tensorflow-lite:2.12.0'
    implementation 'androidx.camera:camera-camera2:1.2.3'
    implementation 'androidx.camera:camera-lifecycle:1.2.3'
    implementation 'androidx.camera:camera-view:1.2.3'
}
```

## 🚀 Quick Start

### Step 1: Generate Datasets

Generate dataset untuk skin tone detection dan waste classification:

```bash
cd C:/CAPSTONE_MACHINE_LEARNING/src
python generate_skin_dataset.py
python generate_dataset.py
```

Output:
- `data/skin_detection_dataset.csv` - Dataset untuk fair skin detection
- `data/waste_quality_dataset_expanded.csv` - Expanded waste dataset
- `data/kitchenguard_waste_dataset.csv` - Original dataset

### Step 2: Train Models

#### A. Train Skin Detection Model

```bash
python train_skin_model.py
```

Output files:
```
models/
├── category_classifier.joblib        # Klasifikasi HAND vs FACE
├── skin_type_classifier.joblib       # Klasifikasi FAIR_1, FAIR_2, FAIR_3
├── category_encoder.joblib           # Encoder untuk categories
├── skin_type_encoder.joblib          # Encoder untuk skin types
├── skin_model_metadata.json          # Metadata model
├── android_encoding_map.json         # Mapping untuk Android
└── Android_SkinDetector.java         # Java helper class
```

#### B. Train Waste Classifier

```bash
python train_improved_waste_model.py
```

Output files:
```
models/
├── waste_classifier_tfidf.joblib     # TF-IDF vectorizer
├── waste_classifier_ensemble.joblib  # Voting ensemble classifier
├── label_encoder.joblib              # Label encoder
├── waste_classifier_metadata.json    # Metadata classifier
├── android_classification_map.json   # Mapping Android
└── WasteClassifierHelper.java        # Java helper
```

## 📊 Dataset Structure

### Skin Detection Dataset

```csv
image_id,category,skin_type,rgb_mean_r,rgb_mean_g,rgb_mean_b,...
SKIN_HAND_0001,HAND,FAIR_1,240.5,220.3,200.1,10.2,8.5,7.3,...
SKIN_FACE_0002,FACE,FAIR_2,235.2,215.8,195.4,9.8,7.9,6.8,...
```

**Categories:**
- `HAND` - Gambar tangan
- `FACE` - Gambar wajah

**Skin Types (Fair Skin):**
- `FAIR_1` - Sangat terang, mudah terbakar
- `FAIR_2` - Terang, cenderung terbakar
- `FAIR_3` - Terang dengan sedikit tan natural

### Waste Quality Dataset

```csv
text,category
"Ayam segar kondisi berbau busuk tidak sedap di chiller utama wajib segera dibuang",SPOILED
"Mayonaise telah lewat expired date 3 hari di refrigerated section",EXPIRED
...
```

**Kategori Limbah:**
1. `SPOILED` - Bahan kadaluwarsa/rusak
2. `EXPIRED` - Lewat expired date
3. `PREP_WASTE` - Sisa persiapan makanan
4. `OVERCOOKED` - Makanan overcook/terbakar
5. `CONTAMINATED` - Terkontaminasi
6. `SURPLUS` - Lebih/anjungan

## 🔧 Android Integration

### 1. Copy Models ke Android Project

```bash
# Salin model skiklt-learn ke folder Android
copy ..\..\models\*.joblib android\app\models\
```

### 2. Convert ke TensorFlow Lite (Opsional)

```python
# scripts/convert_to_tflite.py
import tensorflow as tf
from sklearn.externals import joblib

# Load trained model
model = joblib.load('models/category_classifier.joblib')

# Wrap dalam Function
@tf.function(input_signature=[tf.TensorSpec(shape=[None, 6], dtype=tf.float32)])
def predict(x):
    return model.predict(x)

converter = tf.lite.TFLiteConverter.from_concrete_functions(
    [predict.get_concrete_function()]
)
tflite_model = converter.convert

with open('models/skin_model.tflite', 'wb') as f:
    f.write(tflite_model)
```

### 3. Gunakan di MainActivity

Tambahkan di `MainActivity.kt`:

```kotlin
import com.kitchenguard.csm.utils.MachineLearningUtils

class MainActivity : AppCompatActivity() {
    
    private lateinit var mlUtils: MachineLearningUtils
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ML Utils
        try {
            mlUtils = MachineLearningUtils(this)
        } catch (e: Exception) {
            Log.e("ML", "Failed to load models", e)
        }
        
        // Example: Analyze waste description
        val wasteText = binding.txtDescription.text.toString()
        val wasteResult = mlUtils.analyzeWaste(wasteText)
        
        binding.resultText.text = wasteResult.toString()
        
        // Example: Detect skin from RGB values
        val r = 240f
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

### 4. Skin Detection Activity

Aktifkan activity skin detection dengan intent dari MainActivity:

```kotlin
val intent = Intent(this, SkinDetectionActivity::class.java)
startActivity(intent)
```

Atau tambahkan button di XML:

```xml
<Button
    android:id="@+id/btnSkinDetection"
    android:text="Skin Hygiene Check"
    android:onClick="openSkinDetection" />
```

```kotlin
fun openSkinDetection(view: View) {
    startActivity(Intent(this, SkinDetectionActivity::class.java))
}
```

## 🎯 Usage Examples

### Python - Manual Testing

```python
from train_skin_model import load_and_prepare_data

# Test single prediction
test_pixel = [[240, 220, 200, 10, 8, 7]]  # R, G, B mean + std

prediction = rf_category.predict(test_pixel)[0]
skin_pred = rf_skin.predict(test_pixel)[0]

print(f"Category: {category_encoder.inverse_transform([prediction])[0]}")
print(f"Skin Type: {skin_encoder.inverse_transform([skin_pred])[0]}")
```

### Android - Real-time Detection

```kotlin
// Capture frame from camera
val bitmap = captureCameraFrame()

// Process each pixel
for (y in 0 until bitmap.height) {
    for (x in 0 until bitmap.width) {
        val color = bitmap.getPixel(x, y)
        val r = Color.red(color).toFloat()
        val g = Color.green(color).toFloat()
        val b = Color.blue(color).toFloat()
        
        val result = mlUtils.detectSkinSimple(r, g, b)
        if (result.isFairSkinDetected) {
            // Highlight detected area
            drawHighlight(x, y, result.skinType)
        }
    }
}
```

## 📈 Performance Metrics

Setelah training, metrics akan ditampilkan:

### Skin Detection Model
```
Category Classifier Accuracy: 0.9850
Skin Type Classifier Accuracy: 0.9620
```

### Waste Classifier
```
Accuracy  : 0.9420
Precision : 0.9380
Recall    : 0.9350
F1-Score  : 0.9365
```

## 🔒 Data Privacy & Security

- Semua processing dapat dilakukan on-device
- Tidak ada data pribadi yang dikirim ke cloud
- Fair skin detection hanya untuk hygiene monitoring
- Compliance dengan GDPR dan local regulations

## 🐛 Troubleshooting

### Issue: Models tidak loading
```
Solution: Pastikan path ke models directory benar
```

### Issue: Camera permission denied
```kotlin
requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE)
```

### Issue: Memory allocation error
```
Solution: Reduksi image resolution atau gunakan quantized model
```

## 📝 License

KitchenGuard CSM Machine Learning - Internal Use Only

---

**Need Help?** Contact kitchenguard-support@example.com

**Version:** 2.0.0 | **Last Updated:** September 2026
