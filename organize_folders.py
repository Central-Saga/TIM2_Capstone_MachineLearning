#!/usr/bin/env python3
"""
KitchenGuard CSM - Folder Organization Script
Membuat struktur folder yang rapi dan sistematis
"""

import os
import shutil
from pathlib import Path

# Define base directory
BASE_DIR = Path(__file__).parent.resolve()

print("="*70)
print("📁 KitchenGuard CSM - Folder Organization")
print("="*70)

def create_folder(path):
    """Create folder if not exists"""
    path.mkdir(parents=True, exist_ok=True)
    print(f"✓ Created: {path.relative_to(BASE_DIR)}")

def move_file(src, dest, reason=""):
    """Move file with confirmation"""
    src_path = BASE_DIR / src
    dest_path = BASE_DIR / dest
    
    if src_path.exists():
        dest_path.parent.mkdir(parents=True, exist_ok=True)
        
        # Handle duplicate names
        counter = 1
        final_dest = dest_path
        while final_dest.exists():
            name, ext = final_dest.name.rsplit('.', 1) if '.' in final_dest.name else (final_dest.name, '')
            final_dest = dest_path.parent / f"{name}_v{counter}{ext}"
            counter += 1
        
        shutil.move(str(src_path), str(final_dest))
        print(f"✓ Moved: {src} → {dest}")
        if reason:
            print(f"   Reason: {reason}")
        return True
    else:
        print(f"⊘ Skipped (not found): {src}")
        return False

def copy_file(src, dest, reason=""):
    """Copy file to destination"""
    src_path = BASE_DIR / src
    dest_path = BASE_DIR / dest
    
    if src_path.exists():
        dest_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(str(src_path), str(dest_path))
        print(f"✓ Copied: {src} → {dest}")
        if reason:
            print(f"   Reason: {reason}")
        return True
    return False


# ===========================================
# STEP 1: Create Directory Structure
# ===========================================
print("\n📂 Creating Directory Structure...")
print("-"*70)

create_folder(BASE_DIR / "docs")
create_folder(BASE_DIR / "models" / "skin_detection")
create_folder(BASE_DIR / "models" / "waste_classification")
create_folder(BASE_DIR / "models" / "android")
create_folder(BASE_DIR / "data" / "raw")
create_folder(BASE_DIR / "data" / "processed")
create_folder(BASE_DIR / "reports")
create_folder(BASE_DIR / "scripts")
create_folder(BASE_DIR / "config")
create_folder(BASE_DIR / "deployment")

# ===========================================
# STEP 2: Organize Model Files
# ===========================================
print("\n🤖 Organizing Model Files...")
print("-"*70)

# Skin Detection Models
move_file("models/category_classifier.joblib", 
          "models/skin_detection/category_classifier.joblib",
          "Skin detection category model")
move_file("models/skin_type_classifier.joblib", 
          "models/skin_detection/skin_type_classifier.joblib",
          "Skin detection skin type model")
move_file("models/category_encoder.joblib", 
          "models/skin_detection/category_encoder.joblib",
          "Category label encoder")
move_file("models/skin_type_encoder.joblib", 
          "models/skin_detection/skin_type_encoder.joblib",
          "Skin type label encoder")
copy_file("models/skin_model_metadata.json", 
          "models/skin_detection/metadata.json",
          "Skin model metadata")

# Waste Classification Models
move_file("models/waste_classifier_tfidf.joblib", 
          "models/waste_classification/waste_classifier_tfidf.joblib",
          "Waste TF-IDF vectorizer")
move_file("models/waste_classifier_ensemble.joblib", 
          "models/waste_classification/waste_classifier_ensemble.joblib",
          "Waste ensemble classifier")
move_file("models/label_encoder.joblib", 
          "models/waste_classification/label_encoder.joblib",
          "Waste label encoder")
copy_file("models/waste_classifier_metadata.json", 
          "models/waste_classification/metadata.json",
          "Waste model metadata")
copy_file("models/android_classification_map.json", 
          "models/waste_classification/android_class_map.json",
          "Android class mapping")

# Android Helpers
copy_file("models/Android_SkinDetector.java", 
          "models/android/Android_SkinDetector.java",
          "Android skin detection helper")
