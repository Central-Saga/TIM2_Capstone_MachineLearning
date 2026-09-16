# 🦺 KitchenGuard CSM - Hygiene Detection Implementation (Q4 2026)

**Version**: 3.0  
**Target Completion**: Q4 2026  
**Priority**: ⭐⭐⭐⭐ (Industry Standard)  

---

## 📋 Executive Summary

Strategic reorientation from skin tone detection → **hygiene compliance detection** to meet F&B industry standards for food safety monitoring.

### Key Changes:
- ❌ **DEPRECATED**: Skin tone detection (not critical for core functionality)
- ✅ **NEW PRIORITY**: Glove, mask, hairnet compliance detection
- 🎯 **Use Case**: Real-time hygiene violation alerts in commercial kitchens

---

## Phase 1: Dataset Collection (Weeks 1-2)

### 1.1 Dataset Structure

```
hygiene_dataset/
├── gloves_worn/                          # Correct glove usage
│   ├── category_01_gloves_on_meat.jpg
│   ├── category_02_gloves_on_vegetables.jpg
│   ├── ... (500+ images minimum)
│   └── annotations.json
├── gloves_not_worn/                      # Violation - no gloves
│   ├── hands_direct_contact.jpg
│   └── ... (500+ images)
├── mask_compliant/                       # Correct mask coverage
│   ├── mask_over_nose_mouth.jpg
│   └── ... (500+ images)
├── mask_non_compliant/                   # Violations
│   ├── mask_under_chin.jpg
│   ├── mask_on_forehead.jpg
│   └── ... (500+ images)
├── hairnet_worn/                         # Correct usage
│   ├── full_hair_coverage.jpg
│   └── ... (500+ images)
└── hairnet_missing/                      # Violation
    ├── bare_head_exposed.jpg
    └── ... (500+ images)
```

### 1.2 Data Collection Requirements

**Minimum Targets:**
- Total: 5,000+ labeled images
- Per category: 500+ images
- Classes: 6 categories (above)

**Image Specifications:**
- Resolution: Minimum 640x480 pixels
- Formats: JPG, PNG
- Lighting: Varied (natural, fluorescent, dim)
- Angles: Multiple perspectives
- Subjects: Different staff members, ethnicities, ages

**Annotation Format (YOLO format):**
```json
{
  "image": "gloves_worn/category_01.jpg",
  "annotations": [
    {
      "class": "person",
      "bbox": [x_min, y_min, x_max, y_max],
      "attributes": {
        "gloves_worn": true,
        "glove_type": "disposable"
      }
    },
    {
      "class": "mask",
      "bbox": [...],
      "attributes": {
        "covering_nose": false,
        "covering_mouth": false
      }
    }
  ]
}
```

---

## Phase 2: Model Architecture Design

### 2.1 Recommended Approach: YOLOv8 Object Detection

**Why YOLOv8?**
- Real-time inference on mobile devices
- Multiple objects in single image
- Bounding box visualization
- High accuracy (>90% mAP expected)
- Pre-trained weights available

**Model Options:**
| Model | Parameters | Speed | Accuracy | Mobile Friendly |
|-------|-----------|-------|----------|-----------------|
| YOLOv8n | 3.2M | Fastest | Good | ✅ Best choice |
| YOLOv8s | 11M | Fast | Very Good | ✅ Recommended |
| YOLOv8m | 21M | Medium | Excellent | ⚠️ Larger model |
| YOLOv8x | 86M | Slow | State-of-art | ❌ Too heavy |

**Recommended: YOLOv8s** (balance of speed and accuracy)

### 2.2 Alternative: MobileNetV2 Custom Classifier

If object detection too complex initially:

**Architecture:**
```python
MobileNetV2(weights='imagenet', include_top=False, input_shape=(224, 224, 3))
├── GlobalAveragePooling2D()
├── Dropout(0.2)
├── Dense(128, activation='relu')
├── Dropout(0.2)
└── Dense(6, activation='softmax')  # 6 classes
```

---

## Phase 3: Training Pipeline

### 3.1 Python Training Script

Create file: `scripts/train_hygiene_yolov8.py`

