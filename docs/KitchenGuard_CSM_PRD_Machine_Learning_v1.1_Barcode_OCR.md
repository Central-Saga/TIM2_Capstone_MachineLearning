# PRODUCT REQUIREMENTS DOCUMENT (PRD)

## KitchenGuard CSM — Machine Learning & Vision Intelligence

**Dokumen Turunan dari:** KitchenGuard CSM PRD v1.3 — Baseline Implementasi  
**Versi:** Machine Learning v1.1 — Barcode + OCR + Freshness AI  
**Tanggal:** 15 September 2026  
**Mitra Industri:** PT Central Saga Mandala  
**Program:** Capstone Project ITB STIKOM Bali — Track 2: Automated Quality Control & Waste Prevention

---

# 1. Ringkasan Produk ML

Komponen **Machine Learning KitchenGuard CSM** berfungsi sebagai **decision-support system** untuk membantu staff mengklasifikasikan kondisi visual bahan melalui kamera Android.

Model dijalankan secara **on-device** menggunakan TensorFlow Lite.

ML bukan pengganti keputusan Head Chef atau quality control manual, dan bukan source of truth untuk financial write-off.

---

# 2. Tujuan ML

1. Memberikan bantuan freshness classification secara cepat.
2. Berjalan tanpa koneksi internet.
3. Menghasilkan confidence score.
4. Menghindari auto-decision pada low confidence.
5. Menyimpan model version untuk audit.
6. Terintegrasi dengan Android Waste Logging.
7. Memiliki dataset manifest dan evaluasi yang dapat dipertanggungjawabkan.

---

# 3. Scope ML

## P0 — Required
1. Dataset definition.
2. Dataset manifest.
3. Train/validation/test split.
4. Image preprocessing.
5. Model training.
6. Evaluation.
7. TFLite conversion.
8. On-device inference.
9. Confidence gate.
10. Model version tracking.
11. Integration contract dengan Android.

## P1
- Quantization optimization.
- Expanded dataset.
- Device benchmarking.
- Confusion analysis.
- Retraining pipeline.

## P2
- Model update mechanism.
- Additional ingredient classes.
- Advanced freshness scoring.

---

# 4. Non-Goals

ML tidak:
- melakukan final financial approval;
- menentukan stock quantity;
- menggantikan physical audit;
- menggantikan supplier inspection;
- menghitung variance;
- menghitung HPP;
- menjadi satu-satunya dasar reject bahan.

---

# 5. Initial Output Classes

Initial class:

```text
FRESH
ACCEPTABLE
SPOILED
REJECT
```

System state:

```text
UNCERTAIN
```

`UNCERTAIN` dihasilkan oleh application rule ketika confidence di bawah threshold.

---

# 6. Confidence Gate

Default threshold:

```text
85%
```

Aturan:

```text
confidence >= 85%
→ predicted class digunakan sebagai decision-support

confidence < 85%
→ output aplikasi = UNCERTAIN
```

Jika `UNCERTAIN`:
- aplikasi tidak boleh auto-reject;
- staff melakukan manual observation;
- high-value waste dapat diteruskan untuk review Head Chef.

---

# 7. ML Input

Input utama:

```text
image captured from Android camera
```

Metadata integrasi:

```text
ingredient_id
batch_id
capture_timestamp
device_model
model_version
```

Image size aktual wajib dicatat pada ML Dataset Manifest.

---

# 8. ML Output Contract

Output minimum:

```text
predicted_class
confidence
model_version
inference_time_ms
```

Contoh:

```json
{
  "predicted_class": "SPOILED",
  "confidence": 0.964,
  "model_version": "freshness-v1",
  "inference_time_ms": 380
}
```

Android kemudian menerapkan confidence gate.

---

# 9. Integration dengan Waste Logging

```text
Waste Form
↓
Capture Ingredient Image
↓
Preprocess Image
↓
TFLite Inference
↓
Predicted Class + Confidence
↓
Apply Confidence Gate
↓
User Confirmation / Manual Observation
↓
Save ML Metadata into Waste Record
```

ML result disimpan bersama waste log, tetapi financial loss tetap dihitung backend.

---

# 10. Dataset Manifest

Sebelum model final, artefak berikut wajib tersedia:

```text
docs/ml-dataset-manifest.md
```

Field minimum:

```text
dataset_name
dataset_version
source
license
total_images

class_names
images_per_class

train_images
validation_images
test_images

augmentation
image_size

model_architecture
quantization

model_size_mb
validation_accuracy
macro_f1

confusion_matrix
device_model
avg_inference_ms
p95_inference_ms
```

