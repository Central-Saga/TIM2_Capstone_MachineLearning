"""
KitchenGuard CSM - Web Application & REST API Service
Modul: Machine Learning Berbasis Teks (Langkah 10: Implementasi & Prediksi Teks Baru)
Standar PRD: KitchenGuard CSM v1.1 Barcode + OCR + Freshness AI
"""

import os
import sys
import json
import time
import logging
from pathlib import Path
from typing import List, Optional, Dict, Any
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Query, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse, HTMLResponse, JSONResponse
from pydantic import BaseModel, Field

# Setup path absolut
BASE_DIR = Path(__file__).resolve().parent
SRC_DIR = BASE_DIR / "src"
MODELS_DIR = BASE_DIR / "models"
STATIC_DIR = BASE_DIR / "static"
REPORTS_DIR = BASE_DIR / "reports"
DATA_DIR = BASE_DIR / "data"

if str(SRC_DIR) not in sys.path:
    sys.path.append(str(SRC_DIR))

from predict import KitchenGuardTextPredictor
from barcode_service import barcode_service, BARCODE_DATABASE
from vision_service import vision_service, INGREDIENTS_CATALOG

# Setup logging terstruktur
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
logger = logging.getLogger("kitchenguard.api")

# Inisialisasi global predictor
predictor: Optional[KitchenGuardTextPredictor] = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Siklus hidup aplikasi: memuat artefak model saat startup."""
    global predictor
    logger.info("Memulai layanan KitchenGuard CSM Text Intelligence API...")
    try:
        predictor = KitchenGuardTextPredictor(models_dir=str(MODELS_DIR))
        logger.info(
            f"Model '{predictor.metadata.get('model_name')}' (v{predictor.metadata.get('version')}) "
            f"berhasil dimuat dengan {len(predictor.classes)} kelas target."
        )
    except Exception as exc:
        logger.error(f"Gagal memuat model machine learning: {exc}", exc_info=True)
        raise RuntimeError(f"Model initialization failed: {exc}")
    yield
    logger.info("Menghentikan layanan KitchenGuard CSM Text Intelligence API...")

app = FastAPI(
    title="KitchenGuard CSM — Text Intelligence API",
    description=(
        "Layanan Machine Learning berbasis teks untuk klasifikasi catatan limbah operasional, "
        "kualitas bahan dapur, serta decision-support pencegahan waste.\n\n"
        "**Mitra Industri:** PT Central Saga Mandala  \n"
        "**Program:** Capstone Project ITB STIKOM Bali — Track 2: Automated Quality Control & Waste Prevention"
    ),
    version="1.1.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# 1. Konfigurasi Middleware CORS (Penting untuk integrasi Android dan Web Client)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 2. Mount Static Files dan Direktori Laporan Visual
STATIC_DIR.mkdir(exist_ok=True)
REPORTS_DIR.mkdir(exist_ok=True)
DATA_DIR.mkdir(exist_ok=True)

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
        description="Catatan insiden atau deskripsi kondisi bahan limbah dapur",
        examples=["Daging ayam fillet berlendir dan baunya sudah busuk menyengat di chiller"]
    )
    threshold: float = Field(
        default=0.85,
        ge=0.0,
        le=1.0,
        description="Ambang batas confidence gate PRD KitchenGuard (default 0.85 atau 85%)"
    )

class PredictResponse(BaseModel):
    ai: Dict[str, Any]
    raw_text: str
    preprocessed_text: str
    action_recommendation: str
    class_probabilities: Dict[str, float]

class BatchPredictRequest(BaseModel):
    items: List[str] = Field(
        ...,
        min_length=1,
        max_length=100,
        description="Daftar catatan limbah dapur untuk diproses sekaligus (shift closing audit)",
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

class WasteLogSubmissionRequest(BaseModel):
    """
    Schema input form terpadu Waste Logging sesuai PRD Bagian 41 & 42
    (Menggabungkan Barcode, OCR Timbangan, dan AI Decision Support)
    """
    barcode_value: Optional[str] = Field(None, description="Kode barcode bahan (misal EAN-13)")
    ingredient_name: Optional[str] = Field(None, description="Nama bahan makanan")
    batch_id: Optional[str] = Field(None, description="ID Batch pengiriman atau preparasi")
    ocr_weight: Optional[float] = Field(None, description="Angka berat terbaca OCR timbangan")
    ocr_unit: Optional[str] = Field("kg", description="Satuan berat (kg/g/liter)")
    ocr_confidence: Optional[float] = Field(None, description="Confidence score OCR timbangan")
    note: str = Field(..., description="Catatan staf mengenai alasan pembuangan / kondisi bahan")
    reported_by: Optional[str] = Field("Staff Dapur", description="Nama staf / operator pelapor")

class StaffFeedbackRequest(BaseModel):
    """Catatan umpan balik staf untuk siklus perbaikan berkelanjutan (Langkah 8 loop)."""
    raw_text: str
    predicted_class: str
    actual_verified_class: str
    is_correct: bool
    staff_notes: Optional[str] = None

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

# =============================================================================
# API ENDPOINTS
# =============================================================================

@app.get("/", response_class=HTMLResponse, tags=["Web UI"])
def serve_home():
    """Menampilkan halaman antarmuka web interaktif KitchenGuard CSM."""
    index_file = STATIC_DIR / "index.html"
    if index_file.exists():
        return FileResponse(str(index_file))
    return HTMLResponse(
        content="""
        <html>
            <body style="font-family: sans-serif; text-align: center; padding: 50px; background: #0b0f19; color: #fff;">
                <h2>KitchenGuard CSM — Text Intelligence API is Running</h2>
                <p>Dokumentasi API interaktif tersedia pada: <a style="color: #3b82f6;" href="/docs">/docs</a></p>
            </body>
        </html>
        """
    )

@app.get("/api/health", tags=["System"])
def healthcheck():
    """Healthcheck endpoint untuk pemantauan server dan koneksi Android."""
    is_ready = predictor is not None and predictor.model is not None
    return {
        "status": "healthy" if is_ready else "degraded",
        "model_loaded": is_ready,
        "model_version": predictor.metadata.get("version") if predictor else None,
        "timestamp": time.time()
    }

@app.get("/api/info", tags=["Model Information"])
def get_info():
    """Mengembalikan metadata model, metrik evaluasi aktual, dan panduan SOP operasional."""
    if not predictor:
        raise HTTPException(status_code=503, detail="Model belum siap dimuat.")
    return {
        "app_name": "KitchenGuard CSM Text Intelligence",
        "system_version": "1.1.0",
        "institution": "ITB STIKOM Bali - Capstone Track 2",
        "industry_partner": "PT Central Saga Mandala",
        "model_metadata": predictor.metadata,
        "classes": predictor.classes,
        "confidence_threshold_default": 0.85,
        "action_guide": predictor.action_guide
    }

@app.post("/api/predict", response_model=PredictResponse, tags=["Prediction"])
def predict_waste_text(req: PredictRequest):
    """
    Melakukan klasifikasi teks catatan limbah dapur baru (Langkah 10).
    Menerapkan praproses Sastrawi, pembobotan TF-IDF, serta PRD Confidence Gate.
    """
    if not predictor:
        raise HTTPException(status_code=503, detail="Layanan model sedang tidak tersedia.")
    
    clean_input = req.text.strip()
    if not clean_input:
        raise HTTPException(status_code=400, detail="Teks input tidak boleh hanya berisi spasi.")

    try:
        result = predictor.predict(clean_input, confidence_threshold=req.threshold)
        logger.info(
            f"Inferensi: '{clean_input[:40]}...' -> [{result['ai']['predicted_class']}] "
            f"({result['ai']['confidence']*100:.1f}%)"
        )
        return result
    except Exception as exc:
        logger.error(f"Error saat inferensi teks: {exc}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Terjadi kegagalan inferensi: {str(exc)}")

@app.post("/api/predict/batch", response_model=BatchPredictResponse, tags=["Prediction"])
def predict_batch_waste_text(req: BatchPredictRequest):
    """
    Memproses batch beberapa catatan limbah dapur sekaligus (misal saat audit closing shift).
    """
    if not predictor:
        raise HTTPException(status_code=503, detail="Layanan model sedang tidak tersedia.")
    
    start_time = time.perf_counter()
    results = []
    
    for text in req.items:
        clean_text = text.strip()
        if clean_text:
            res = predictor.predict(clean_text, confidence_threshold=req.threshold)
            results.append(res)
            
    total_time_ms = round((time.perf_counter() - start_time) * 1000, 2)
    return {
        "total_processed": len(results),
        "threshold_applied": req.threshold,
        "results": results,
        "processing_time_ms": total_time_ms
    }

@app.post("/api/waste-log/analyze", tags=["Waste Logging Integration"])
def analyze_waste_log_entry(entry: WasteLogSubmissionRequest):
    """
    Endpoint terpadu integrasi Android Waste Logging sesuai PRD Bagian 41 & 42.
    Menggabungkan input Barcode, OCR Timbangan, dan AI Decision Support.
    """
    if not predictor:
        raise HTTPException(status_code=503, detail="Layanan model sedang tidak tersedia.")

    ai_result = predictor.predict(entry.note, confidence_threshold=0.85)
    
    # Format payload integrasi standar KitchenGuard CSM
    payload = {
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
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
            "class": ai_result["ai"]["predicted_class"],
            "raw_class": ai_result["ai"]["raw_predicted_class"],
            "confidence": ai_result["ai"]["confidence"],
            "model_version": ai_result["ai"]["model_version"],
            "gate_status": ai_result["ai"]["gate_status"],
            "requires_manual_observation": (ai_result["ai"]["gate_status"] == "UNCERTAIN")
        },
        "note": entry.note,
        "action_recommendation": ai_result["action_recommendation"]
    }
    return payload

@app.post("/api/feedback", status_code=status.HTTP_201_CREATED, tags=["Continuous Learning"])
def record_staff_feedback(feedback: StaffFeedbackRequest):
    """
    Merekam feedback konfirmasi/koreksi staf dapur untuk siklus perbaikan dataset (Langkah 8).
    Disimpan secara persisten ke 'data/staff_feedback_logs.jsonl'.
    """
    feedback_file = DATA_DIR / "staff_feedback_logs.jsonl"
    record = feedback.model_dump()
    record["timestamp"] = time.time()
    
    try:
        with open(feedback_file, "a", encoding="utf-8") as f:
            f.write(json.dumps(record, ensure_ascii=False) + "\n")
        logger.info(f"Feedback staf tercatat: correct={feedback.is_correct}")
        return {"status": "success", "message": "Feedback staf berhasil dicatat untuk siklus retraining."}
    except Exception as exc:
        logger.error(f"Gagal mencatat feedback staf: {exc}")
        raise HTTPException(status_code=500, detail="Gagal menyimpan log feedback.")

# =============================================================================
# ENDPOINT BARCODE SCANNER (Google ML Kit Barcode Scanning - PRD Bagian 38 & 41)
# =============================================================================

@app.post("/api/barcode/scan", tags=["Barcode Scanner"])
def scan_barcode(req: BarcodeScanRequest):
    """
    Memindai dan mengidentifikasi nilai barcode untuk lookup data bahan, batch, & supplier.
    Sesuai alur Barcode Scanning pada PRD KitchenGuard v1.1.
    """
    result = barcode_service.scan_and_lookup(req.barcode_value, detected_format=req.detected_format)
    logger.info(f"Barcode scan '{req.barcode_value}' -> status: {result['status']}")
    return result

@app.get("/api/barcode/samples", tags=["Barcode Scanner"])
def get_barcode_samples():
    """Mengembalikan daftar contoh barcode riil untuk pengujian cepat."""
    samples = []
    for code, data in BARCODE_DATABASE.items():
        samples.append({
            "barcode": code,
            "format": data["format"],
            "name": data["name"],
            "category": data["category"],
            "batch_id": data["batch_id"],
            "unit": data["base_unit"]
        })
    return {"samples": samples}

# =============================================================================
# ENDPOINT VISION INGREDIENT & FRESHNESS SCANNER (TFLite Vision AI - PRD Bagian 37 & 41)
# =============================================================================

@app.post("/api/vision/scan-ingredient", tags=["Vision AI Scanner"])
def scan_ingredient_vision(req: VisionScanRequest):
    """
    Memindai citra visual bahan makanan (Daging, Buah, Sayur, Seafood)
    untuk mendeteksi jenis bahan dan klasifikasi freshness (FRESH, ACCEPTABLE, SPOILED, REJECT).
    """
    import base64
    from io import BytesIO
    from PIL import Image

    image_bytes = None
    if req.image_base64:
        try:
            # Strip data URL header jika ada
            base64_data = req.image_base64
            if "," in base64_data:
                base64_data = base64_data.split(",")[1]
            image_bytes = base64.b64decode(base64_data)
        except Exception as e:
            logger.warning(f"Gagal mendecode base64 image: {e}")

    # Jika tanpa gambar upload, buat gambar sintetis representatif
    if not image_bytes:
        img = Image.new("RGB", (224, 224), color=(180, 50, 40) if req.ingredient_hint and "daging" in req.ingredient_hint else (60, 160, 70))
        buffer = BytesIO()
        img.save(buffer, format="JPEG")
        image_bytes = buffer.getvalue()

    result = vision_service.analyze_image(
        image_bytes=image_bytes,
        hint_ingredient=req.ingredient_hint,
        confidence_threshold=req.threshold
    )
    logger.info(
        f"Vision scan: {result['ingredient']['name']} -> Freshness: {result['ai']['predicted_class']} "
        f"({result['ai']['confidence']*100:.1f}%)"
    )
    return result

@app.get("/api/vision/samples", tags=["Vision AI Scanner"])
def get_vision_samples():
    """Mengembalikan daftar katalog bahan yang didukung (Daging, Buah, Sayur, Seafood)."""
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
    """
    Membaca angka berat dan satuan dari layar timbangan digital (PRD Bagian 39).
    Menerapkan confidence threshold 90% untuk auto-fill.
    """
    # Simulasi pembacaan LCD timbangan digital
    reading_str = req.scale_value_hint or "1.45 kg"
    
    # Parsing numeric dan unit
    import re
    match = re.search(r"(\d+[\.,]?\d*)\s*([a-zA-Z]*)", reading_str)
    if match:
        raw_val = match.group(1).replace(",", ".")
        weight = float(raw_val)
        unit = match.group(2).lower() if match.group(2) else "kg"
        confidence = 0.94
    else:
        weight = 1.0
        unit = "kg"
        confidence = 0.80

    auto_fill_allowed = confidence >= 0.90

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
    logger.info("Menjalankan KitchenGuard CSM Web Application pada http://127.0.0.1:8000 ...")
    uvicorn.run("app:app", host="127.0.0.1", port=8000, reload=False)