```python
"""
Train YOLOv8 model for hygiene compliance detection
Targets: gloves, masks, hairnets
"""

import yaml
from ultralytics import YOLO
from ultralytics.engine.results import Results
from ultralytics.utils.plotting import Colors
import os

def prepare_dataset_config():
    """Create YOLO dataset configuration"""
    
    config = {
        'path': '../data/hygiene_dataset',  # Dataset root
        'train': 'train/images',             # Train images directory
        'val': 'valid/images',               # Validation images
        'test': '',                        # Test images (optional)
        
        # Number of classes
        'nc': 3,  # person (with hygiene attributes)
        
        # Class names - extended attributes will be detected
        'names': {
            0: 'person',
            1: 'gloves',
            2: 'mask',
            3: 'hairnet'
        }
    }
    
    with open('hygiene_dataset.yaml', 'w') as f:
        yaml.dump(config, f)
    
    return config['path']


def train_hygiene_model():
    """Train YOLOv8s hygiene detector"""
    
    # Load pre-trained YOLOv8s model
    print("Loading YOLOv8s pretrained weights...")
    model = YOLO('yolov8s.pt')
    
    # Configure training parameters
    training_params = {
        'data': 'hygiene_dataset.yaml',
        'epochs': 100,
        'batch': 16,
        'imgsz': 640,
        'device': '0',  # GPU ID (-1 for CPU)
        'optimizer': 'AdamW',
        'lr0': 0.01,
        'lrf': 0.01,
        'momentum': 0.937,
        'weight_decay': 0.05,
        'warmup_epochs': 3.0,
        'warmup_momentum': 0.8,
        'warmup_bias_lr': 0.1,
        'patience': 20,  # Early stopping after 20 epochs without improvement
        'save': True,
        'project': 'runs/detect',
        'name': 'hygiene_detector_v1',
        'exist_ok': False,
        'pretrained': True,
        'workers': 8,
        'seed': 42,
        # Augmentation parameters
        ' hsv_h': 0.015,
        ' hsv_s': 0.7,
        ' hsv_v': 0.4,
        'degrees': 0.0,
        'translate': 0.1,
        'scale': 0.5,
        'shear': 0.0,
        'perspective': 0.0,
        'flipud': 0.0,
        'fliplr': 0.5,
        'bgr': 0.0,
    }
    
    # Start training
    print("Starting training...")
    results = model.train(**training_params)
    
    # Save final model
    model.save('models/hygiene_detector_yolov8s.pt')
    print(f"\n✓ Model saved to models/hygiene_detector_yolov8s.pt")
    
    return results


def validate_model():
    """Validate trained model performance"""
    
    model = YOLO('runs/detect/hygiene_detector_v1/weights/best.pt')
    
    val_results = model.val(
        data='hygiene_dataset.yaml',
        batch=16,
        imgsz=640,
        split='val'  # Validate on validation set
    )
    
    print("\nValidation Results:")
    print(f"Precision: {results.box.map:.4f}")
    print(f"Recall: {results.box.map50:.4f}")
    print(f"F1 Score: {results.box.map50_95:.4f}")
    
    # Return results dictionary
    return {
        'precision': float(results.box.map),
        'recall': float(results.box.map50),
        'map50_95': float(results.box.map50_95)
    }


def export_to_tflite():
    """Convert model to TFLite for Android deployment"""
    
    model = YOLO('runs/detect/hygiene_detector_v1/weights/best.pt')
    
    # Export to TFLite
    model.export(format='tflite', imgsz=640)
    
    print(f"✓ Model exported to TFLite format")
    

if __name__ == "__main__":
    # Step 1: Prepare dataset config
    dataset_path = prepare_dataset_config()
    print(f"Dataset configured at: {dataset_path}")
    
    # Step 2: Train model
    train_hygiene_model()
    
    # Step 3: Validate
    metrics = validate_model()
    
    # Step 4: Export (after validation passes)
    export_to_tflite()
    
    print("\n✓ Training pipeline completed successfully!")
```

---

## Phase 4: Backend API Integration

### 4.1 Create Hygiene Detection Endpoint

File: `app.py` - Add new endpoint section:

```python
# =============================================================================
# HYGIENE DETECTION ENDPOINTS
# =============================================================================

@app.post("/api/hygiene/detect", tags=["Hygiene Detection"])
async def detect_hygiene_violations(request: Request):
    """
    Detect hygiene violations from uploaded kitchen worker images
    
    Endpoints for:
    - Camera feed analysis (real-time)
    - Photo upload (batch processing)
    - Continuous monitoring alerts
    
    Returns:
    - List of detected violations
    - Confidence scores per violation type
    - Compliance percentage
    - Risk assessment
    """
    try:
        start_time = time.time()
        
        # Handle multipart form data (image upload)
        if request.headers.get("Content-Type") == "multipart/form-data":
            image_data = await request.form()
            image_bytes = image_data["image"]
        else:
            # Assume JSON with base64 image
            json_data = await request.json()
            image_b64 = json_data.get("image_base64", "")
            image_bytes = base64.b64decode(image_b64)
        
        # Load PIL Image
        from PIL import Image
        import io
        
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        
        # Run detection using YOLO model
        results = hygiene_model.predict(
            image, 
            conf=0.45,  # Minimum confidence threshold
            imgsz=640,
            device='cpu'  # or 'cuda' if available
        )[0]
        
        # Parse results for violations
        violations = []
        compliance_scores = {}
        
        # Process each detection
        for box in results.boxes:
            cls_id = int(box.cls[0])
            confidence = float(box.conf[0])
            bbox = box.xyxy[0].tolist()
            
            class_name = results.names[cls_id]
            
            # Determine violation based on context and attributes
            violation = analyze_violation(
                class_name=class_name,
                confidence=confidence,
                bbox=bbox,
                image_size=image.size
            )
            
            if violation:
                violations.append(violation)
        
        # Calculate overall compliance
        total_detected = len(violations) + len(compliance_scores.get("compliant", 0))
        compliant_count = compliance_scores.get("compliant", total_detected - total_violations)
        compliance_rate = compliant_count / total_detected if total_detected > 0 else 1.0
        
        # Risk assessment
        risk_level = assess_risk(violations)
        
        processing_time = (time.time() - start_time) * 1000
        
        return {
            "success": True,
            "timestamp": datetime.now().isoformat(),
            "data": {
                "violations": violations,
                "compliance_score": compliance_rate,
                "risk_level": risk_level,
                "processing_time_ms": round(processing_time, 2)
            }
        }
        
    except Exception as e:
        logger.error(f"Hypiene detection error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


def analyze_violation(class_name: str, confidence: float, bbox: list, image_size: tuple) -> dict:
    """Analyze specific detection for violations"""
    
    # Context-aware violation detection
    # This is simplified - actual implementation needs attribute detection
    
    if class_name == "gloves":
        if confidence < 0.7:  # No gloves detected
            return {
                "type": "NO_GLOVES",
                "confidence": confidence,
                "bbox": bbox,
                "severity": "HIGH",
                "message": "Gloves not worn during food handling"
            }
    
    elif class_name == "mask":
        if confidence < 0.7:  # No mask detected
            return {
                "type": "NO_MASK",
                "confidence": confidence,
                "bbox": bbox,
                "severity": "CRITICAL",
                "message": "Mask not covering nose/mouth"
            }
    
    elif class_name == "hairnet":
        if confidence < 0.7:  # No hairnet detected
            return {
                "type": "NO_HAIRNET",
                "confidence": confidence,
                "bbox": bbox,
                "severity": "MEDIUM",
                "message": "Hair not properly covered"
            }
    
    return None


def assess_risk(violations: list) -> str:
    """Assess overall risk level based on violations"""
    
    critical_count = sum(1 for v in violations if v.get("severity") == "CRITICAL")
    high_count = sum(1 for v in violations if v.get("severity") == "HIGH")
    
    if critical_count > 0:
        return {"level": "CRITICAL", "action": "Immediate intervention required"}
    elif high_count >= 2:
        return {"level": "HIGH", "action": "Staff reminder needed urgently"}
    elif high_count > 0 or len(violations) > 1:
        return {"level": "MEDIUM", "action": "Schedule refresher training"}
    else:
        return {"level": "LOW", "action": "Continue monitoring"}
```

---

## Phase 5: Android Integration

### 5.1 Kotlin Implementation

