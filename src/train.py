"""
KitchenGuard CSM - Training & Evaluation Pipeline
Mengimplementasikan:
- Langkah 4: Analisis dan Pembagian Data (Train 70%, Val 15%, Test 15%)
- Langkah 5: Representasi Teks (TF-IDF Vectorizer)
- Langkah 6: Pemilihan Algoritma (Naive Bayes, SVM, Logistic Regression, Random Forest)
- Langkah 7: Pelatihan Model & Parameter Tuning
- Langkah 8: Evaluasi Model & Decision Gate (Accuracy, Precision, Recall, F1, Confusion Matrix)
- Langkah 9: Simpan Model & Vectorizer
"""

import os
import json
import joblib
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import LabelEncoder
from sklearn.naive_bayes import MultinomialNB
from sklearn.svm import LinearSVC
from sklearn.calibration import CalibratedClassifierCV
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    classification_report, confusion_matrix
)

from preprocess import clean_and_preprocess_dataframe

# Konfigurasi style matplotlib
sns.set_theme(style="whitegrid")
plt.rcParams.update({'font.sans-serif': 'Arial', 'font.size': 11})

def run_pipeline(
    data_path: str = os.path.join("data", "kitchenguard_waste_dataset.csv"),
    models_dir: str = "models",
    reports_dir: str = "reports"
):
    os.makedirs(models_dir, exist_ok=True)
    os.makedirs(reports_dir, exist_ok=True)
    
    print("=" * 80)
    print(" KITCHENGUARD CSM: ALUR PEMBUATAN MODEL MACHINE LEARNING BERBASIS TEKS ")
    print("=" * 80)
    
    # =========================================================================
    # LANGKAH 1 & 2: MEMUAT DATASET & TARGET
    # =========================================================================
    print("\n[Langkah 1 & 2] Memuat dataset teks operasional limbah dapur...")
    df_raw = pd.read_csv(data_path)
    print(f"Dataset dimuat dari '{data_path}'. Total baris: {len(df_raw)}")
    print("Kategori kelas target:", sorted(df_raw["category"].unique()))
    
    # =========================================================================
    # LANGKAH 3: PEMBERSIHAN & PRAPROSES TEKS
    # =========================================================================
    print("\n[Langkah 3] Pembersihan & Praproses Teks...")
    df = clean_and_preprocess_dataframe(df_raw, text_col="text", target_col="category")
    
    # =========================================================================
    # LANGKAH 4: ANALISIS DAN PEMBAGIAN DATA
    # =========================================================================
    print("\n[Langkah 4] Analisis dan Pembagian Data...")
    
    # Visualisasi Distribusi Kelas
    plt.figure(figsize=(9, 5))
    order = df["category"].value_counts().index
    ax = sns.countplot(data=df, x="category", hue="category", order=order, palette="viridis", legend=False)
    plt.title("Distribusi Kelas Dataset Teks KitchenGuard CSM", fontsize=14, fontweight="bold", pad=15)
    plt.xlabel("Kategori Limbah / Kualitas", fontweight="bold")
    plt.ylabel("Jumlah Sampel", fontweight="bold")
    plt.xticks(rotation=15)
    for p in ax.patches:
        ax.annotate(f"{int(p.get_height())}", (p.get_x() + p.get_width() / 2., p.get_height()),
                    ha='center', va='bottom', fontsize=10, xytext=(0, 3), textcoords='offset points')
    plt.tight_layout()
    dist_plot_path = os.path.join(reports_dir, "class_distribution.png")
    plt.savefig(dist_plot_path, dpi=300)
    plt.close()
    print(f"Visualisasi distribusi data disimpan: {dist_plot_path}")
    
    # Label Encoding
    label_encoder = LabelEncoder()
    encoded_labels = label_encoder.fit_transform(df["category"].tolist())
    class_names = list(label_encoder.classes_)
    
    # Pembagian Data: 70% Train, 15% Validation, 15% Test
    # Gunakan list/numpy array murni untuk kompatibilitas penuh
    X = np.array(df["clean_text"].tolist(), dtype=object)
    y = np.array(encoded_labels, dtype=int)
    
    X_train_val, X_test, y_train_val, y_test = train_test_split(
        X, y, test_size=0.15, random_state=42, stratify=y
    )
    
    val_ratio = 0.15 / (0.70 + 0.15)  # proporsi dari sisa untuk val
    X_train, X_val, y_train, y_val = train_test_split(
        X_train_val, y_train_val, test_size=val_ratio, random_state=42, stratify=y_train_val
    )
    
    print(f"Hasil Pembagian Data (Stratified Split):")
    print(f"  - Data Latih (Train 70%)      : {len(X_train)} sampel")
    print(f"  - Data Validasi (Val 15%)     : {len(X_val)} sampel")
    print(f"  - Data Uji (Test Akhir 15%)   : {len(X_test)} sampel")
    
    # =========================================================================
    # LANGKAH 5: REPRESENTASI TEKS (TF-IDF)
    # =========================================================================
    print("\n[Langkah 5] Representasi Teks Menggunakan TF-IDF...")
    # N-gram range (1, 2) menangkap unigram dan bigram (misal: 'lewat kadaluarsa', 'jamur putih')
    vectorizer = TfidfVectorizer(
        ngram_range=(1, 2),
        sublinear_tf=True,
        max_features=3000,
        min_df=2
    )
    
    # Fit HANYA pada data latih untuk mencegah data leakage
    X_train_tfidf = vectorizer.fit_transform(X_train)
    X_val_tfidf = vectorizer.transform(X_val)
    X_test_tfidf = vectorizer.transform(X_test)
    
    print(f"Vocabulary Size: {len(vectorizer.vocabulary_)} fitur n-gram.")
    print(f"Dimensi Matriks Fitur Latih: {X_train_tfidf.shape}")
    
    # =========================================================================
    # LANGKAH 6 & 7: PEMILIHAN ALGORITMA & PELATIHAN MODEL
    # =========================================================================
    print("\n[Langkah 6 & 7] Pemilihan Algoritma & Pelatihan Model...")
    
    algorithms = {
        "Multinomial Naive Bayes": MultinomialNB(alpha=0.1),
        "Linear SVM (Calibrated)": CalibratedClassifierCV(
            estimator=LinearSVC(C=1.0, random_state=42, max_iter=2000),
            cv=3
        ),
        "Logistic Regression": LogisticRegression(C=2.0, max_iter=1000, random_state=42),
        "Random Forest": RandomForestClassifier(n_estimators=150, max_depth=25, random_state=42)
    }
    
    validation_results = {}
    
    for name, model in algorithms.items():
        print(f"  --> Melatih model {name}...")
        model.fit(X_train_tfidf, y_train)
        
        y_val_pred = model.predict(X_val_tfidf)
        acc = accuracy_score(y_val, y_val_pred)
        macro_f1 = f1_score(y_val, y_val_pred, average="macro")
        weighted_f1 = f1_score(y_val, y_val_pred, average="weighted")
        
        validation_results[name] = {
            "model": model,
            "val_accuracy": acc,
            "val_macro_f1": macro_f1,
            "val_weighted_f1": weighted_f1
        }
        print(f"      Validation Accuracy: {acc*100:.2f}% | Macro F1: {macro_f1:.4f}")
        
    # Visualisasi Perbandingan Model
    model_names = list(validation_results.keys())
    accuracies = [validation_results[m]["val_accuracy"] * 100 for m in model_names]
    f1_scores = [validation_results[m]["val_macro_f1"] * 100 for m in model_names]
    
    plt.figure(figsize=(10, 5))
    x_indices = np.arange(len(model_names))
    bar_width = 0.35
    
    plt.bar(x_indices - bar_width/2, accuracies, bar_width, label="Accuracy (%)", color="#3b82f6")
    plt.bar(x_indices + bar_width/2, f1_scores, bar_width, label="Macro F1 (%)", color="#10b981")
    
    plt.xlabel("Algoritma Machine Learning", fontweight="bold")
    plt.ylabel("Skor Evaluasi (%)", fontweight="bold")
    plt.title("Perbandingan Kinerja Algoritma pada Data Validasi", fontsize=14, fontweight="bold", pad=15)
    plt.xticks(x_indices, model_names, rotation=10)
    plt.ylim(0, 110)
    plt.legend(loc="lower right")
    
    for i in range(len(model_names)):
        plt.text(i - bar_width/2, accuracies[i] + 1.5, f"{accuracies[i]:.1f}%", ha='center', fontsize=9, fontweight='bold')
        plt.text(i + bar_width/2, f1_scores[i] + 1.5, f"{f1_scores[i]:.1f}%", ha='center', fontsize=9, fontweight='bold')
        
    plt.tight_layout()
    comp_plot_path = os.path.join(reports_dir, "models_comparison.png")
    plt.savefig(comp_plot_path, dpi=300)
    plt.close()
    print(f"Grafik perbandingan model disimpan: {comp_plot_path}")
    
    # Pilih model terbaik berdasarkan Macro F1 pada data validasi
    best_model_name = max(validation_results, key=lambda k: validation_results[k]["val_macro_f1"])
    best_model_info = validation_results[best_model_name]
    best_model = best_model_info["model"]
    
    print(f"\nModel Terbaik Terpilih: '{best_model_name}'")
    print(f"Validation Accuracy : {best_model_info['val_accuracy']*100:.2f}%")
    print(f"Validation Macro F1 : {best_model_info['val_macro_f1']:.4f}")
    
    # =========================================================================
    # LANGKAH 8: EVALUASI MODEL PADA DATA UJI (TEST SET) & DECISION GATE
    # =========================================================================
    print("\n[Langkah 8] Evaluasi Akhir Model pada Data Uji (Test Set)...")
    print("Catatan: Data uji HANYA digunakan di langkah ini untuk menguji performa final.")
    
    y_test_pred = best_model.predict(X_test_tfidf)
    test_acc = accuracy_score(y_test, y_test_pred)
    test_prec_macro = precision_score(y_test, y_test_pred, average="macro")
    test_rec_macro = recall_score(y_test, y_test_pred, average="macro")
    test_f1_macro = f1_score(y_test, y_test_pred, average="macro")
    test_f1_weighted = f1_score(y_test, y_test_pred, average="weighted")
    
    print("-" * 60)
    print(f"HASIL EVALUASI AKHIR (TEST SET) - {best_model_name}:")
    print(f"  - Accuracy          : {test_acc*100:.2f}%")
    print(f"  - Macro Precision   : {test_prec_macro:.4f}")
    print(f"  - Macro Recall      : {test_rec_macro:.4f}")
    print(f"  - Macro F1-Score    : {test_f1_macro:.4f}")
    print(f"  - Weighted F1-Score : {test_f1_weighted:.4f}")
    print("-" * 60)
    
    clf_report = classification_report(y_test, y_test_pred, target_names=class_names, digits=4)
    print("\nClassification Report:")
    print(clf_report)
    
    # Confusion Matrix Heatmap
    cm = confusion_matrix(y_test, y_test_pred)
    plt.figure(figsize=(8, 6))
    sns.heatmap(cm, annot=True, fmt="d", cmap="Blues",
                xticklabels=class_names, yticklabels=class_names)
    plt.title(f"Confusion Matrix Test Set ({best_model_name})", fontsize=13, fontweight="bold", pad=15)
    plt.xlabel("Predicted Class", fontweight="bold")
    plt.ylabel("Actual True Class", fontweight="bold")
    plt.xticks(rotation=20)
    plt.tight_layout()
    cm_plot_path = os.path.join(reports_dir, "confusion_matrix.png")
    plt.savefig(cm_plot_path, dpi=300)
    plt.close()
    print(f"Confusion matrix disimpan: {cm_plot_path}")
    
    # DECISION GATE: "Hasil Sudah Baik?"
    print("\n" + "=" * 50)
    print(" DECISION GATE: Hasil Sudah Baik?")
    print(" Target PRD: Accuracy >= 85% & Macro F1 >= 0.80")
    print("=" * 50)
    
    meets_target = (test_acc >= 0.85) and (test_f1_macro >= 0.80)
    
    if meets_target:
        print(" [STATUS: YA] Hasil telah melampaui target PRD! Model siap disimpan.")
    else:
        print(" [STATUS: BELUM] Hasil belum memenuhi target PRD. Diperlukan perbaikan praproses/fitur.")
        raise ValueError("Model gagal memenuhi kriteria ambang batas minimum.")
        
    # =========================================================================
    # LANGKAH 9: SIMPAN MODEL & VECTORIZER
    # =========================================================================
    print("\n[Langkah 9] Menyimpan Model, Vectorizer, Label Encoder, & Metadata...")
    
    model_save_path = os.path.join(models_dir, "waste_classifier_model.joblib")
    vectorizer_save_path = os.path.join(models_dir, "tfidf_vectorizer.joblib")
    encoder_save_path = os.path.join(models_dir, "label_encoder.joblib")
    metadata_save_path = os.path.join(models_dir, "model_metadata.json")
    
    joblib.dump(best_model, model_save_path)
    joblib.dump(vectorizer, vectorizer_save_path)
    joblib.dump(label_encoder, encoder_save_path)
    
    metadata = {
        "model_name": best_model_name,
        "version": "kitchenguard-text-v1.0",
        "date_created": "2026-09-15",
        "task": "Kitchen Waste & Quality Reason Classification",
        "num_classes": len(class_names),
        "classes": class_names,
        "metrics": {
            "test_accuracy": round(float(test_acc), 4),
            "test_macro_precision": round(float(test_prec_macro), 4),
            "test_macro_recall": round(float(test_rec_macro), 4),
            "test_macro_f1": round(float(test_f1_macro), 4),
            "test_weighted_f1": round(float(test_f1_weighted), 4)
        },
        "target_comparison": {
            "accuracy_target": ">= 85%",
            "accuracy_actual": f"{test_acc*100:.2f}%",
            "macro_f1_target": ">= 0.80",
            "macro_f1_actual": f"{test_f1_macro:.4f}",
            "status": "APPROVED_FINAL"
        },
        "preprocessing_pipeline": [
            "clean_and_case_fold",
            "tokenize",
            "remove_stopwords_sastrawi",
            "stemming_sastrawi",
            "tfidf_vectorizer_ngram_1_2"
        ]
    }
    
    with open(metadata_save_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
        
    print(f"Model disimpan      : {model_save_path}")
    print(f"Vectorizer disimpan : {vectorizer_save_path}")
    print(f"Encoder disimpan    : {encoder_save_path}")
    print(f"Metadata disimpan   : {metadata_save_path}")
    
    # Pembuatan Laporan Evaluasi Markdown
    report_md_path = os.path.join(reports_dir, "evaluation_report.md")
    with open(report_md_path, "w", encoding="utf-8") as f:
        f.write(f"""# Laporan Evaluasi Model Machine Learning Berbasis Teks KitchenGuard CSM

**Versi Model:** `kitchenguard-text-v1.0`  
**Algoritma Terbaik:** `{best_model_name}`  
**Tanggal Evaluasi:** 15 September 2026  
**Status Release:** `FINAL (Approved)`  

---

## 1. Ringkasan Metrik Kinerja (Data Uji Akhir)

| Metrik | Target PRD | Hasil Aktual | Status |
|---|---|---|---|
| **Test Accuracy** | $\\ge 85\\%$ | **{test_acc*100:.2f}%** | Lulus |
| **Macro F1-Score** | $\\ge 0.80$ | **{test_f1_macro:.4f}** | Lulus |
| **Macro Precision** | - | **{test_prec_macro:.4f}** | - |
| **Macro Recall** | - | **{test_rec_macro:.4f}** | - |
| **Weighted F1-Score** | - | **{test_f1_weighted:.4f}** | - |

---

## 2. Per-Class Performance

```text
{clf_report}
```

---

## 3. Komparasi Algoritma pada Data Validasi

| Algoritma | Validation Accuracy | Validation Macro F1 |
|---|---|---|
""")
        for m in model_names:
            f.write(f"| {m} | {validation_results[m]['val_accuracy']*100:.2f}% | {validation_results[m]['val_macro_f1']:.4f} |\n")
            
        f.write(f"""
---

## 4. Visualisasi Hasil
- Confusion Matrix: `reports/confusion_matrix.png`
- Distribusi Data: `reports/class_distribution.png`
- Perbandingan Model: `reports/models_comparison.png`

Langkah 9 (Simpan Model) selesai. Sistem siap beralih ke **Langkah 10: Implementasi & Prediksi Teks Baru**.
""")
    print(f"Laporan lengkap disimpan: {report_md_path}")
    print("\n[SUKSES] Seluruh tahapan Langkah 4 hingga 9 berhasil diselesaikan!")
    return metadata

if __name__ == "__main__":
    run_pipeline()
