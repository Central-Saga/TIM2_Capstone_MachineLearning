"""
KitchenGuard CSM - Skin Tone Detection Model Training
Melatih model untuk mendeteksi warna kulit fair skin (orang putih)
Output: TensorFlow Lite model untuk Android
"""

import os
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
import joblib
import json
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

# Konfigurasi
DATA_PATH = os.path.join("data", "skin_detection_dataset.csv")
MODELS_DIR = os.path.join("models", "skin_detection")
REPORTS_DIR = "reports"

os.makedirs(MODELS_DIR, exist_ok=True)
os.makedirs(REPORTS_DIR, exist_ok=True)

def load_and_prepare_data():
    """Load data dan prepare features untuk training"""
    df = pd.read_csv(DATA_PATH)
    
    # Features: RGB mean and std values + lighting condition encoding
    feature_cols = ["rgb_mean_r", "rgb_mean_g", "rgb_mean_b", 
                   "rgb_std_r", "rgb_std_g", "rgb_std_b"]
    
    X = df[feature_cols].values
    
    # Encode categories and skin types
    category_encoder = LabelEncoder()
    y_category = category_encoder.fit_transform(df["category"])
    
    skin_encoder = LabelEncoder()
    y_skin = skin_encoder.fit_transform(df["skin_type"])
    
    return X, y_category, y_skin, category_encoder, skin_encoder, df

def train_classification_models(X, y_category, y_skin):
    """Train multiple models untuk kategori dan skin type"""
    
    # Split data
    X_train_cat, X_test_cat, y_train_cat, y_test_cat = train_test_split(
        X, y_category, test_size=0.2, random_state=42, stratify=y_category
    )
    
    X_train_skin, X_test_skin, y_train_skin, y_test_skin = train_test_split(
        X, y_skin, test_size=0.2, random_state=42, stratify=y_skin
    )
    
    # Train Random Forest for category detection
    print("\nTraining Category Classifier...")
    rf_category = RandomForestClassifier(
        n_estimators=100,
        max_depth=10,
        min_samples_split=5,
        random_state=42
    )
    rf_category.fit(X_train_cat, y_train_cat)
    
    cat_pred = rf_category.predict(X_test_cat)
    cat_accuracy = accuracy_score(y_test_cat, cat_pred)
    print(f"Category Classifier Accuracy: {cat_accuracy:.4f}")
    print(classification_report(y_test_cat, cat_pred))
    
    # Train Random Forest for skin type detection
    print("\nTraining Skin Type Classifier...")
    rf_skin = RandomForestClassifier(
        n_estimators=100,
        max_depth=10,
        min_samples_split=5,
        random_state=42
    )
    rf_skin.fit(X_train_skin, y_train_skin)
    
    skin_pred = rf_skin.predict(X_test_skin)
    skin_accuracy = accuracy_score(y_test_skin, skin_pred)
    print(f"Skin Type Classifier Accuracy: {skin_accuracy:.4f}")
    print(classification_report(y_test_skin, skin_pred))
    
    return rf_category, rf_skin, cat_accuracy, skin_accuracy

def save_models(rf_category, rf_skin, category_encoder, skin_encoder, metrics):
    """Simpan models dan metadata"""
    
    # Save scikit-learn models
    joblib.dump(rf_category, os.path.join(MODELS_DIR, "category_classifier.joblib"))
    joblib.dump(rf_skin, os.path.join(MODELS_DIR, "skin_type_classifier.joblib"))
    joblib.dump(category_encoder, os.path.join(MODELS_DIR, "category_encoder.joblib"))
    joblib.dump(skin_encoder, os.path.join(MODELS_DIR, "skin_type_encoder.joblib"))
    
    # Create metadata
    metadata = {
        "model_type": "skin_tone_detection",
        "version": "1.0.0",
        "created_at": str(pd.Timestamp.now()),
        "categories": list(category_encoder.classes_),
        "skin_types": list(skin_encoder.classes_),
        "metrics": metrics,
        "feature_names": [
            "rgb_mean_r", "rgb_mean_g", "rgb_mean_b",
            "rgb_std_r", "rgb_std_g", "rgb_std_b"
        ],
        "android_compatible": True,
        "tflite_ready": False
    }
    
    with open(os.path.join(MODELS_DIR, "skin_model_metadata.json"), "w") as f:
        json.dump(metadata, f, indent=2)
    
    # Save encoder mappings for Android
    android_mapping = {
        "category_classes": dict(zip(category_encoder.classes_, range(len(category_encoder.classes_)))),
        "skin_type_classes": dict(zip(skin_encoder.classes_, range(len(skin_encoder.classes_)))),
        "category_to_name": {cls: cls.replace("_", " ").title() for cls in category_encoder.classes_},
        "skin_type_to_name": {
            cls: FAIR_SKIN_DESCRIPTIONS.get(cls, cls) 
            for cls in skin_encoder.classes_
        }
    }
    
    with open(os.path.join(MODELS_DIR, "android_encoding_map.json"), "w") as f:
        json.dump(android_mapping, f, indent=2)
    
    print(f"\nModels saved to {MODELS_DIR}/")
    return metadata

