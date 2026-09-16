# Skin Tone Detection Model Evaluation Report

## Model Information
- Version: 1.0.0
- Created: 2026-09-16 09:57:29.285034
- Total Samples: 3600
- Features Used: 6

## Performance Metrics
### Category Classification (HAND vs FACE)
- Accuracy: 0.5014

### Skin Type Classification (Fair Skin Types)
- Accuracy: 0.7292

## Categories Detected
- FACE
- HAND

## Skin Types (Fair/White)
- FAIR_1: Sangat terang, mudah terbakar sinar matahari
- FAIR_2: Terang, cenderung terbakar
- FAIR_3: Terang dengan sedikit tan natural

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
