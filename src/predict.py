"""
KitchenGuard CSM - Inference & Prediction Module
Langkah 10: Implementasi & Prediksi Teks Baru
Model digunakan pada aplikasi atau sistem (CLI & REST API / Backend Integration).
"""

import os
import time
import json
import argparse
import joblib
import numpy as np

from preprocess import preprocess_text

class KitchenGuardTextPredictor:
    """
    Kelas Inferensi untuk mengklasifikasikan catatan teks log limbah / kualitas bahan makanan
    ke dalam kategori standar KitchenGuard CSM:
    [SPOILED, EXPIRED, PREP_WASTE, OVERCOOKED, CONTAMINATED, SURPLUS]
    """
    def __init__(self, models_dir: str = "models"):
        self.models_dir = models_dir
        self.model_path = os.path.join(models_dir, "waste_classifier_model.joblib")
        self.vectorizer_path = os.path.join(models_dir, "tfidf_vectorizer.joblib")
        self.encoder_path = os.path.join(models_dir, "label_encoder.joblib")
        self.metadata_path = os.path.join(models_dir, "model_metadata.json")
        
        self._load_artifacts()
        
        # Rekomendasi aksi operasional dapur berdasarkan kategori PRD
        self.action_guide = {
            "SPOILED": "Reject & Pisahkan segera dari area penyimpanan dingin. Buang ke limbah organik untuk mencegah kontaminasi silang.",
            "EXPIRED": "Tarik dari inventori FIFO. Buang dan catat tanggal kadaluarsa serta nomor batch supplier untuk klaim retur.",
            "PREP_WASTE": "Timbang sisa prep (yield loss). Masukkan ke dalam kalkulasi efisiensi bahan butchery/vegetable station.",
            "OVERCOOKED": "Catat insiden kesalahan masak. Laporkan ke Sous Chef/Head Chef untuk evaluasi resep & suhu operasional.",
            "CONTAMINATED": "BAHAYA FOOD SAFETY! Segera reject seluruh bahan yang terpapar dan lakukan sterilisasi area/peralatan.",
            "SURPLUS": "Evaluasi porsi berlebih. Alihkan ke program staff meal jika higienis atau catat overproduction write-off."
        }
        
    def _load_artifacts(self):
        if not os.path.exists(self.model_path):
            raise FileNotFoundError(f"Model file tidak ditemukan di {self.model_path}. Jalankan train.py terlebih dahulu.")
        
        self.model = joblib.load(self.model_path)
        self.vectorizer = joblib.load(self.vectorizer_path)
        self.label_encoder = joblib.load(self.encoder_path)
        
        if os.path.exists(self.metadata_path):
            with open(self.metadata_path, "r", encoding="utf-8") as f:
                self.metadata = json.load(f)
        else:
            self.metadata = {"version": "kitchenguard-text-v1.0"}
            
        self.classes = list(self.label_encoder.classes_)

    def predict(self, raw_text: str, confidence_threshold: float = 0.85) -> dict:
        """
        Melakukan prediksi teks baru secara end-to-end:
        1. Preprocessing teks menggunakan Sastrawi (case folding, tokenisasi, stopword, stemming).
        2. Transformasi ke representasi TF-IDF.
        3. Inference probabilitas kelas.
        4. Penerapan Confidence Gate PRD (default threshold: 85%).
        5. Penyusunan payload standar KitchenGuard CSM.
        """
        start_time = time.perf_counter()
        
        # 1. Praproses
        clean_text = preprocess_text(raw_text)
        
        # Jika hasil praproses kosong
        if not clean_text.strip():
            return {
                "error": "Teks tidak mengandung kata valid setelah praproses.",
                "raw_text": raw_text,
                "ai": {
                    "predicted_class": "UNCERTAIN",
                    "confidence": 0.0,
                    "model_version": self.metadata.get("version", "kitchenguard-text-v1.0")
                }
            }
            
        # 2. Representasi Teks (TF-IDF)
        text_vec = self.vectorizer.transform([clean_text])
        
        # 3. Prediksi Probabilitas
        if hasattr(self.model, "predict_proba"):
            probs = self.model.predict_proba(text_vec)[0]
        elif hasattr(self.model, "decision_function"):
            decision = self.model.decision_function(text_vec)[0]
            # Softmax
            exp_d = np.exp(decision - np.max(decision))
            probs = exp_d / np.sum(exp_d)
        else:
            probs = np.zeros(len(self.classes))
            pred_idx = self.model.predict(text_vec)[0]
            probs[pred_idx] = 1.0
            
        top_idx = int(np.argmax(probs))
        top_class = self.classes[top_idx]
        confidence = float(probs[top_idx])
        
        inference_time_ms = round((time.perf_counter() - start_time) * 1000, 2)
        
        # 4. Penerapan Confidence Gate PRD
        # PRD KitchenGuard: confidence >= 85% -> decision support valid; < 85% -> UNCERTAIN
        gate_status = "APPROVED" if confidence >= confidence_threshold else "UNCERTAIN"
        effective_class = top_class if gate_status == "APPROVED" else "UNCERTAIN"
        
        # Probabilitas seluruh kelas
        class_probabilities = {
            self.classes[i]: round(float(probs[i]), 4) for i in range(len(self.classes))
        }
        
        # 5. Kontrak Output KitchenGuard CSM
        result = {
            "ai": {
                "task": "waste_text_classification",
                "class": effective_class,
                "predicted_class": effective_class,
                "raw_predicted_class": top_class,
                "confidence": round(confidence, 4),
                "confidence_threshold": confidence_threshold,
                "gate_status": gate_status,
                "model_version": self.metadata.get("version", "kitchenguard-text-v1.0"),
                "inference_time_ms": inference_time_ms
            },
            "raw_text": raw_text,
            "preprocessed_text": clean_text,
            "action_recommendation": self.action_guide.get(top_class, "Lakukan verifikasi manual staff dapur."),
            "class_probabilities": class_probabilities
        }
        return result