---

# 11. Dataset Manifest — Actual Result Template

```text
dataset_name           = TBD
dataset_version        = TBD
source                 = TBD
license                = TBD
total_images           = TBD

class_names            = TBD
images_per_class       = TBD

train_images           = TBD
validation_images      = TBD
test_images            = TBD

augmentation           = TBD
image_size             = TBD

model_architecture     = TBD
quantization           = TBD

model_size_mb          = TBD
validation_accuracy    = TBD
macro_f1               = TBD

confusion_matrix       = ATTACH_FILE_OR_LINK
device_model           = TBD
avg_inference_ms       = TBD
p95_inference_ms       = TBD
```

Semua `TBD` harus diganti dengan hasil eksperimen aktual sebelum model berstatus `FINAL`.

---

# 12. Dataset Requirements

Dataset harus memiliki:
- sumber yang dicatat;
- license/status penggunaan;
- class distribution;
- train/validation/test split;
- informasi augmentation;
- versioning dataset.

PRD tidak mengarang jumlah image aktual sebelum dataset benar-benar ditetapkan.

---

# 13. Dataset Split

Manifest harus mendokumentasikan:

```text
train
validation
test
```

Split dibuat sebelum final evaluation.

Jika terdapat image sequence atau gambar yang sangat mirip, tim harus menghindari data leakage antar split sejauh memungkinkan.

---

# 14. Preprocessing

Preprocessing aktual harus dicatat:

```text
input image size
normalization
color format
resize/crop strategy
augmentation
```

Preprocessing Android harus konsisten dengan preprocessing training/evaluation.

---

# 15. Augmentation

Jika digunakan, manifest mencatat augmentation aktual.

Contoh kategori dokumentasi:

```text
rotation
flip
brightness
contrast
crop
zoom
```

Dokumen final tidak boleh mengklaim augmentation yang tidak benar-benar digunakan.

---

# 16. Model Architecture

Field minimum:

```text
model_architecture
framework
input_shape
output_shape
parameter_count (jika tersedia)
```

Arsitektur aktual dipilih melalui eksperimen.

Requirement utama:
- dapat dikonversi ke TensorFlow Lite;
- cocok untuk inference on-device;
- output kompatibel dengan class contract.

---

# 17. Target Model

Target requirement:

```text
Validation Accuracy >= 85%
Macro F1 >= 0.80
Model Size <= 20 MB
On-device Inference <= 2 seconds
```

Target bukan klaim hasil.

Hasil aktual harus dilaporkan apa adanya.

---

# 18. Evaluation Metrics

Minimal:

```text
validation_accuracy
macro_f1
confusion_matrix
```

Jika tersedia, sertakan per-class:

```text
precision
recall
f1-score
support
```

---

# 19. Confusion Matrix

Confusion matrix wajib tersedia sebelum model `FINAL`.

Analisis minimal:
- class yang paling sering tertukar;
- apakah `SPOILED` salah menjadi `FRESH`;
- apakah `FRESH` salah menjadi `REJECT`;
- limitation yang terlihat dari hasil.

---

# 20. Model Release Status

```text
EXPERIMENTAL
CANDIDATE
FINAL
```

## EXPERIMENTAL
- Dataset/training masih berubah.
- Metric belum dijadikan baseline.

## CANDIDATE
- Model sudah dapat diuji di Android.
- Evaluation tersedia.
- Manifest sebagian besar lengkap.

## FINAL
Hanya jika:
1. Dataset manifest lengkap.
2. Split terdokumentasi.
3. Accuracy aktual tersedia.
4. Macro F1 aktual tersedia.
5. Confusion matrix tersedia.
6. Model size aktual tersedia.
7. Inference diuji minimal pada satu device target.
8. Hasil dibandingkan dengan target.
9. Limitation ditulis jika target tidak tercapai.

---

# 21. TFLite Conversion

Model candidate/final dikonversi menjadi:

```text
.tflite
```

Dokumentasi:

```text
source model version
TFLite version
quantization mode
model size
conversion date
```

---

# 22. Quantization

Quantization boleh digunakan untuk mengurangi ukuran atau inference time.

Manifest mencatat:

```text
none
float16
int8
other
```

Model hasil quantization harus dievaluasi kembali.

---

# 23. On-Device Inference

Inference dilakukan pada Android.

Target:

```text
<= 2 seconds
```

Benchmark aktual:

