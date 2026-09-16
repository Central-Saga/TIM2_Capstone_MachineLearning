# DATASET & MODEL MANIFEST

## KitchenGuard CSM — Text Machine Learning Intelligence
**Komponen:** Modul Klasifikasi Teks Log Limbah & Kualitas Dapur  
**Versi Model:** `kitchenguard-text-v1.0`  
**Tanggal Rilis:** 15 September 2026  
**Status:** `FINAL (Approved)`  
**Program:** Capstone Project ITB STIKOM Bali — Track 2: Automated Quality Control & Waste Prevention  
**Mitra Industri:** PT Central Saga Mandala  

---

## 1. Dataset Manifest

```text
dataset_name           = kitchenguard_waste_text_dataset
dataset_version        = v1.0
source                 = KitchenGuard Operational Kitchen Logs (Indonesian Language)
license                = Internal Capstone Project ITB STIKOM Bali x PT Central Saga Mandala
total_samples          = 1078

class_names            = CONTAMINATED, EXPIRED, OVERCOOKED, PREP_WASTE, SPOILED, SURPLUS
samples_per_class      = CONTAMINATED: 180, EXPIRED: 180, OVERCOOKED: 180, PREP_WASTE: 178, SPOILED: 180, SURPLUS: 180

train_samples (70%)    = 754
validation_samples (15%)= 162
test_samples (15%)     = 162

preprocessing          = Lowercasing, Regex Punctuation/Number Cleaning, Tokenization, Indonesian Stopword Removal (Sastrawi), Stemming (Sastrawi)
feature_representation = TF-IDF Vectorizer (ngram_range=(1,2), sublinear_tf=True, max_features=3000)
vocabulary_size        = 1205 n-grams

model_architecture     = Multinomial Naive Bayes (alpha=0.1) & Linear SVM (Calibrated)
serialization_format   = Joblib (.joblib)

model_size_kb          = ~116 KB
test_accuracy          = 100.00%
macro_f1               = 1.0000
macro_precision        = 1.0000
macro_recall           = 1.0000

confusion_matrix       = reports/confusion_matrix.png
avg_inference_ms       = 32.2 ms
confidence_gate        = 85% (Default PRD threshold)
```

---

## 2. Definisi Kategori Output

| Kategori | Deskripsi Operasional | Rekomendasi SOP |
|---|---|---|
| `SPOILED` | Bahan makanan membusuk, berjamur, berlendir, asam | Reject & buang ke tempat sampah organik, cegah kontaminasi silang |
| `EXPIRED` | Melewati tanggal kadaluarsa atau best before | Tarik dari inventori FIFO, catat batch supplier untuk retur |
| `PREP_WASTE` | Sisa kupasan sayur, tulang, trim lemak preparasi | Timbang sisa prep, masukkan kalkulasi yield loss butchery/prep |
| `OVERCOOKED` | Kesalahan masak, gosong, hangus, over-seasoned | Catat insiden masak, evaluasi resep & suhu operasional |
| `CONTAMINATED` | Makanan jatuh ke lantai kotor, terkena bahan kimia/alergen | Bahaya Food Safety! Segera reject dan lakukan sterilisasi peralatan |
| `SURPLUS` | Sisa prasmanan/buffet tidak habis dikonsumsi | Evaluasi porsi berlebih, alihkan ke program staff meal jika higienis |
| `UNCERTAIN` | Confidence model di bawah threshold 85% | Memerlukan observasi manual staf dapur sebelum tindakan diambil |

---

## 3. Komparasi Terhadap Target PRD

| Metrik | Target PRD | Hasil Aktual | Status |
|---|---|---|---|
| **Validation / Test Accuracy** | $\ge 85\%$ | **100.00%** | **Lulus** |
| **Macro F1-Score** | $\ge 0.80$ | **1.0000** | **Lulus** |
| **Model Size** | $\le 20\text{ MB}$ | **~0.12 MB** | **Lulus** |
| **Inference Latency** | $\le 2\text{ detik}$ | **~0.03 detik** | **Lulus** |
| **Confidence Gate** | $100\%$ patuh threshold | **100%** | **Lulus** |

---

## 4. File Deliverables

- Dataset: `data/kitchenguard_waste_dataset.csv`
- Pipeline Praproses: `src/preprocess.py`
- Training & Evaluasi: `src/train.py`
- Modul Inferensi: `src/predict.py`
- Model Serialized: `models/waste_classifier_model.joblib`
- Vectorizer Serialized: `models/tfidf_vectorizer.joblib`
- Label Encoder: `models/label_encoder.joblib`
- Metadata Model: `models/model_metadata.json`
- Jupyter Notebook: `notebooks/KitchenGuard_Text_ML_10_Steps.ipynb`
- Web Dashboard & API: `app.py` & `static/index.html`
- Laporan & Plot: `reports/confusion_matrix.png`, `reports/class_distribution.png`, `reports/models_comparison.png`, `reports/evaluation_report.md`
