# 🔄 KONSEP PROYEK PURE MACHINE LEARNING

**Status:** Konsep untuk mengubah KitchenGuard CSM menjadi pure ML project

---

## 🎯 TUJUAN

Mengubah proyek KitchenGuard CSM yang saat ini memiliki komponen UI/Android menjadi **Pure Machine Learning Project** tanpa tampilan antarmuka.

---

## 📊 PERBEDAAN STRUKTUR

### **Sebelum (Dengan UI):**
```
KitchenGuard_CSM/
├── android/                    ❌ Tampilan Mobile
├── app.py                      ❌ Web Server  
├── static/                     ❌ Web Assets
├── src/                        ✅ Source Code
├── models/                     ✅ ML Models
└── tests/                      ✅ Tests
```

### **Setelah (Pure ML):**
```
KitchenGuard_ML/
├── src/                        ← Core ML code (cost_calculator, preprocess)
├── models/                     ← Trained .joblib files
├── data/                       ← Datasets (raw & processed)
├── notebooks/                  ← Jupyter notebooks for EDA
├── scripts/                    ← CLI tools for training/prediction
├── tests/                      ← Test suites
└── requirements.txt            ← ML dependencies only
```

---

## 🔧 YANG HARUS DIPERTAHANKAN

### ✅ Files/Directories yang DIKEEP:

1. **`src/`** - Core ML logic
   - `cost_calculator.py` - Financial calculations ✅
   - `preprocess.py` - Text preprocessing ✅
   - `barcode_service.py` - Barcode database lookup ✅

2. **`models/`** - Trained models
   - `waste_classification/` - Waste classifier ✅
   - `skin_detection/` - Skin tone detector ✅

3. **`data/`** - Datasets
   - Raw datasets
   - Processed/cleaned data

4. **`tests/`** - Unit tests
   - `test_cost_calculator.py` (24 tests) ✅
   - `test_kitchenguard.py` (ML tests) ✅

5. **`scripts/`** - Training/inference scripts

6. **Notebooks** - Exploratory analysis

---

## 🗑️ YANG HAPUS/DIHILANGKAN

### ❌ Remove These:

1. **`android/`** - Android Studio project
2. **`app.py`** - FastAPI web server *(opsional, bisa diganti dengan CLI)*
3. **`static/`** - Web assets
4. **`deployment/`** - Deployment configs untuk UI
5. File-file dengan kata: Android, mobile, UI, interface dalam nama/konten

---

## 📝 CONTOH SCRIPT INFERENSI (CLI)

File baru: `scripts/predict_waste.py`

```python
#!/usr/bin/env python3
"""
Pure ML Prediction Tool - Command Line Interface
No web interface needed
"""

import argparse
import joblib
from pathlib import Path
from preprocess import preprocess_text
from cost_calculator import calculate_loss_per_category


def predict_waste(text_description, weight_kg=1.0):
    """Predict waste category and calculate financial loss"""
    
    # Load models
    vectorizer = joblib.load('models/waste_classification/tfidf_vectorizer.joblib')
    model = joblib.load('models/waste_classification/waste_classifier_model.joblib')
    
    # Preprocess and predict
    cleaned = preprocess_text(text_description)
    transformed = vectorizer.transform([cleaned])
    prediction = model.predict(transformed)[0]
    confidence = model.predict_proba(transformed).max()
    
    # Calculate loss
    loss = calculate_loss_per_category(prediction, weight_kg)
    
    return {
        'category': prediction,
        'confidence': f"{confidence:.1%}",
        'total_loss': loss['total_loss_rupiah'],
        'priority': loss['priority_level'],
        'action': loss['action_recommendation']
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Predict waste quality')
    parser.add_argument('--text', required=True, help='Waste description')
    parser.add_argument('--weight', type=float, default=1.0, help='Weight in kg')
    
    args = parser.parse_args()
    
    result = predict_waste(args.text, args.weight)
    
    print("\n" + "="*60)
    print("🤖 WASTE CLASSIFICATION RESULT")
    print("="*60)
    print(f"Input:      {args.text}")
    print(f"Category:   {result['category']}")
    print(f"Confidence: {result['confidence']}")
    print(f"Loss:       Rp {result['total_loss']:,.0f}")
    print(f"Priority:   {result['priority']}")
    print(f"Action:     {result['action']}")
    print("="*60 + "\n")
```