File: `app/src/main/java/com/kitchenguard/ui/HygieneMonitorActivity.kt`

```kotlin
package com.kitchenguard.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.DetectedObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HygieneMonitorActivity : AppCompatActivity() {
    
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hygiene_monitor)
        
        checkPermissions()
        setupCamera()
        setupPreview()
    }
    
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO  // For future audio alerts
        )
        
        val remainingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (remainingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                remainingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.apply(applicationContext)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
            
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .build()
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun captureAndAnalyze() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onSuccess(output: ImageCapture.OutputImageCaptureOptions) {
                    // Image captured successfully
                    showToast("Image captured, analyzing hygiene compliance...")
                    
                    // Send to backend for analysis
                    analyzeWithBackend(output.imageProxy)
                }
                
                override fun onError(exception: ImageCapture.ImageCaptureException) {
                    showToast("Capture failed: ${exception.message}")
                }
            }
        )
    }
    
    private fun analyzeWithBackend(imageProxy: ImageProxy) {
        // Convert ImageProxy to bitmap
        val bitmap = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyFromImageProxy(imageProxy)
        
        // Encode to base64
        val base64Image = bitmapToBase64(bitmap)
        
        // Call backend API
        viewModel.analyzeHygiene(base64Image) { result ->
            result.onSuccess { analysis ->
                displayViolationAlerts(analysis)
            }.onFailure { error ->
                showToast("Analysis failed: ${error.message}")
            }
        }
    }
    
    private fun displayViolationAlerts(hygieneData: HygieneAnalysisData) {
        binding.root.visibility = View.VISIBLE
        
        // Show visual alerts for each violation
        hygieneData.violations.forEach { violation ->
            showViolationAlert(
                type = violation.type,
                severity = violation.severity,
                message = violation.message
            )
        }
        
        // Update compliance score
        binding.tvComplianceScore.text = 
            String.format("%.1f%%", hygieneData.complianceScore * 100)
        
        // Color code compliance score
        when (hygieneData.riskLevel) {
            "CRITICAL" -> binding.tvRiskLevel.setTextColor(Color.RED)
            "HIGH" -> binding.tvRiskLevel.setTextColor(Color orange)
            else -> binding.tvRiskLevel.setTextColor(Color.GREEN)
        }
    }
    
    private fun showViolationAlert(type: String, severity: String, message: String) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("$severity Violation Detected!")
            .setMessage(message)
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Dismiss") { _, _ -> }
            .create()
        
        alertDialog.show()
        
        // Vibrate for immediate attention
        vibrator?.vibrate(500)
        
        // Optional: Play sound alert
        playAlertSound()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}
```

---

## Timeline & Milestones

| Week | Task | Deliverable | Owner |
|------|------|-------------|-------|
| 1-2 | Dataset Collection | 5,000+ labeled images | Data Team |
| 3-4 | Annotation | YOLO-format labels | Annotation Team |
| 5-6 | Model Training | YOLOv8s checkpoint | ML Engineer |
| 7 | Model Validation | ≥90% mAP @ IoU 0.5 | ML Engineer |
| 8 | Backend Integration | `/api/hygiene/detect` API | Backend Team |
| 9 | Android SDK | TFLite inference module | Android Team |
| 10 | Integration Testing | End-to-end system test | QA Team |
| 11 | UAT | User Acceptance Testing | Business Team |
| 12 | Production Deploy | Live deployment | DevOps |

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Detection Accuracy | ≥90% mAP | Validation set |
| Inference Speed | <50ms/image | Mobile device |
| False Positive Rate | <5% | Production logs |
| Real-time Performance | 30 FPS | Camera feed |
| Compliance Alert Accuracy | ≥85% | Human verification |

---

## Next Steps

1. ✅ **Approve budget** for dataset collection team
2. ✅ **Start recruitment** for annotation workers
3. ✅ **Procure hardware** (cameras, lighting)
4. ✅ **Begin pilot** at one restaurant location
5. 🗓️ **Track progress** weekly milestones

---

**Documentation Version**: 3.0  
**Status**: Ready for Q4 2026 Implementation  
**KitchenGuard CSM - Hygiene Detection Roadmap Complete**