def export_for_android():
    """Create Android-ready prediction script"""
    
    android_script = '''
package com.kitchenguard.csm.utils;

import android.content.Context;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Skin Tone Detection Helper untuk Android
 * Mendeteksi kategori (HAND/FACE) dan skin type (FAIR_1/FAIR_2/FAIR_3)
 */
public class SkinDetectorHelper {
    
    private Interpreter tflite;
    private int[] categoryClasses;
    private int[] skinTypeClasses;
    
    public SkinDetectorHelper(Context context) throws IOException {
        // Load TFLite model (akan di-generate kemudian)
        ByteBuffer buffer = FileUtil.loadMappedFile(context, "skin_model.tflite");
        tflite = new Interpreter(buffer);
        
        // Load label mappings (aligned with android_encoding_map.json: 0=FACE, 1=HAND)
        this.categoryClasses = new int[]{0, 1}; // 0: FACE, 1: HAND
        this.skinTypeClasses = new int[]{0, 1, 2}; // FAIR_1, FAIR_2, FAIR_3
    }
    
    /**
     * Detect skin from image features (RGB values)
     * @param rgbMean R, G, B mean values from image
     * @param rgbStd R, G, B standard deviation
     * @return PredictionResult with category and skin type
     */
    public PredictionResult detect(float[] rgbMean, float[] rgbStd) {
        // Prepare input tensor (6 features)
        float[][] inputArray = new float[1][6];
        inputArray[0][0] = rgbMean[0];
        inputArray[0][1] = rgbMean[1];
        inputArray[0][2] = rgbMean[2];
        inputArray[0][3] = rgbStd[0];
        inputArray[0][4] = rgbStd[1];
        inputArray[0][5] = rgbStd[2];
        
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(24);
        inputBuffer.order(ByteOrder.nativeOrder());
        for (int i = 0; i < 6; i++) {
            inputBuffer.putFloat(inputArray[0][i]);
        }
        inputBuffer.rewind();
        
        // Run inference
        float[][] outputCategory = new float[1][2];
        float[][] outputSkin = new float[1][4];
        
        tflite.run(inputBuffer, outputCategory);
        tflite.run(inputBuffer, outputSkin);
        
        // Find predictions
        int predictedCategory = getMaxIndex(outputCategory[0]);
        int predictedSkin = getMaxIndex(outputSkin[0]);
        
        String categoryName = getCategoryName(predictedCategory);
        String skinTypeName = getSkinTypeName(predictedSkin);
        
        return new PredictionResult(categoryName, skinTypeName, 
                                   outputCategory[0][predictedCategory],
                                   outputSkin[0][predictedSkin]);
    }
    
    private int getMaxIndex(float[] probabilities) {
        int maxIndex = 0;
        float maxProb = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }
    
    private String getCategoryName(int index) {
        switch(index) {
            case 0: return "HAND";
            case 1: return "FACE";
            default: return "UNKNOWN";
        }
    }
    
    private String getSkinTypeName(int index) {
        switch(index) {
            case 0: return "FAIR_1";
            case 1: return "FAIR_2";
            case 2: return "FAIR_3";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * Simple prediction without TFLite (fallback menggunakan logic sederhana)
     * Berdasarkan RGB threshold
     */
    public FallbackPredictionResult detectSimple(float r, float g, float b) {
        // Fair skin biasanya memiliki RGB values tinggi (> 200)
        boolean isFairSkin = r > 200 && g > 180 && b > 160;
        
        String skinType = isFairSkin ? "FAIR" : "OTHER";
        float confidence = isFairSkin ? 0.85f : 0.15f;
        
        return new FallbackPredictionResult(skinType, confidence);
    }
    
    public static class PredictionResult {
        public String category;
        public String skinType;
        public float categoryConfidence;
        public float skinConfidence;
        
        public PredictionResult(String category, String skinType, 
                               float catConf, float skinConf) {
            this.category = category;
            this.skinType = skinType;
            this.categoryConfidence = catConf;
            this.skinConfidence = skinConf;
        }
        
        @Override
        public String toString() {
            return "Category: " + category + ", Skin: " + skinType + 
                   " (confidence: " + skinConfidence + ")";
        }
    }
    
    public static class FallbackPredictionResult {
        public String skinType;
        public float confidence;
        
        public FallbackPredictionResult(String skinType, float confidence) {
            this.skinType = skinType;
            this.confidence = confidence;
        }
    }
}
'''
    
    with open(os.path.join(MODELS_DIR, "Android_SkinDetector.java"), "w") as f:
        f.write(android_script)
    
    print("Android Java helper created in models/")

