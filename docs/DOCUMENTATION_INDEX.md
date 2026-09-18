# 📚 KitchenGuard CSM — Indeks Dokumentasi & Status Proyek

Dokumen ini adalah ringkasan resmi dan peta panduan navigasi dokumentasi repositori `Central-Saga/TIM2_Capstone_MachineLearning`.

---

## 📌 Dokumen Inti & Acuan Resmi

| Dokumen | Lokasi | Keterangan |
|---|---|---|
| **Panduan Utama Proyek** | [`README.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/README.md) | Panduan instalasi, quick start, struktur proyek, dan arsitektur |
| **Kontrak API & Android Sync** | [`docs/ANDROID_API_CONTRACT.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/docs/ANDROID_API_CONTRACT.md) | Spesifikasi kontrak payload JSON antara FastAPI Backend dan Android Retrofit |
| **Product Requirements (PRD)** | [`docs/KitchenGuard_CSM_PRD_Machine_Learning_v1.1_Barcode_OCR.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/docs/KitchenGuard_CSM_PRD_Machine_Learning_v1.1_Barcode_OCR.md) | Kebutuhan produk sistem ML, OCR, Barcode, dan Freshness AI |
| **Struktur Proyek Terorganisir** | [`docs/PROJECT_STRUCTURE.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/docs/PROJECT_STRUCTURE.md) | Rincian direktori kerja, model artifacts, datasets, dan scripts |
| **Laporan Validasi ML Lanjutan** | [`MODEL_VALIDATION_REPORT.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/MODEL_VALIDATION_REPORT.md) & [`VALIDATION_SUMMARY.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/VALIDATION_SUMMARY.md) | Evaluasi 5-fold cross-validation pipeline, noise robustness, dan kalibrasi threshold |
| **Hasil Pengujian Komprehensif** | [`TEST_RESULTS.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/TEST_RESULTS.md) & [`FINAL_TEST_RESULTS.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/FINAL_TEST_RESULTS.md) | Status pengujian 69/69 passed (unit tests, integration workflows, dan endpoint tests) |

---

## 🧭 Peta File Laporan Historis di Root Repository

Banyak file markdown di root repositori dibuat selama tahap sprint/iterasi. Berikut adalah status dan fungsinya agar tidak menimbulkan kebingungan:

1. **Laporan Validasi & Audit:**
   - [`MODEL_VALIDATION_REPORT.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/MODEL_VALIDATION_REPORT.md): Laporan metrik cross-validation dan overfitting.
   - [`VALIDATION_SUMMARY.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/VALIDATION_SUMMARY.md): Ringkasan status kesiapan model ML.
   - [`TEST_RESULTS.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/TEST_RESULTS.md) & [`FINAL_TEST_RESULTS.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/FINAL_TEST_RESULTS.md): Log eksekusi 69 test pytest.

2. **Panduan Penggunaan & Panduan Integrasi Android:**
   - [`docs/ANDROID_API_CONTRACT.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/docs/ANDROID_API_CONTRACT.md): Acuan kontrak payload Android Retrofit.
   - [`docs/ANDROID_INTEGRATION.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/docs/ANDROID_INTEGRATION.md) & [`ANDROID_INTEGRATION_COMPLETE.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/ANDROID_INTEGRATION_COMPLETE.md): Panduan integrasi modul Android.
   - [`CAMERA_SCANNING_GUIDE.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/CAMERA_SCANNING_GUIDE.md) & [`CAMERA_USAGE_GUIDE.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/CAMERA_USAGE_GUIDE.md): Panduan integrasi CameraX Android.
   - [`STRICT_DETECTION_GUIDE.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/STRICT_DETECTION_GUIDE.md): Panduan mode deteksi ketat dengan fallback UNKNOWN.

3. **Laporan Penyelesaian Sprint:**
   - [`PROJECT_COMPLETE_SUMMARY.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/PROJECT_COMPLETE_SUMMARY.md), [`FINAL_SUMMARY.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/FINAL_SUMMARY.md), [`SELESAI_PERBAIKAN.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/SELESAI_PERBAIKAN.md), [`PROJECT_FIXES_SUMMARY.md`](file:///c:/CAPSTONE_MACHINE_LEARNING/PROJECT_FIXES_SUMMARY.md): Catatan sprint penyelesaian masalah dan pengorganisasian folder.

---

## ⚡ Status Terkini Sistem (September 2026)

- **Test Suite**: 69 / 69 passed (`python -m pytest -v`)
- **Backend API**: Running & Healthy (`/api/health`, `/api/predict`, `/api/barcode/scan`, `/api/ocr/scan-scale`)
- **Cross-Validation**: 1.0000 ± 0.0000 (Pipeline 5-fold CV tanpa data leakage)
- **Android Support**: Dual-mode (REST API Retrofit + Offline Keyword Matching Helper)