copy_file("models/WasteClassifierHelper.java", 
          "models/android/WasteClassifierHelper.java",
          "Android waste classification helper")
copy_file("models/android_encoding_map.json", 
          "models/android/skin_encoding_map.json",
          "Skin encoding map for Android")

# ===========================================
# STEP 3: Organize Dataset Files
# ===========================================
print("\n📊 Organizing Dataset Files...")
print("-"*70)

move_file("data/skin_detection_dataset.csv", 
          "data/processed/skin_detection_v2.csv",
          "Enhanced skin dataset v2")
move_file("data/waste_quality_dataset_expanded.csv", 
          "data/processed/waste_classification_v2.csv",
          "Enhanced waste dataset v2")

# Copy original datasets if they exist
if (BASE_DIR / "data/kitchenguard_waste_dataset.csv").exists():
    copy_file("data/kitchenguard_waste_dataset.csv", 
              "data/raw/kitchenguard_original.csv",
              "Original KitchenGuard dataset")

# ===========================================
# STEP 4: Move Scripts
# ===========================================
print("\n📝 Organizing Training Scripts...")
print("-"*70)

move_file("src/generate_datasets_v2.py", 
          "scripts/generate_datasets_v2.py",
          "Dataset generator v2")
move_file("src/generate_datasets_v3.py", 
          "scripts/generate_datasets_v3.py",
          "Dataset generator v3 (current)")
move_file("src/train_skin_model.py", 
          "scripts/train_skin_model.py",
          "Skin model training")
move_file("src/train_improved_waste_model.py", 
          "scripts/train_improved_waste_model.py",
          "Waste model training")

# Keep important utilities in src
copy_file("src/predict.py", 
          "src/kitchen_guard_predictor.py",
          "Main prediction utility")
copy_file("src/barcode_service.py", 
          "src/barcode_scanner.py",
          "Barcode scanning service")
copy_file("src/vision_service.py", 
          "src/vision_ai.py",
          "Vision AI service")

# Move validation scripts
copy_file("validate_training.py", 
          "scripts/validate_training.py",
          "Training validation script")

# ===========================================
# STEP 5: Documentation Setup
# ===========================================
print("\n📚 Setting Up Documentation...")
print("-"*70)

copy_file("README_FINAL.md", 
          "README.md",
          "Main project documentation")
copy_file("ML_TRAINING_GUIDE.md", 
          "docs/TRAINING_GUIDE.md",
          "ML training guide")
copy_file("ANDROID_INTEGRATION_COMPLETE.md", 
          "docs/ANDROID_INTEGRATION.md",
          "Android integration guide")
copy_file("TRAINING_SUMMARY.md", 
          "docs/TRAINING_SUMMARY.md",
          "Training summary")