```text
device_model
android_version
avg_inference_ms
p95_inference_ms
model_size_mb
```

Minimal satu target device diuji sebelum status `FINAL`.

---

# 24. Android Integration Requirement

Android harus:
1. Memuat model lokal.
2. Melakukan preprocessing sesuai training.
3. Menjalankan inference.
4. Membaca output probabilities.
5. Menentukan top class.
6. Menerapkan threshold 85%.
7. Menghasilkan `UNCERTAIN` di bawah threshold.
8. Menyimpan model version.
9. Menyimpan inference time bila digunakan.
10. Tidak melakukan final approval otomatis.

---

# 25. ML Metadata pada Waste Log

Payload:

```json
{
  "ai": {
    "class": "SPOILED",
    "confidence": 0.964,
    "model_version": "freshness-v1"
  }
}
```

Backend menyimpan metadata untuk traceability.

---

# 26. Low Confidence Handling

Jika confidence rendah:

```text
result = UNCERTAIN
```

UI harus:
- menyatakan hasil tidak yakin;
- meminta manual observation;
- tidak menyatakan AI pasti benar;
- tidak auto-reject.

---

# 27. Governance Rule

Model bersifat:

> **Decision Support, bukan autonomous decision maker.**

High-value write-off tetap mengikuti approval workflow yang diotorisasi.

---

# 28. Model Versioning

Contoh:

```text
freshness-v1
freshness-v1.1
freshness-v2
```

Waste record menyimpan model version untuk traceability.

---

# 29. Reproducibility

Artefak training sebaiknya menyimpan:

```text
dataset version
training config
random seed (jika digunakan)
model architecture
checkpoint/final model
evaluation result
TFLite artifact
```

---

# 30. File / Artifact Deliverables

Minimal:

```text
docs/ml-dataset-manifest.md
models/<model-version>.tflite
reports/confusion-matrix.png
reports/evaluation-summary.md
```

Opsional:

```text
training notebook/script
conversion script
benchmark result
```

---

# 31. Acceptance Criteria ML

ML memenuhi requirement jika:
1. Model dapat dijalankan di Android.
2. Output class dan confidence terbaca.
3. Threshold 85% diterapkan.
4. Confidence rendah menjadi `UNCERTAIN`.
5. Model version tercatat.
6. Manifest tersedia.
7. Metric aktual tersedia.
8. Confusion matrix tersedia sebelum final.
9. Device benchmark tersedia sebelum final.
10. Limitation ditulis.

---

# 32. Testing

## Model Test
- valid input image;
- invalid/corrupt image handling;
- class output shape;
- confidence range;
- preprocessing consistency.

## Integration Test
- Android load model;
- inference success;
- low-confidence flow;
- model version masuk waste payload;
- offline inference berjalan.

## Benchmark
- inference time;
- model size;
- device test.

---

# 33. Success Metrics

| Metric | Target |
|---|---:|
| Validation Accuracy | ≥ 85% |
| Macro F1 | ≥ 0.80 |
| Model Size | ≤ 20 MB |
| On-device inference | ≤ 2 detik |
| Low-confidence handling | 100% mengikuti threshold |

Angka aktual wajib dibandingkan dengan target.

---

# 34. Limitation Documentation

Sebelum final, laporan membahas minimal:
- dataset coverage;
- class imbalance;
- lighting/background sensitivity;
- ingredient visual variation;
- class confusion;
- target yang tidak tercapai;
- device performance limitation.

---

# 35. Definition of Done

ML selesai untuk milestone final jika:
- dataset manifest lengkap;
- source/license terdokumentasi;
- train/validation/test split tersedia;
- preprocessing terdokumentasi;
- architecture terdokumentasi;
- training selesai;
- metric aktual tersedia;
- confusion matrix tersedia;
- TFLite berhasil dibuat;
- Android inference berhasil;
- confidence gate bekerja;
- device benchmark tersedia;
- limitation ditulis;
- model version ditetapkan.

---

# 36. Final Statement

Komponen ML KitchenGuard CSM adalah:

> **On-Device Freshness Classification yang mendukung keputusan waste tanpa menggantikan validasi manusia.**

Keberhasilan ML dinilai dari accuracy, Macro F1, confusion matrix, ukuran model, inference time, confidence handling, traceability dataset, dan integrasi yang aman dengan workflow Android.

---

# 37. Vision Intelligence Extension — Barcode + OCR

PRD Machine Learning ini diperluas agar workflow vision KitchenGuard mencakup tiga komponen yang saling melengkapi:

| Modul | Teknologi | Fungsi |
|---|---|---|
| Barcode Scanner | Google ML Kit Barcode Scanning | Mengidentifikasi ingredient / item / batch |
| Digital Scale OCR | Google ML Kit Text Recognition | Membaca angka berat dari display timbangan |
| Freshness AI | TensorFlow Lite | Mengklasifikasikan kondisi visual bahan |

Barcode dan OCR merupakan **data-capture assistance**, sedangkan TFLite merupakan **decision-support** untuk freshness. Ketiganya bukan source of truth stok atau finansial.

---

# 38. Barcode Scanning Requirement

Barcode digunakan untuk mengidentifikasi barang, bukan membaca teks seperti OCR.

Primary flow:

```text
Camera
↓
Scan Barcode
↓
Decode Barcode Value
↓
Lookup Local Cache / Backend
↓
Ingredient + Batch ditemukan
↓
Lanjut ke OCR timbangan
```

Format yang diprioritaskan pada MVP:

```text
EAN-13
EAN-8
UPC-A
CODE-128
QR Code (optional untuk label internal)
```

Output minimum:

```text
barcode_value
barcode_format
scan_timestamp
ingredient_id
batch_id
```

Jika mapping tersedia, aplikasi dapat menampilkan:

```text
ingredient_name
batch_code
supplier
expiry_at
base_unit
```

## 38.1 Barcode Fallback

Barcode tidak boleh menjadi blocker karena bahan seperti sayur loose, buah, rempah, daging yang sudah dipindah container, atau bahan prep dapat tidak memiliki barcode.

Jika barcode:
- tidak tersedia;
- rusak;
- tidak terdaftar;
- gagal di-decode;

maka:

```text
Cari Ingredient Manual
↓
Pilih Ingredient
↓
Pilih Batch jika tersedia
```

MVP menggunakan decoder dari ML Kit sehingga **tidak membutuhkan custom barcode-training dataset**. Yang diperlukan adalah barcode validation/test set untuk menguji kondisi nyata.

---

# 39. Digital Scale OCR Requirement

OCR digunakan untuk membaca angka berat dari layar timbangan digital.

Primary flow:

```text
Camera
↓
Arahkan ke Display Timbangan
↓
Detect / Crop Area Display
↓
Google ML Kit Text Recognition
↓
Parse Numeric Value
↓
Parse Unit
↓
Confidence / Validation
↓
User Confirmation
```

Contoh:

```text
Display:
1.45 kg

Parsed:
weight = 1.45
unit   = kg
```

Data minimum:

```text
ocr_raw_text
ocr_weight
ocr_unit
ocr_confidence
ocr_confirmed
```

## 39.1 OCR Confidence Gate

```text
confidence >= 0.90
→ nilai boleh di-auto-fill
```

Jika:

```text
confidence < 0.90
→ user wajib confirm / edit manual
```

OCR tidak boleh auto-submit waste tanpa user confirmation.

## 39.2 OCR Parsing Rules

Parser harus menangani format umum:

```text
1.45
1,45
0.250
250 g
1.45 kg
```

Normalisasi:

```text
comma decimal → dot decimal
g → kg jika canonical unit ingredient = kg
ml → liter jika canonical unit ingredient = liter
```

Jika hasil tidak valid:

```text
OCR_PARSE_FAILED
```

dan UI harus menyediakan manual input.

---

# 40. Dataset Strategy untuk Barcode dan OCR

## 40.1 Freshness AI Dataset

Tetap membutuhkan dataset training/evaluation untuk kondisi bahan:

```text
FRESH
ACCEPTABLE
SPOILED
REJECT
```

`UNCERTAIN` merupakan application state ketika confidence di bawah threshold.

## 40.2 OCR Digital Scale Dataset

Karena MVP menggunakan ML Kit OCR, dataset timbangan digunakan terutama untuk:

- benchmark;
- robustness test;
- regression test;
- evaluasi LCD / seven-segment digit recognition.

Keyword/dataset scope yang dibutuhkan:

```text
digital weighing scale display dataset
7-segment display OCR dataset
industrial digital meter digits dataset
LCD numeric display dataset
```

Tim juga disarankan membuat **custom kitchen-scale test set** dari timbangan yang benar-benar digunakan di dapur.

Variasi test image:

```text
normal
angle/miring
blur ringan
low light
bright light
reflection/glare
decimal point
different weight values
```

## 40.3 Barcode Test Dataset