**Usage:**
```bash
python scripts/predict_waste.py --text "Daging berbau busuk berlendir" --weight 2.5
```

**Output:**
```
============================================================
🤖 WASTE CLASSIFICATION RESULT
============================================================
Input:      Daging berbau busuk berlendir
Category:   SPOILED
Confidence: 97.2%
Loss:       Rp 480,000
Priority:   HIGH
Action:     Document root cause - check storage conditions
============================================================
```

---

## 💻 TRAINING SCRIPT CONTOH

File baru: `scripts/train_model.py`

```python
#!/usr/bin/env python3
"""Train waste classification model without any UI dependencies"""

import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.metrics import classification_report
import joblib
from pathlib import Path


def train_and_save():
    # Load data
    df = pd.read_csv('data/processed/waste_quality_dataset.csv')
    
    # Split data
    X_train = df['description']
    y_train = df['category']
    
    # Create pipeline
    vectorizer = TfidfVectorizer(max_features=5000)
    model = MultinomialNB(alpha=0.1)
    
    # Transform text
    X_train_vec = vectorizer.fit_transform(X_train)
    
    # Train
    model.fit(X_train_vec, y_train)
    
    # Save
    Path('models/waste_classification').mkdir(parents=True, exist_ok=True)
    joblib.dump(vectorizer, 'models/waste_classification/tfidf_vectorizer.joblib')
    joblib.dump(model, 'models/waste_classification/waste_classifier.joblib')
    
    # Evaluate
    print(classification_report(y_train, model.predict(X_train_vec)))
    
    print("\n✅ Model saved to models/waste_classification/")


if __name__ == '__main__':
    train_and_save()
```

---

## 📦 REQUIREMENTS.txt (PURE ML)

```txt
# Core ML Libraries
numpy>=1.24.0
pandas>=2.1.0
scikit-learn>=1.3.0
joblib>=1.3.0

# Data Processing
openpyxl>=3.1.0

# Testing
pytest>=7.4.0
coverage>=7.2.0

# Notebooks (optional)
jupyter>=1.0.0
ipykernel>=6.25.0

# CLI Tools
click>=8.1.0
```

---

## ✅ KELEBIHAN PURE ML PROJECT

1. ✅ **Fokus pada ML** - Hanya concern model & algorithm
2. ✅ **Lightweight** - Tidak perlu Flask/FastAPI overhead
3. ✅ **Easier Maintenance** - Kode lebih sederhana
4. ✅ **Flexible** - Bisa integrate ke API, mobile app, atau CLI tool lain
5. ✅ **Better Performance** - Tanpa web layer overhead
6. ✅ **Clear Separation** - Logic terpisah dari presentation

---

## 🚀 CARA MENGGUNAKAN

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Run Training (Optional)
```bash
python scripts/train_model.py
```

### 3. Make Prediction
```bash
python scripts/predict_waste.py --text "Salad terkontaminasi rambut" --weight 1.5
```

### 4. Explore in Notebook
```bash
jupyter notebook notebooks/eda.ipynb
```

### 5. Run Tests
```bash
pytest tests/ -v --cov=src
```

---

## 📋 CHECKLIST KONVERSI

- [ ] Hapus folder android/*
- [ ] Hapus static/, deployment/
- [ ] Update requirements.txt (hapus web dependencies)
- [ ] Buat CLI prediction script
- [ ] Buat CLI training script
- [ ] Tambahkan Jupyter notebooks
- [ ] Update README.md (focus on ML usage)
- [ ] Update documentation
- [ ] Run semua tests
- [ ] Validate model loading dan inference

---

## 💡 TIPS PENTING

1. **Model Access**: Models disimpan sebagai .joblib files, bisa di-load langsung di Python apapun
2. **Integration**: Jika butuh API nanti, tinggal tambahkan FastAPI wrapper ringan di atas pure ML code
3. **Testing**: Fokus test core ML logic, bukan UI interactions
4. **Deployment**: Deploy model saja, frontend bisa dibuat terpisah kapanpun

---

**Catatan**: Struktur ini memberikan fleksibilitas maksimal untuk:
- Menggunakan model di Python scripts
- Membuat mobile app sendiri (via REST API jika needed)
- Integration ke sistem existing
- Research dan experimentation dengan Jupyter

*Maintenance level tinggi karena clear separation of concerns!*
