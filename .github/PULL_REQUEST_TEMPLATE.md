## 📋 Pull Request Review & Quality Checklist

### 1. Deskripsi Perubahan
<!-- Jelaskan secara singkat tujuan PR ini dan komponen yang diubah -->

### 2. Tipe Perubahan
- [ ] Bug fix (perbaikan non-breaking)
- [ ] Feature baru (penambahan fungsionalitas model/API)
- [ ] Refactoring / Optimasi Model ML
- [ ] Dokumentasi & Sinkronisasi Kontrak Android

### 3. Checklist Validasi Kualitas & CI
- [ ] **Linting passed**: Flake8 lulus tanpa error sintaks.
- [ ] **Pytest passed**: Seluruh 58 unit & integration test lulus (`python -m pytest tests/`).
- [ ] **ML Cross-Validation passed**: `validate_model_advanced.py` berjalan sukses (5-fold CV).
- [ ] **Robustness & 2-Skema passed**: `comprehensive_multi_tests.py` lulus (28/28 test, 100%).
- [ ] **Artifact Bersih**: Tidak ada error/traceback pada file laporan/output.
- [ ] **Sinkronisasi Android**: Kontrak model selaras dengan repo Android (`TIM_2_ANDROID` / Dewa).

### 4. Kebijakan Review Manusia (Human-in-the-Loop)
> ⚠️ **PENTING**: Sesuai kebijakan tata kelola tim, PR ke branch `main` **WAJIB** direview dan disetujui secara eksplisit oleh minimal satu reviewer manusia (peer review) sebelum di-merge. Dilarang melakukan merge otomatis tanpa approval manusia.