Tidak dibutuhkan custom training dataset untuk MVP.

Buat validation/test set berisi:

```text
EAN-13
EAN-8
UPC-A
CODE-128
QR internal
```

dengan kondisi:

```text
normal
miring
blur ringan
low light
reflection
partially damaged
```

---

# 41. Unified Vision Workflow

Workflow Waste Logging yang direkomendasikan:

```text
1. Scan Barcode
   ↓
   Ingredient / Batch Identified

2. Scan Timbangan
   ↓
   OCR Reads Weight

3. Scan Kondisi Bahan
   ↓
   TFLite Freshness Inference

4. User Confirmation
   ↓
   Save Local
   ↓
   Sync Backend
```

Contoh:

```text
Barcode:
8991234567890
→ Wagyu Ribeye MB7
→ Batch WG-0915-A

OCR:
1.45 kg
→ confidence 94%

Freshness AI:
SPOILED
→ confidence 96.4%
```

Jika barcode gagal, user memilih ingredient secara manual. Jika OCR gagal atau confidence rendah, user mengoreksi berat secara manual.

---

# 42. Vision Metadata pada Waste Log

Contoh payload:

```json
{
  "barcode": {
    "value": "8991234567890",
    "format": "EAN_13"
  },
  "ocr": {
    "raw_text": "1.45 kg",
    "weight": 1.45,
    "unit": "kg",
    "confidence": 0.94,
    "confirmed": true
  },
  "ai": {
    "class": "SPOILED",
    "confidence": 0.964,
    "model_version": "freshness-v1"
  }
}
```

Backend tetap menjadi source of truth untuk:

```text
ingredient mapping final
hpp_snapshot
loss_amount
stock movement
approval
variance
financial calculation
```

---

# 43. Barcode + OCR Testing Requirements

## Barcode Test

Minimal menguji:

- EAN-13 decode;
- EAN-8 decode;
- UPC-A decode;
- CODE-128 decode;
- unknown barcode;
- damaged barcode;
- blurred barcode;
- manual fallback;
- ingredient lookup.

## OCR Test

Minimal menguji:

- integer value;
- decimal value;
- decimal point;
- decimal comma;
- `g`;
- `kg`;
- glare/reflection;
- low-light;
- camera angle;
- low-confidence confirmation;
- manual correction.

## Integrated Vision Test

```text
barcode → ingredient
OCR → weight
TFLite → freshness
↓
waste payload
```

Seluruh flow harus dapat berjalan tanpa membuat barcode/OCR menjadi keputusan otomatis yang tidak dapat dikoreksi user.

---

# 44. Updated Vision Success Metrics & Definition of Done

## Target

| Metric | Target |
|---|---:|
| Barcode scan latency | ≤ 2 detik target |
| Barcode manual fallback | 100% tersedia |
| OCR reading latency | ≤ 2 detik target |
| OCR auto-fill | hanya confidence ≥ 90% |
| OCR manual correction | 100% tersedia |
| Freshness Validation Accuracy | ≥ 85% |
| Freshness Macro F1 | ≥ 0.80 |
| TFLite model size | ≤ 20 MB |
| Freshness inference | ≤ 2 detik |
| Freshness low-confidence handling | 100% mengikuti threshold |

Tim wajib melaporkan **barcode decode success rate** dan **OCR exact-read accuracy** dari test set aktual. Nilai aktual tidak boleh dikarang sebelum pengujian.

## Tambahan Definition of Done

Machine Learning & Vision dianggap siap untuk milestone final jika:

- barcode scanner terintegrasi;
- barcode-to-ingredient lookup bekerja;
- manual ingredient fallback tersedia;
- OCR timbangan terintegrasi;
- OCR confirm/edit flow bekerja;
- barcode benchmark terdokumentasi;
- OCR benchmark terdokumentasi;
- freshness dataset manifest lengkap;
- TFLite inference berhasil di Android;
- confidence gate OCR dan freshness bekerja;
- model version tercatat;
- limitation seluruh vision module terdokumentasi.

---

# 45. Updated Final Vision Statement

Komponen Machine Learning & Vision KitchenGuard CSM menggabungkan:

> **Barcode Ingredient Identification + Digital Scale OCR + On-Device Freshness Classification**

Ringkasnya:

```text
Barcode → Ingredient
OCR     → Weight
TFLite  → Freshness
```

Ketiga modul mempercepat pencatatan waste dan mengurangi input manual, tetapi tetap mengikuti user confirmation, backend validation, dan governance KitchenGuard.

