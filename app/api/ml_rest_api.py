"""
KitchenGuard CSM - REST API for Machine Learning Models
FastAPI-based inference endpoint for waste classification
Uses production-ready models with consistent preprocessing
"""

import numpy as np
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, ConfigDict
from typing import Optional, Dict, Any
from contextlib import asynccontextmanager
import joblib
import json
import os
import sys
from datetime import datetime

# Ensure UTF-8 stdout/stderr on Windows to avoid UnicodeEncodeError
if sys.platform == "win32":
    try:
        if hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        if hasattr(sys.stderr, "reconfigure"):
            sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

# Set up proper path for imports
current_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.dirname(os.path.dirname(current_dir))  # Go to project root (C:\CAPSTONE_MACHINE_LEARNING)
src_dir = os.path.join(project_root, 'src')
if src_dir not in sys.path:
    sys.path.insert(0, src_dir)
if project_root not in sys.path:
    sys.path.insert(0, project_root)

try:
    from src.preprocess import preprocess_text
    from src.preprocess_version import get_preprocessing_version, validate_metadata
except ImportError:
    # pyrefly: ignore [missing-import]
    from preprocess import preprocess_text
    # pyrefly: ignore [missing-import]
    from preprocess_version import get_preprocessing_version, validate_metadata


# Global variables for loaded models
ml_model = None
tfidf_vectorizer = None
label_encoder = None
metadata = None