# Create new organization documentation
doc_content = """# 🍽️ KitchenGuard CSM - Project Structure

## 📁 Directory Overview

```
CAPSTONE_MACHINE_LEARNING/
├── 📄 README.md                    # Main project documentation
├── 📄 app.py                       # FastAPI application (main server)
├── 📄 .env                         # Environment variables (optional)
│
├── 📂 models/                      # Trained ML models
│   ├── 📂 skin_detection/          # Skin detection models
│   │   ├── category_classifier.joblib
│   │   ├── skin_type_classifier.joblib
│   │   ├── category_encoder.joblib
│   │   ├── skin_type_encoder.joblib
│   │   └── metadata.json
│   │
│   ├── 📂 waste_classification/    # Waste quality models
│   │   ├── waste_classifier_tfidf.joblib
│   │   ├── waste_classifier_ensemble.joblib
│   │   ├── label_encoder.joblib
│   │   ├── metadata.json
│   │   └── android_class_map.json
│   │
│   └── 📂 android/                 # Android integration helpers
│       ├── Android_SkinDetector.java
│       ├── WasteClassifierHelper.java
│       └── skin_encoding_map.json
│
├── 📂 data/                        # Datasets
│   ├── 📂 raw/                     # Original datasets
│   │   └── kitchenguard_original.csv
│   │
│   └── 📂 processed/               # Processed/enhanced datasets
│       ├── skin_detection_v2.csv         (3,600 samples)
│       └── waste_classification_v2.csv   (3,000 samples)
│
├── 📂 scripts/                     # Training & utility scripts
│   ├── generate_datasets_v3.py     # Generate enhanced datasets
│   ├── train_skin_model.py         # Train skin detection model
│   ├── train_improved_waste_model.py  # Train waste classifier
│   ├── validate_training.py        # Validate training results
│   └── organize_folders.py         # This organization script
│
├── 📂 src/                         # Application source code
│   ├── predict.py                  → kitchen_guard_predictor.py  # Prediction engine
│   ├── barcode_service.py          → barcode_scanner.py         # Barcode scanning
│   ├── vision_service.py           → vision_ai.py               # Vision AI
│   └── preprocess.py               # Text preprocessing utilities
│
├── 📂 static/                      # Web interface files
│   └── index.html                  # Main web UI (polished design)
│
├── 📂 docs/                        # Documentation
│   ├── TRAINING_GUIDE.md           # Complete ML training guide
│   ├── ANDROID_INTEGRATION.md      # Android setup guide
│   ├── TRAINING_SUMMARY.md         # Current training status
│   └── PROJECT_STRUCTURE.md        # This file
│
├── 📂 reports/                     # Generated reports & logs
│   └── (auto-generated by training)
│
├── 📂 deployment/                  # Deployment configurations
│   ├── docker-compose.yml          # Docker orchestration
│   ├── nginx.conf                  # Nginx configuration
│   └── requirements.txt            # Python dependencies
│
├── 📂 config/                      # Configuration files
│   ├── app_config.json             # Application settings
│   └── model_config.yaml           # Model parameters
│
└── 📂 android/                     # Android Studio project
    └── app/src/main/java/...
```

## 🎯 File Placement Guide

### ✅ Models Location

**Skin Detection:**
- Models: `models/skin_detection/`
- Android Helper: `models/android/Android_SkinDetector.java`

**Waste Classification:**
- Models: `models/waste_classification/`
- Android Helper: `models/android/WasteClassifierHelper.java`

### ✅ Data Location

**Raw Data:**
- Original datasets: `data/raw/`
- Use for reference or re-training

**Processed Data:**
- Enhanced datasets: `data/processed/`
- Ready for model training

### ✅ Scripts Location

**Training Scripts:**
- All training: `scripts/`
- Execute from project root: `python scripts/train_xxx.py`

**Utilities:**
- Validation: `scripts/validate_training.py`
- Dataset generation: `scripts/generate_datasets_v3.py`

### ✅ Source Code Location

**Application Logic:**
- Main predictor: `src/kitchen_guard_predictor.py`
- Services: `src/barcode_scanner.py`, `src/vision_ai.py`
- Used by FastAPI app in `app.py`

## 🚀 Quick Start

1. **Setup Environment:**
   ```bash
   pip install -r deployment/requirements.txt
   ```

2. **Run Training:**
   ```bash
   python scripts/generate_datasets_v3.py
   python scripts/train_skin_model.py
   python scripts/train_improved_waste_model.py
   ```

3. **Start Server:**
   ```bash
   python app.py
   # Or
   uvicorn app:app --host 0.0.0.0 --port 8000
   ```

4. **Access Web UI:**
   - URL: http://localhost:8000
   - Features: Polished gradient navbar + smooth animations

## 📱 Android Integration

1. **Copy Models to Android:**
   ```bash
   xcopy /Y models\android\*.java android\app\models\
   xcopy /Y models\android\*.json android\app\models\
   ```

2. **Import Java Classes:**
   - `Android_SkinDetector.java` → Camera-based skin detection
   - `WasteClassifierHelper.java` → Text-based waste classification

3. **Reference Implementation:**
   - See: `docs/ANDROID_INTEGRATION.md`

## 📚 Documentation

| Document | Description |
|----------|-------------|
| `README.md` | Main project overview |
| `docs/TRAINING_GUIDE.md` | ML training procedures |
| `docs/ANDROID_INTEGRATION.md` | Android setup guide |
| `docs/TRAINING_SUMMARY.md` | Current training metrics |
| `docs/PROJECT_STRUCTURE.md` | This file |

## 🔧 Configuration

Edit `config/app_config.json` for:
- API endpoints
- Model paths
- Threshold settings
- Logging preferences

## 🏆 Status

✅ **Models**: All trained and organized  
✅ **Datasets**: Enhanced and categorized  
✅ **Scripts**: Centralized in `/scripts`  
✅ **Docs**: Comprehensive guides available  
✅ **UI**: Modern gradient design polished  

---

*Last Updated: September 16, 2026*  
*KitchenGuard CSM v2.0 - Organized Edition*
"""

