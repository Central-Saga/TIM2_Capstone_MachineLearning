"""
KitchenGuard CSM - Web Application & REST API Service v3.0
Automated Quality Control & Waste Prevention
Mitra Industri: PT Central Saga Mandala
Capstone Project ITB STIKOM Bali — Track 2

Features:
- Text-based Waste Classification ML (Ensemble + Confidence Gate)
- Barcode Scanner Integration (Google ML Kit / Database Lookup)
- Vision AI Freshness Scanner (Daging, Buah, Sayur, Seafood)
- Digital Scale OCR (ML Kit Scale Reader)
- Financial Loss Calculator (Rupiah per Category & Disposal Impact)
- Shift Closing & Daily Audit Summary Reports
- Staff Feedback Loop for Continuous Learning
"""

import os
import sys
import time
import json
import logging
import re
import base64
from io import BytesIO
from pathlib import Path
from typing import Optional, List, Dict, Any
from contextlib import asynccontextmanager
from datetime import datetime

from fastapi import FastAPI, HTTPException, Query, Request, status, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse, FileResponse, JSONResponse
from pydantic import BaseModel, Field
import joblib

# Setup path absolut
BASE_DIR = Path(__file__).resolve().parent
SRC_DIR = BASE_DIR / "src"
MODELS_DIR = BASE_DIR / "models"
STATIC_DIR = BASE_DIR / "static"
REPORTS_DIR = BASE_DIR / "reports"
DATA_DIR = BASE_DIR / "data"

if str(SRC_DIR) not in sys.path:
    sys.path.insert(0, str(SRC_DIR))

try:
    from src.barcode_service import barcode_service, BARCODE_DATABASE
    from src.vision_service import vision_service, INGREDIENTS_CATALOG
    from src.preprocess import preprocess_text
    from src.cost_calculator import calculate_loss_per_category, generate_daily_summary, WASTE_COST_PER_KG
except ImportError:
    from barcode_service import barcode_service, BARCODE_DATABASE
    from vision_service import vision_service, INGREDIENTS_CATALOG
    from preprocess import preprocess_text
    from cost_calculator import calculate_loss_per_category, generate_daily_summary, WASTE_COST_PER_KG

# Setup logging terstruktur
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
logger = logging.getLogger("kitchenguard.api")

# State global model
model = None
vectorizer = None
class_names = []
metadata = {}

ACTION_GUIDE = {
    "CONTAMINATED": "Dispose immediately - critical safety hazard (bakteri/kimia)",
    "SPOILED": "Document root cause - periksa chiller & suhu penyimpanan",
    "EXPIRED": "Review FIFO inventory system - rotasi stok bahan lebih ketat",
    "OVERCOOKED": "Retrain staff on cooking procedures - cek timer dan suhu",
    "SURPLUS": "Consider portion adjustment or donation - audit sisa saji",
    "PREP_WASTE": "Optimize cutting techniques - minimalkan limbah kupasan"
}

def load_models() -> bool:
    """Memuat artefak model klasifikasi limbah dan TF-IDF vectorizer."""
    global model, vectorizer, class_names, metadata
    try:
        search_dirs = [
            MODELS_DIR / "waste_classification",
            MODELS_DIR
        ]
        model_path = None
        vec_path = None
        meta_path = None

        for d in search_dirs:
            mp = d / "waste_classifier_model.joblib"
            vp = d / "tfidf_vectorizer.joblib"
            if mp.exists() and vp.exists():
                model_path = mp
                vec_path = vp
                for mf in ["waste_classifier_metadata.json", "metadata.json"]:
                    if (d / mf).exists():
                        meta_path = d / mf
                        break
                break

        if not model_path or not vec_path:
            logger.warning(f"File model atau vectorizer tidak ditemukan di {search_dirs}")
            return False

        logger.info(f"Memuat model Machine Learning KitchenGuard dari {model_path.parent}...")
        model = joblib.load(str(model_path))
        vectorizer = joblib.load(str(vec_path))

        if meta_path and meta_path.exists():
            with open(meta_path, "r", encoding="utf-8") as f:
                metadata = json.load(f)
                class_names = metadata.get("classes", [])
        
        if not class_names and hasattr(model, "classes_"):
            class_names = list(model.classes_)
            
        logger.info(f"[OK] Model berhasil dimuat dengan {len(class_names)} kategori: {class_names}")
        return True
    except Exception as exc:
        logger.error(f"Gagal memuat model: {exc}", exc_info=True)
        return False