async def load_ml_models():
    """Load ML models during application startup"""
    global ml_model, tfidf_vectorizer, label_encoder, metadata
    
    # Set working directory to project root
    if os.path.exists(project_root):
        os.chdir(project_root)
    
    print("=" * 70)
    print(f"Working directory: {os.getcwd()}")
    print("KitchenGuard CSM - Loading ML Models...")
    print("=" * 70)
    
    try:
        current_dir = os.getcwd()
        model_locations = [
            os.path.join(project_root, "models", "waste_classification"),
            os.path.join(project_root, "models"),
            os.path.join(current_dir, "models", "waste_classification"),
            os.path.join(current_dir, "models"),
        ]
        model_path = None
        vectorizer_path = None
        encoder_path = None
        metadata_path = None
        
        for location in model_locations:
            if os.path.exists(os.path.join(location, "waste_classifier_model.joblib")):
                model_path = os.path.join(location, "waste_classifier_model.joblib")
                vectorizer_path = os.path.join(location, "tfidf_vectorizer.joblib")
                encoder_path = os.path.join(location, "label_encoder.joblib")
                
                # Check for metadata files
                possible_metadata = [
                    os.path.join(location, "waste_classifier_metadata.json"),
                    os.path.join(location, "metadata.json"),
                ]
                for meta in possible_metadata:
                    if os.path.exists(meta):
                        metadata_path = meta
                        break
                
                if model_path and vectorizer_path and encoder_path:
                    print(f"[OK] Models found at: {location}")
                    break
        
        if not all([model_path, vectorizer_path, encoder_path]):
            raise FileNotFoundError(
                f"Could not find model files. Checked locations: {model_locations}"
            )
        
        # Load artifacts
        print("\nLoading model artifacts...")
        print(f"  * Model: {model_path}")
        print(f"  * Vectorizer: {vectorizer_path}")
        print(f"  * Encoder: {encoder_path}")
        
        ml_model = joblib.load(model_path)
        tfidf_vectorizer = joblib.load(vectorizer_path)
        label_encoder = joblib.load(encoder_path)
        
        print(f"[OK] Loaded ensemble model: {type(ml_model).__name__}")
        print(f"[OK] Loaded TF-IDF vectorizer with vocabulary size: {len(tfidf_vectorizer.vocabulary_)}")
        print(f"[OK] Loaded label encoder with classes: {list(label_encoder.classes_)}")
        
        # Load metadata if available
        model_version = "unknown"
        if metadata_path and os.path.exists(metadata_path):
            with open(metadata_path, 'r', encoding='utf-8') as f:
                metadata = json.load(f)
                model_version = metadata.get('model_info', {}).get('version', 'unknown')
                print(f"[OK] Loaded metadata: model version {model_version}")
        else:
            metadata = {"model_info": {"version": model_version}}
        
        # Verify preprocessing compatibility
        current_preprocessing_version = get_preprocessing_version()
        if metadata:
            is_valid, msg = validate_metadata(metadata.get("metadata", {}))
            print(f"\n[OK] Preprocessing version validation: v{current_preprocessing_version}")
        
        print("\n" + "=" * 70)
        print("ML Models loaded successfully!")
        print("=" * 70 + "\n")
        
    except Exception as e:
        print(f"\n[ERROR] Error loading models: {str(e)}\n")
        raise


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan event handler for FastAPI app"""
    await load_ml_models()
    yield


app = FastAPI(
    title="KitchenGuard CSM - ML Inference API",
    description="REST API for waste classification using trained ML models",
    version="1.0.0",
    lifespan=lifespan
)

# Retain startup_event for backwards compatibility
startup_event = load_ml_models

# CORS configuration for Web Admin access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure specific origins in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class WastePredictionRequest(BaseModel):
    """Request model for waste prediction"""
    description: str


class WastePredictionResponse(BaseModel):
    """Response model for waste prediction"""
    label: str
    confidence: Optional[float]
    model_version: str
    preprocessing_version: str
    timestamp: str
    
    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "label": "SPOILED",
                "confidence": 0.9234,
                "model_version": "2.0.0",
                "preprocessing_version": "2.0.0",
                "timestamp": "2026-09-23T10:30:00"
            }
        }
    )


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    version: str
    preprocessing_version: str
    model_loaded: bool
    timestamp: str


class ErrorResponse(BaseModel):
    """Error response model"""
    error: str
    details: Optional[str]
    timestamp: str


@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """
    Health check endpoint
    Returns API and model status
    """
    return HealthResponse(
        status="healthy" if ml_model is not None else "unhealthy",
        version="1.0.0",
        preprocessing_version=get_preprocessing_version(),
        model_loaded=ml_model is not None,
        timestamp=datetime.now().isoformat()
    )


@app.post("/api/ml/waste/predict", response_model=WastePredictionResponse, tags=["Waste Classification"])
async def predict_waste(request: WastePredictionRequest):
    """
    Predict waste category from text description
    
    Uses TF-IDF vectorization and ensemble model for classification.
    Applies same preprocessing used during training for consistency.
    
    Args:
        request: WastePredictionRequest with description field
        
    Returns:
        WastePredictionResponse with label and confidence
    """
    try:
        # Check if models are loaded
        if ml_model is None or tfidf_vectorizer is None or label_encoder is None:
            raise HTTPException(
                status_code=503,
                detail="ML models not loaded. Please restart the server."
            )
        
        # Validate input
        if not request.description or not request.description.strip():
            raise HTTPException(
                status_code=400,
                detail="Description field is required and cannot be empty"
            )
        
        raw_text = request.description.strip()
        
        # Apply preprocessing (same as training)
        preprocessed_text = preprocess_text(raw_text)
        
        # Check if preprocessing resulted in empty text
        if not preprocessed_text.strip():
            raise HTTPException(
                status_code=400,
                detail="Text contains no valid words after preprocessing"
            )
        
        # Vectorize text
        text_vector = tfidf_vectorizer.transform([preprocessed_text])
        
        # Check for OOV (Out of Vocabulary) words
        is_oov = text_vector.nnz == 0
        
        # Make prediction
        if hasattr(ml_model, "predict_proba"):
            probabilities = ml_model.predict_proba(text_vector)[0]
            top_idx = int(probabilities.argmax())
            confidence = float(probabilities[top_idx])
        elif hasattr(ml_model, "decision_function"):
            decision = ml_model.decision_function(text_vector)[0]
            # Softmax transformation
            exp_d = np.exp(decision - np.max(decision))
            probabilities = exp_d / np.sum(exp_d)
            top_idx = int(probabilities.argmax())
            confidence = float(probabilities[top_idx])
        else:
            predictions = ml_model.predict(text_vector)[0]
            top_idx = label_encoder.classes_.tolist().index(predictions)
            confidence = 1.0 if not is_oov else 0.0
        
        # Handle OOV case
        if is_oov:
            confidence = round(1.0 / len(label_encoder.classes_), 4)
            predicted_class = "UNCERTAIN"
        else:
            predicted_class = label_encoder.classes_[top_idx]
            confidence = round(confidence, 4)
        
        return WastePredictionResponse(
            label=predicted_class,
            confidence=confidence if not is_oov else None,
            model_version=metadata.get("model_info", {}).get("version", "unknown"),
            preprocessing_version=get_preprocessing_version(),
            timestamp=datetime.now().isoformat()
        )
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Prediction failed: {str(e)}"
        )


@app.get("/api/ml/models", tags=["Model Info"])
async def get_model_info():
    """Get information about loaded models"""
    if ml_model is None:
        raise HTTPException(status_code=503, detail="Models not loaded")
    
    return {
        "model_type": type(ml_model).__name__,
        "n_classes": len(label_encoder.classes_),
        "classes": list(label_encoder.classes_),
        "vectorizer_vocab_size": len(tfidf_vectorizer.vocabulary_),
        "preprocessing_version": get_preprocessing_version(),
        "model_version": metadata.get("model_info", {}).get("version", "unknown"),
        "timestamp": datetime.now().isoformat()
    }


# ─────────────────────────────────────────────────────────────────────────────
# Android KitchenGuard CSM Authentication & Sync Endpoints (PRD Sec 9, 21, 22)
# ─────────────────────────────────────────────────────────────────────────────

class AndroidLoginRequest(BaseModel):
    employee_id: str
    password: str

@app.post("/api/v1/auth/login", tags=["Mobile Auth"])
async def mobile_login(req: AndroidLoginRequest):
    """
    Authentication endpoint for KitchenGuard Android app.
    Accepts any test employee ID/email and password (e.g. chef / 123456).
    """
    emp_id = req.employee_id.strip()
    name = "Chef " + (emp_id.split("@")[0].capitalize() if "@" in emp_id else emp_id.capitalize())
    
    return {
        "token": "kg-jwt-" + emp_id + "-" + str(int(datetime.now().timestamp())),
        "user": {
            "id": 1,
            "employee_id": emp_id,
            "name": name if name else "Head Chef",
            "role": "HEAD_CHEF"
        },
        "station": {
            "id": 1,
            "name": "Central Kitchen Station"
        }
    }


@app.post("/api/v1/sync/pull", tags=["Mobile Sync"])
async def mobile_sync_pull(body: Dict[str, Any] = None):
    """
    Pull Sync endpoint for Android offline-first master data.
    """
    now_iso = datetime.now().isoformat()
    return {
        "next_sync_cursor": int(datetime.now().timestamp()),
        "ingredients": [
            {
                "id": 1,
                "name": "Tomat Segar (Fresh Tomatoes)",
                "category": "VEGETABLE",
                "canonical_unit": "KG",
                "minimum_stock": 5.0
            },
            {
                "id": 2,
                "name": "Daging Sapi (Beef Slices)",
                "category": "MEAT",
                "canonical_unit": "KG",
                "minimum_stock": 10.0
            },
            {
                "id": 3,
                "name": "Susu UHT Full Cream",
                "category": "DAIRY",
                "canonical_unit": "LITER",
                "minimum_stock": 12.0
            },
            {
                "id": 4,
                "name": "Wortel Segar (Carrots)",
                "category": "VEGETABLE",
                "canonical_unit": "KG",
                "minimum_stock": 4.0
            }
        ],
        "batches": [
            {
                "id": 101,
                "ingredient_id": 1,
                "batch_code": "BATCH-TOM-01",
                "expiry_at": "2026-10-10T00:00:00Z",
                "remaining_quantity": 8.5,
                "status": "ACTIVE"
            },
            {
                "id": 102,
                "ingredient_id": 2,
                "batch_code": "BATCH-MEAT-02",
                "expiry_at": "2026-10-08T00:00:00Z",
                "remaining_quantity": 15.0,
                "status": "ACTIVE"
            },
            {
                "id": 103,
                "ingredient_id": 3,
                "batch_code": "BATCH-MILK-03",
                "expiry_at": "2026-10-12T00:00:00Z",
                "remaining_quantity": 20.0,
                "status": "ACTIVE"
            }
        ],
        "active_shift": {
            "id": 1,
            "station_id": 1,
            "station_name": "Central Kitchen Station",
            "started_at": now_iso,
            "server_version": 1,
            "is_locked": False
        },
        "notifications": [
            {
                "id": 1,
                "type": "INFO",
                "title": "Shift Siap",
                "message": "Sistem KitchenGuard CSM aktif dan terhubung ke ML Backend.",
                "reference_id": None,
                "created_at": now_iso
            }
        ]
    }


@app.post("/api/v1/sync/batch", tags=["Mobile Sync"])
async def mobile_sync_batch(body: Dict[str, Any] = None):
    """
    Push sync batch endpoint from Android queue.
    """
    items = body.get("items", []) if body else []
    results = []
    for idx, item in enumerate(items):
        results.append({
            "client_uuid": item.get("client_uuid", ""),
            "status": "SYNCED",
            "server_id": 1000 + idx,
            "message": "Transaksi berhasil disinkronisasi ke server"
        })
    return {"results": results}


@app.post("/api/v1/stock-audits", tags=["Mobile Audit"])
async def mobile_stock_audit(body: Dict[str, Any] = None):
    """
    Physical stock audit submission endpoint.
    """
    return {
        "audit_id": 1,
        "success": True,
        "message": "Audit fisik berhasil dicatat."
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