with open(BASE_DIR / "docs" / "PROJECT_STRUCTURE.md", "w", encoding="utf-8") as f:
    f.write(doc_content)
print(f"✓ Created: docs/PROJECT_STRUCTURE.md")

copy_file("TRAINING_SUMMARY.md", 
          "docs/",
          "Updated training summary")

# ===========================================
# STEP 6: Create Requirements & Config
# ===========================================
print("\n⚙️ Creating Configuration Files...")
print("-"*70)

requirements = """# Core Dependencies
numpy>=1.24.0
pandas>=2.0.0
scikit-learn>=1.3.0
joblib>=1.2.0

# Deep Learning (Optional for future TFLite)
tensorflow>=2.12.0
opencv-python>=4.8.0
Pillow>=9.5.0

# Web Framework
fastapi>=0.100.0
uvicorn[standard]>=0.23.0
python-multipart>=0.0.6
pydantic>=2.0.0

# Utilities
python-dotenv>=1.0.0
requests>=2.31.0
"""

with open(BASE_DIR / "deployment" / "requirements.txt", "w", encoding="utf-8") as f:
    f.write(requirements)
print(f"✓ Created: deployment/requirements.txt")

config_example = """{
    "api": {
        "title": "KitchenGuard CSM API",
        "version": "2.0.0",
        "debug": false,
        "cors_origins": ["*"]
    },
    "models": {
        "skin_detection": {
            "category_model": "../models/skin_detection/category_classifier.joblib",
            "skin_type_model": "../models/skin_detection/skin_type_classifier.joblib",
            "category_encoder": "../models/skin_detection/category_encoder.joblib",
            "skin_type_encoder": "../models/skin_detection/skin_type_encoder.joblib",
            "confidence_threshold": 0.75
        },
        "waste_classification": {
            "tfidf_model": "../models/waste_classification/waste_classifier_tfidf.joblib",
            "classifier_model": "../models/waste_classification/waste_classifier_ensemble.joblib",
            "label_encoder": "../models/waste_classification/label_encoder.joblib",
            "confidence_threshold": 0.85
        }
    },
    "data": {
        "input_dir": "../data/processed/",
        "output_dir": "../reports/",
        "log_dir": "../logs/"
    },
    "logging": {
        "level": "INFO",
        "format": "%(asctime)s [%(levelname)s] %(name)s: %(message)s"
    }
}
"""

with open(BASE_DIR / "config" / "app_config.json.example", "w", encoding="utf-8") as f:
    f.write(config_example)
print(f"✓ Created: config/app_config.json.example")

# ===========================================
# Summary Output
# ===========================================
print("\n" + "="*70)
print("✅ FOLDER ORGANIZATION COMPLETE!")
print("="*70)
print(f"\n📁 New Structure:")
print(f"   • models/skin_detection/        - Skin detection artifacts")
print(f"   • models/waste_classification/  - Waste classification artifacts")
print(f"   • models/android/               - Android helper files")
print(f"   • data/processed/               - Enhanced datasets")
print(f"   • data/raw/                     - Original datasets")
print(f"   • scripts/                      - Training & utility scripts")
print(f"   • src/                          - Application source code")
print(f"   • docs/                         - Documentation")
print(f"   • deployment/                   - Deployment configs")
print(f"   • config/                       - Configuration files")
print(f"\n📚 Documentation:")
print(f"   ✓ docs/PROJECT_STRUCTURE.md     - Complete structure guide")
print(f"   ✓ docs/TRAINING_GUIDE.md        - ML training procedures")
print(f"   ✓ docs/ANDROID_INTEGRATION.md   - Android setup")
print(f"   ✓ docs/TRAINING_SUMMARY.md      - Training metrics")
print(f"\n🚀 Next Steps:")
print(f"   1. Review docs/PROJECT_STRUCTURE.md for full details")
print(f"   2. Update .env if needed (optional)")
print(f"   3. Copy models to Android project when ready")
print(f"   4. Deploy using uvicorn app:app --host 0.0.0.0")
print("="*70)