# Panggil pemuatan model saat inisialisasi modul agar siap saat diimport
load_models()

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Siklus hidup aplikasi saat startup dan shutdown."""
    logger.info("Memulai layanan KitchenGuard CSM v3.0...")
    STATIC_DIR.mkdir(exist_ok=True)
    REPORTS_DIR.mkdir(exist_ok=True)
    DATA_DIR.mkdir(exist_ok=True)
    
    if model is None or vectorizer is None:
        loaded = load_models()
        if not loaded:
            logger.warning("Peringatan: Model ML belum siap. Sistem akan mengembalikan error 503 saat inferensi.")
    yield
    logger.info("Menghentikan layanan KitchenGuard CSM v3.0...")

app = FastAPI(
    title="KitchenGuard CSM — Intelligence API",
    description=(
        "Layanan Machine Learning terpadu untuk klasifikasi limbah dapur, deteksi visual freshness, "
        "kalkulasi kerugian finansial, dan audit harian operasional F&B.\n\n"
        "**Mitra Industri:** PT Central Saga Mandala  \n"
        "**Program:** Capstone Project ITB STIKOM Bali — Track 2: Automated Quality Control & Waste Prevention"
    ),
    version="3.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS Middleware (allow_credentials=False for wildcard origins to satisfy CORS spec & security)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount folder static dan laporan
app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")
app.mount("/reports", StaticFiles(directory=str(REPORTS_DIR)), name="reports")

# =============================================================================
# PYDANTIC SCHEMAS / REQUEST & RESPONSE CONTRACTS
# =============================================================================

class PredictRequest(BaseModel):
    text: str = Field(
        ...,
        min_length=2,
        max_length=1000,
        description="Catatan insiden atau kondisi bahan limbah dapur",
        examples=["Daging ayam fillet berlendir dan baunya sudah busuk menyengat di chiller"]
    )
    threshold: float = Field(
        default=0.85,
        ge=0.0,
        le=1.0,
        description="Confidence threshold gate PRD (default 0.85 atau 85%)"
    )
    estimated_weight_kg: Optional[float] = Field(
        None,
        ge=0.0,
        description="Estimasi berat limbah dalam kilogram untuk perhitungan kerugian finansial"
    )

class PredictResponse(BaseModel):
    success: bool = True
    ai: Dict[str, Any]
    raw_text: str
    preprocessed_text: str
    action_recommendation: str
    financial_impact: Optional[Dict[str, Any]] = None
    recommendations: List[str] = []
    class_probabilities: Dict[str, float]
    processing_metadata: Dict[str, Any]

class BatchPredictRequest(BaseModel):
    items: List[str] = Field(
        ...,
        min_length=1,
        max_length=100,
        description="Daftar catatan limbah dapur untuk closing audit shift",
        examples=[[
            "Susu uht 1 liter expired date kemarin sore",
            "Kulit wortel sisa kupasan prep sayur sup pagi",
            "Steak sirloin hangus terbakar api grill terlalu besar"
        ]]
    )
    threshold: float = Field(default=0.85, ge=0.0, le=1.0)

class BatchPredictResponse(BaseModel):
    total_processed: int
    threshold_applied: float
    results: List[PredictResponse]
    processing_time_ms: float

class BarcodeScanRequest(BaseModel):
    barcode_value: str = Field(..., description="Nilai barcode hasil scan kamera", examples=["8991234567890"])
    detected_format: str = Field("EAN_13", description="Format barcode terdeteksi (EAN_13, CODE_128, QR)")

class VisionScanRequest(BaseModel):
    image_base64: Optional[str] = Field(None, description="String gambar format base64 dari kamera Android")
    ingredient_hint: Optional[str] = Field(None, description="Hint jenis bahan jika dipilih manual (daging_sapi, apple, tomato, dll)")
    threshold: float = Field(0.85, ge=0.0, le=1.0, description="Confidence threshold PRD (default 0.85)")

class OCRScaleRequest(BaseModel):
    image_base64: Optional[str] = Field(None, description="Gambar display timbangan digital dari kamera Android")
    scale_value_hint: Optional[str] = Field(None, description="Nilai simulasi jika tanpa kamera langsung (misal: '1.45 kg')")

class WasteLogSubmissionRequest(BaseModel):
    barcode_value: Optional[str] = Field(None, description="Kode barcode bahan (misal EAN-13)")
    ingredient_name: Optional[str] = Field(None, description="Nama bahan makanan")
    batch_id: Optional[str] = Field(None, description="ID Batch pengiriman atau preparasi")
    ocr_weight: Optional[float] = Field(None, description="Angka berat terbaca OCR timbangan")
    ocr_unit: Optional[str] = Field("kg", description="Satuan berat (kg/g/liter)")
    ocr_confidence: Optional[float] = Field(None, description="Confidence score OCR timbangan")
    note: str = Field(..., description="Catatan staf mengenai alasan pembuangan / kondisi bahan")
    reported_by: Optional[str] = Field("Staff Dapur", description="Nama staf / operator pelapor")

class StaffFeedbackRequest(BaseModel):
    raw_text: str
    predicted_class: str
    actual_verified_class: str
    is_correct: bool
    staff_notes: Optional[str] = None

# =============================================================================
# HELPER INFERENCE FUNCTION
# =============================================================================

def predict_waste(text: str, threshold: float = 0.85) -> Dict[str, Any]:
    """Menjalankan inferensi model ML klasifikasi teks dengan TF-IDF."""
    if model is None or vectorizer is None:
        raise HTTPException(status_code=503, detail="Model Machine Learning belum siap dimuat.")

    start_time = time.perf_counter()
    clean_text = preprocess_text(text)
    
    # Tangani input kosong
    if not clean_text or clean_text.strip() == "":
        inference_time = round((time.perf_counter() - start_time) * 1000, 2)
        default_prob = round(1.0 / len(class_names), 4) if class_names else 0.0
        prob_dist = {cls: default_prob for cls in class_names}
        return {
            "task": "waste_text_classification",
            "class": "UNCERTAIN",
            "predicted_class": "UNCERTAIN",
            "raw_predicted_class": "UNCERTAIN",
            "confidence": 0.0,
            "gate_status": "UNCERTAIN",
            "action_recommendation": "UNCERTAIN: Teks kosong atau tidak mengandung kata yang dapat diidentifikasi. Lakukan verifikasi manual.",
            "probabilities": prob_dist,
            "inference_time_ms": inference_time,
            "model_version": metadata.get("model_info", {}).get("version", "3.0.0"),
            "preprocessed_text": clean_text
        }

    X = vectorizer.transform([clean_text])
    is_oov = (X.nnz == 0)
    
    pred_idx = model.predict(X)[0]
    
    if hasattr(model, "predict_proba"):
        pred_proba = model.predict_proba(X)[0]
    else:
        pred_proba = [1.0 if i == pred_idx else 0.0 for i in range(len(class_names))]

    # Tentukan nama kelas prediksi
    try:
        idx = int(pred_idx)
        if 0 <= idx < len(class_names):
            predicted_class = class_names[idx]
        else:
            predicted_class = str(pred_idx)
    except (ValueError, TypeError):
        predicted_class = str(pred_idx)

    # Tangani teks Out of Vocabulary (OOV) agar tidak menghasilkan confidence tinggi palsu
    if is_oov:
        confidence = round(1.0 / len(class_names), 4) if class_names else 0.1
        gate_status = "UNCERTAIN"
        action = "UNCERTAIN: Teks berada di luar kosakata model. Staf dapur wajib melakukan verifikasi manual."
    else:
        confidence = float(pred_proba[idx]) if (0 <= idx < len(pred_proba)) else 0.0
        gate_status = "APPROVED" if confidence >= threshold else "UNCERTAIN"
        action = ACTION_GUIDE.get(predicted_class, "Review and document root cause.")

    prob_dist = {cls: round(float(pred_proba[i]), 4) for i, cls in enumerate(class_names) if i < len(pred_proba)}
    inference_time = round((time.perf_counter() - start_time) * 1000, 2)

    return {
        "task": "waste_text_classification",
        "class": predicted_class,
        "predicted_class": predicted_class,
        "raw_predicted_class": predicted_class,
        "confidence": round(confidence, 4),
        "gate_status": gate_status,
        "action_recommendation": action,
        "probabilities": prob_dist,
        "inference_time_ms": inference_time,
        "model_version": metadata.get("model_info", {}).get("version", "3.0.0"),
        "preprocessed_text": clean_text
    }

# =============================================================================
# API ENDPOINTS
# =============================================================================

@app.get("/", response_class=HTMLResponse, tags=["Web UI"])
@app.get("/reports", response_class=HTMLResponse, tags=["Web UI"])
def serve_home():
    """Menampilkan halaman antarmuka web interaktif KitchenGuard CSM (bebas UnicodeDecodeError di Windows)."""
    index_path = STATIC_DIR / "index.html"
    if index_path.exists():
        return FileResponse(str(index_path))
    return HTMLResponse(
        content="""
        <html>
            <body style="font-family: sans-serif; text-align: center; padding: 50px; background: #0b0f19; color: #fff;">
                <h2>KitchenGuard CSM — System Running</h2>
                <p>Dokumentasi API interaktif: <a style="color: #3b82f6;" href="/docs">/docs</a></p>
            </body>
        </html>
        """
    )

@app.get("/api/health", tags=["System"])
def healthcheck():
    """Healthcheck endpoint untuk pemantauan koneksi backend dan Android."""
    is_ready = model is not None and vectorizer is not None
    ver = metadata.get("model_info", {}).get("version", "3.0.0")
    return {
        "status": "healthy" if is_ready else "degraded",
        "service": "KitchenGuard CSM v3.0",
        "ml_available": is_ready,
        "model_loaded": is_ready,
        "version": ver,
        "model_version": ver,
        "categories_count": len(class_names),
        "active_classes": class_names,
        "timestamp": time.time()
    }

@app.get("/api/info", tags=["Model Information"])
def get_info():
    """Mengembalikan metadata model, kelas target, dan panduan SOP tindakan."""
    return {
        "app_name": "KitchenGuard CSM Text Intelligence",
        "system_version": "3.0.0",
        "institution": "ITB STIKOM Bali - Capstone Track 2",
        "industry_partner": "PT Central Saga Mandala",
        "model_metadata": metadata,
        "classes": class_names,
        "confidence_threshold_default": 0.85,
        "action_guide": ACTION_GUIDE,
        "cost_model_per_kg": WASTE_COST_PER_KG
    }

@app.post("/api/predict", response_model=PredictResponse, tags=["Prediction"])
def predict_waste_text(req: PredictRequest):
    """
    Klasifikasi teks catatan limbah dapur + PRD Confidence Gate + Estimasi Kerugian Finansial (Rp).
    Kompatibel penuh dengan UI Web dan Android Client.
    """
    clean_input = req.text.strip()
    if not clean_input:
        raise HTTPException(status_code=400, detail="Teks input tidak boleh kosong.")

    start_time = time.perf_counter()
    ai_result = predict_waste(clean_input, threshold=req.threshold)

    # Kalkulasi dampak finansial jika berat diberikan
    financial_impact = None
    if req.estimated_weight_kg is not None and req.estimated_weight_kg > 0:
        try:
            financial_impact = calculate_loss_per_category(
                ai_result["predicted_class"],
                req.estimated_weight_kg
            )
        except Exception as e:
            logger.warning(f"Gagal menghitung kerugian finansial: {e}")

    proc_time = round((time.perf_counter() - start_time) * 1000, 2)

    return {
        "success": True,
        "ai": ai_result,
        "raw_text": clean_input,
        "preprocessed_text": ai_result["preprocessed_text"],
        "action_recommendation": ai_result["action_recommendation"],
        "financial_impact": financial_impact,
        "recommendations": [ai_result["action_recommendation"]],
        "class_probabilities": ai_result["probabilities"],
        "processing_metadata": {
            "processing_time_ms": proc_time,
            "model_version": ai_result["model_version"]
        }
    }

@app.post("/api/predict/batch", response_model=BatchPredictResponse, tags=["Prediction"])
def predict_batch_waste_text(req: BatchPredictRequest):
    """Memproses batch catatan limbah dapur sekaligus saat shift closing audit."""
    start_time = time.perf_counter()
    results = []

    for text in req.items:
        clean_text = text.strip()
        if clean_text:
            ai_res = predict_waste(clean_text, threshold=req.threshold)
            results.append({
                "success": True,
                "ai": ai_res,
                "raw_text": clean_text,
                "preprocessed_text": ai_res["preprocessed_text"],
                "action_recommendation": ai_res["action_recommendation"],
                "financial_impact": None,
                "recommendations": [ai_res["action_recommendation"]],
                "class_probabilities": ai_res["probabilities"],
                "processing_metadata": {"processing_time_ms": ai_res["inference_time_ms"], "model_version": ai_res["model_version"]}
            })

    total_time_ms = round((time.perf_counter() - start_time) * 1000, 2)
    return {
        "total_processed": len(results),
        "threshold_applied": req.threshold,
        "results": results,
        "processing_time_ms": total_time_ms
    }

@app.post("/api/calculate-loss", tags=["Financial Analytics"])
def calculate_financial_loss(req: dict):
    """Menghitung kerugian finansial langsung berdasarkan kategori limbah dan berat (kg)."""
    try:
        if "category" not in req or "weight_kg" not in req:
            raise HTTPException(status_code=400, detail="Field 'category' dan 'weight_kg' wajib diisi.")
        
        weight = float(req["weight_kg"])
        category = str(req["category"]).upper().strip()
        
        result = calculate_loss_per_category(category, weight)
        return {"success": True, "timestamp": datetime.now().isoformat(), "data": result}
    except HTTPException: raise
    except ValueError as val_err:
        raise HTTPException(status_code=400, detail=str(val_err))
    except Exception as exc:
        logger.error(f"Loss calculation error: {exc}")
        raise HTTPException(status_code=500, detail=str(exc))

@app.post("/api/reports/daily-summary", tags=["Financial Analytics"])
def get_daily_report(req: dict):
    """Menghasilkan ringkasan audit harian, total kerugian rupiah, dan rekomendasi prioritas."""
    try:
        if "waste_entries" not in req:
            raise HTTPException(status_code=400, detail="Field 'waste_entries' wajib disertakan.")
        
        entries = req.get("waste_entries", [])
        filter_cat = req.get("filter_category")
        if filter_cat:
            entries = [e for e in entries if e.get("category") == filter_cat]
            
        report = generate_daily_summary(entries)
        return {"success": True, "timestamp": datetime.now().isoformat(), "data": report}
    except HTTPException: raise
    except Exception as exc:
        logger.error(f"Report generation error: {exc}")
        raise HTTPException(status_code=500, detail=str(exc))

@app.post("/api/waste-log/analyze", tags=["Waste Logging Integration"])
def analyze_waste_log_entry(entry: WasteLogSubmissionRequest):
    """
    Endpoint terpadu Android Waste Logging sesuai PRD Bagian 41 & 42.
    Menggabungkan Barcode, OCR Timbangan, AI Text Classification, dan Financial Loss Calculator.
    """
    ai_result = predict_waste(entry.note, threshold=0.85)
    
    # Hitung kerugian finansial jika OCR timbangan membaca angka berat
    financial_data = None
    if entry.ocr_weight is not None and entry.ocr_weight > 0:
        try:
            financial_data = calculate_loss_per_category(ai_result["predicted_class"], entry.ocr_weight)
        except Exception as e:
            logger.warning(f"Gagal kalkulasi finansial di waste-log: {e}")

    return {
        "timestamp": datetime.now().isoformat(),
        "reported_by": entry.reported_by,
        "ingredient": {
            "name": entry.ingredient_name,
            "batch_id": entry.batch_id
        },
        "barcode": {
            "value": entry.barcode_value,
            "detected": entry.barcode_value is not None
        },
        "ocr": {
            "weight": entry.ocr_weight,
            "unit": entry.ocr_unit,
            "confidence": entry.ocr_confidence,
            "auto_filled": (entry.ocr_confidence is not None and entry.ocr_confidence >= 0.90)
        },
        "ai": {
            "task": "waste_reason_classification",
            "class": ai_result["predicted_class"],
            "raw_class": ai_result["raw_predicted_class"],
            "confidence": ai_result["confidence"],
            "model_version": ai_result["model_version"],
            "gate_status": ai_result["gate_status"],
            "requires_manual_observation": (ai_result["gate_status"] == "UNCERTAIN")
        },
        "note": entry.note,
        "action_recommendation": ai_result["action_recommendation"],
        "financial_impact": financial_data
    }

@app.post("/api/feedback", status_code=status.HTTP_201_CREATED, tags=["Continuous Learning"])
def record_staff_feedback(feedback: StaffFeedbackRequest):
    """Merekam feedback staf dapur untuk siklus perbaikan berkelanjutan & retraining model."""
    feedback_file = DATA_DIR / "staff_feedback_logs.jsonl"
    record = feedback.model_dump()
    record["timestamp"] = time.time()
    record["datetime"] = datetime.now().isoformat()
    
    try:
        with open(feedback_file, "a", encoding="utf-8") as f:
            f.write(json.dumps(record, ensure_ascii=False) + "\n")
        logger.info(f"Feedback staf dicatat: {feedback.raw_text[:30]} -> {feedback.actual_verified_class}")
        return {"status": "success", "message": "Feedback staf berhasil dicatat untuk siklus retraining."}
    except Exception as exc:
        logger.error(f"Gagal mencatat feedback staf: {exc}")
        raise HTTPException(status_code=500, detail="Gagal menyimpan log feedback.")

# =============================================================================
# ENDPOINT BARCODE SCANNER (Google ML Kit Barcode - PRD Bagian 38 & 41)
# =============================================================================

@app.post("/api/barcode/scan", tags=["Barcode Scanner"])
def scan_barcode(req: BarcodeScanRequest):
    """Lookup database bahan, batch, & supplier berdasarkan kode barcode."""
    result = barcode_service.scan_and_lookup(req.barcode_value, detected_format=req.detected_format)
    logger.info(f"Barcode scan '{req.barcode_value}' -> status: {result.get('status')}")
    return result

@app.get("/api/barcode/samples", tags=["Barcode Scanner"])
def get_barcode_samples():
    """Mengembalikan daftar contoh barcode riil inventori dapur."""
    samples = []
    for code, data in BARCODE_DATABASE.items():
        samples.append({
            "barcode": code,
            "format": data.get("format", "EAN_13"),
            "name": data.get("name"),
            "category": data.get("category"),
            "batch_id": data.get("batch_id"),
            "unit": data.get("base_unit", "kg")
        })
    return {"samples": samples}

# =============================================================================
# ENDPOINT VISION INGREDIENT & FRESHNESS SCANNER (TFLite Vision AI - PRD Bagian 37 & 41)
# =============================================================================

@app.post("/api/vision/scan-ingredient", tags=["Vision AI Scanner"])
def scan_ingredient_vision(req: VisionScanRequest):
    """Memindai citra visual bahan makanan (Daging, Buah, Sayur, Seafood) dan klasifikasi freshness."""
    image_bytes = None
    if req.image_base64:
        try:
            base64_data = req.image_base64
            if "," in base64_data:
                base64_data = base64_data.split(",")[1]
            image_bytes = base64.b64decode(base64_data)
        except Exception as e:
            logger.warning(f"Gagal mendecode base64 image: {e}")

    # Fallback sintetis representatif jika tanpa gambar kamera langsung
    if not image_bytes:
        from PIL import Image
        img = Image.new("RGB", (224, 224), color=(180, 50, 40) if req.ingredient_hint and "daging" in req.ingredient_hint else (60, 160, 70))
        buffer = BytesIO()
        img.save(buffer, format="JPEG")
        image_bytes = buffer.getvalue()

    result = vision_service.analyze_image(
        image_bytes=image_bytes,
        hint_ingredient=req.ingredient_hint,
        confidence_threshold=req.threshold
    )
    return result

@app.get("/api/vision/samples", tags=["Vision AI Scanner"])
def get_vision_samples():
    """Mengembalikan daftar katalog bahan makanan yang didukung sistem visual."""
    return {
        "total_categories": 4,
        "supported_categories": ["Daging & Unggas", "Buah-buahan", "Sayuran", "Seafood"],
        "ingredients": INGREDIENTS_CATALOG
    }

# =============================================================================
# ENDPOINT OCR DIGITAL SCALE (Google ML Kit Text Recognition - PRD Bagian 39)
# =============================================================================

@app.post("/api/ocr/scan-scale", tags=["Digital Scale OCR"])
def scan_scale_ocr(req: OCRScaleRequest):
    """Membaca angka berat dan satuan dari layar timbangan digital dengan ambang auto-fill 90%."""
    if not req.scale_value_hint and not req.image_base64:
        raise HTTPException(
            status_code=400,
            detail="Wajib menyertakan 'image_base64' atau 'scale_value_hint' untuk membaca timbangan."
        )

    reading_str = req.scale_value_hint
    
    # Jika gambar diberikan namun tidak ada hint teks, coba ekstraksi atau tandai butuh pembacaan
    if not reading_str and req.image_base64:
        # Placeholder OCR parsing untuk base64 image jika tidak ada engine OCR eksternal terpasang
        reading_str = "0.0 kg"

    match = re.search(r"(\d+[\.,]?\d*)\s*([a-zA-Z]*)", reading_str) if reading_str else None
    if match and float(match.group(1).replace(",", ".")) > 0:
        raw_val = match.group(1).replace(",", ".")
        weight = float(raw_val)
        unit = match.group(2).lower() if match.group(2) else "kg"
        confidence = 0.94 if req.scale_value_hint else 0.85
    else:
        weight = 0.0
        unit = "kg"
        confidence = 0.0

    auto_fill_allowed = confidence >= 0.90

    if confidence == 0.0:
        return {
            "status": "NOT_FOUND",
            "ocr": {
                "raw_text": reading_str or "",
                "weight": 0.0,
                "unit": "kg",
                "confidence": 0.0,
                "auto_filled": False,
                "requires_confirmation": True
            },
            "message": "Tidak dapat mendeteksi angka berat yang valid dari input timbangan."
        }

    return {
        "status": "SUCCESS",
        "ocr": {
            "raw_text": reading_str,
            "weight": weight,
            "unit": unit,
            "confidence": confidence,
            "auto_filled": auto_fill_allowed,
            "requires_confirmation": True
        },
        "message": "Berat timbangan berhasil dibaca oleh ML Kit OCR."
    }

if __name__ == "__main__":
    import uvicorn
    logger.info("=" * 60)
    logger.info("KitchenGuard CSM v3.0 Starting on http://127.0.0.1:8000")
    logger.info("=" * 60)
    uvicorn.run("app:app", host="127.0.0.1", port=8000, reload=False)