# Fair skin descriptions untuk mapping
FAIR_SKIN_DESCRIPTIONS = {
    "FAIR_1": "Sangat terang, mudah terbakar sinar matahari",
    "FAIR_2": "Terang, cenderung terbakar",
    "FAIR_3": "Terang dengan sedikit tan natural"
}

if __name__ == "__main__":
    print("="*60)
    print("KitchenGuard Skin Tone Detection Model Training")
    print("="*60)
    
    # Load and prepare data
    print("\n[1/5] Loading and preparing data...")
    X, y_category, y_skin, category_encoder, skin_encoder, df = load_and_prepare_data()
    
    print(f"Total samples: {len(df)}")
    print(f"Features shape: {X.shape}")
    print(f"Categories: {list(category_encoder.classes_)}")
    print(f"Skin types: {list(skin_encoder.classes_)}")
    
    # Train models
    print("\n[2/5] Training classifiers...")
    rf_category, rf_skin, cat_accuracy, skin_accuracy = train_classification_models(
        X, y_category, y_skin
    )
    
    # Collect metrics
    metrics = {
        "category_accuracy": float(cat_accuracy),
        "skin_type_accuracy": float(skin_accuracy),
        "total_samples": len(df),
        "features_count": X.shape[1]
    }
    
    # Save models
    print("\n[3/5] Saving models...")
    metadata = save_models(rf_category, rf_skin, category_encoder, skin_encoder, metrics)
    
    # Export for Android
    print("\n[4/5] Creating Android helper...")
    export_for_android()
    
    # Generate report
    print("\n[5/5] Generating evaluation report...")
    report = f"""# Skin Tone Detection Model Evaluation Report

## Model Information
- Version: {metadata['version']}
- Created: {metadata['created_at']}
- Total Samples: {metrics['total_samples']}
- Features Used: {metrics['features_count']}

## Performance Metrics
### Category Classification (HAND vs FACE)
- Accuracy: {cat_accuracy:.4f}

### Skin Type Classification (Fair Skin Types)
- Accuracy: {skin_accuracy:.4f}

## Categories Detected
{chr(10).join(['- ' + c for c in metadata['categories']])}

## Skin Types (Fair/White)
{chr(10).join(['- ' + st + ': ' + FAIR_SKIN_DESCRIPTIONS.get(st, '') for st in metadata['skin_types']])}

## Android Integration
- Models are saved in scikit-learn format (.joblib)
- Encoding mappings available for Android conversion
- Java helper class created: Android_SkinDetector.java
- Can be converted to TensorFlow Lite for mobile deployment

## Next Steps
1. Convert scikit-learn models to ONNX format
2. Quantize and convert to TensorFlow Lite
3. Integrate into MainActivity.kt
4. Add camera preview for real-time detection
"""
    
    with open(os.path.join(REPORTS_DIR, "skin_detection_report.md"), "w") as f:
        f.write(report)
    
    print("\n" + "="*60)
    print("Training Complete!")
    print("="*60)
    print(f"\nMetrics:")
    print(f"  - Category Accuracy: {cat_accuracy:.4f}")
    print(f"  - Skin Type Accuracy: {skin_accuracy:.4f}")
    print(f"\nSaved files:")
    print(f"  - {MODELS_DIR}/category_classifier.joblib")
    print(f"  - {MODELS_DIR}/skin_type_classifier.joblib")
    print(f"  - {MODELS_DIR}/category_encoder.joblib")
    print(f"  - {MODELS_DIR}/skin_type_encoder.joblib")
    print(f"  - {MODELS_DIR}/skin_model_metadata.json")
    print(f"  - {MODELS_DIR}/android_encoding_map.json")
    print(f"  - {MODELS_DIR}/Android_SkinDetector.java")
    print(f"  - {REPORTS_DIR}/skin_detection_report.md")