def main():
    parser = argparse.ArgumentParser(description="KitchenGuard CSM - Prediksi Teks Log Limbah Dapur Baru")
    parser.add_argument("--text", type=str, help="Teks catatan limbah/kualitas dapur")
    parser.add_argument("--threshold", type=float, default=0.85, help="Ambang batas confidence PRD (default: 0.85)")
    args = parser.parse_args()
    
    predictor = KitchenGuardTextPredictor()
    
    if args.text:
        res = predictor.predict(args.text, confidence_threshold=args.threshold)
        print("\n" + "=" * 60)
        print(" KITCHENGUARD CSM — HASIL INFERENSI TEKS (LANGKAH 10)")
        print("=" * 60)
        print(f"Teks Input       : {res['raw_text']}")
        print(f"Teks Praproses   : {res['preprocessed_text']}")
        print(f"Prediksi Kelas   : {res['ai']['predicted_class']}")
        print(f"Confidence Score : {res['ai']['confidence'] * 100:.2f}% (Status: {res['ai']['gate_status']})")
        print(f"Waktu Inferensi  : {res['ai']['inference_time_ms']} ms")
        print(f"Rekomendasi SOP  : {res['action_recommendation']}")
        print("\nProbabilitas Kelas:")
        for cls, prob in sorted(res['class_probabilities'].items(), key=lambda x: x[1], reverse=True):
            bar = "#" * int(prob * 25)
            print(f"  - {cls:<14}: {prob*100:6.2f}% | {bar}")
        print("\nPayload Integrasi JSON:")
        print(json.dumps(res, indent=2, ensure_ascii=False))
    else:
        # Jalankan beberapa contoh default
        sample_texts = [
            "Tomat merah sudah berlendir dan berjamur putih tebal di kulkas walk-in",
            "Susu UHT 1 liter melewati tanggal kadaluarsa tercantum 3 hari yang lalu",
            "Kulit wortel dan bonggol brokoli sisa pemotongan sup pagi di meja prep",
            "Daging sirloin gosong hitam pahit karena api grill kompor terlalu besar",
            "Daging ayam jatuh ke lantai dapur yang kotor berminyak dekat tempat cuci",
            "Nasi dan aneka lauk pauk sisa buffet prasmanan dinner tidak habis dikonsumsi"
        ]
        print("\nMenjalankan pengujian teks inferensi otomatis:")
        for text in sample_texts:
            print("-" * 60)
            res = predictor.predict(text, confidence_threshold=args.threshold)
            print(f"Input : '{text}'")
            print(f"Kelas : {res['ai']['predicted_class']} (Confidence: {res['ai']['confidence']*100:.1f}%)")
            print(f"SOP   : {res['action_recommendation']}")

if __name__ == "__main__":
    main()
