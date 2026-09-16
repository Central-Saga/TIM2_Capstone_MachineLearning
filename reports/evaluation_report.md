# Laporan Evaluasi Model Machine Learning Berbasis Teks KitchenGuard CSM

**Versi Model:** `kitchenguard-text-v1.0`  
**Algoritma Terbaik:** `Multinomial Naive Bayes`  
**Tanggal Evaluasi:** 15 September 2026  
**Status Release:** `FINAL (Approved)`  

---

## 1. Ringkasan Metrik Kinerja (Data Uji Akhir)

| Metrik | Target PRD | Hasil Aktual | Status |
|---|---|---|---|
| **Test Accuracy** | $\ge 85\%$ | **100.00%** | Lulus |
| **Macro F1-Score** | $\ge 0.80$ | **1.0000** | Lulus |
| **Macro Precision** | - | **1.0000** | - |
| **Macro Recall** | - | **1.0000** | - |
| **Weighted F1-Score** | - | **1.0000** | - |

---

## 2. Per-Class Performance

```text
              precision    recall  f1-score   support

CONTAMINATED     1.0000    1.0000    1.0000        27
     EXPIRED     1.0000    1.0000    1.0000        27
  OVERCOOKED     1.0000    1.0000    1.0000        27
  PREP_WASTE     1.0000    1.0000    1.0000        27
     SPOILED     1.0000    1.0000    1.0000        27
     SURPLUS     1.0000    1.0000    1.0000        27

    accuracy                         1.0000       162
   macro avg     1.0000    1.0000    1.0000       162
weighted avg     1.0000    1.0000    1.0000       162

```

---

## 3. Komparasi Algoritma pada Data Validasi

| Algoritma | Validation Accuracy | Validation Macro F1 |
|---|---|---|
| Multinomial Naive Bayes | 100.00% | 1.0000 |
| Linear SVM (Calibrated) | 100.00% | 1.0000 |
| Logistic Regression | 100.00% | 1.0000 |
| Random Forest | 99.38% | 0.9938 |

---

## 4. Visualisasi Hasil
- Confusion Matrix: `reports/confusion_matrix.png`
- Distribusi Data: `reports/class_distribution.png`
- Perbandingan Model: `reports/models_comparison.png`

Langkah 9 (Simpan Model) selesai. Sistem siap beralih ke **Langkah 10: Implementasi & Prediksi Teks Baru**.
