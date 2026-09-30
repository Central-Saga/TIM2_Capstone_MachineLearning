"""
KitchenGuard CSM - REST API for Machine Learning Models
FastAPI-based inference endpoint for waste classification
Uses production-ready models with consistent preprocessing
"""

import numpy as np
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, Dict, Any
import joblib
import json
import os
import sys
from datetime import datetime

# Add src to path
# Set up proper path for imports
import sys
current_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.dirname(current_dir)  # Go to project root
src_dir = os.path.join(project_root, 'src')
sys.path.insert(0, src_dir)
os.chdir(project_root)  # Set working directory to project root

from preprocess import preprocess_text
from preprocess_version import get_preprocessing_version, validate_metadata
app = FastAPI(
    title="KitchenGuard CSM - ML Inference API",
    description="REST API for waste classification using trained ML models",
    version="1.0.0"
)

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
    
    class Config:
        json_schema_extra = {
            "example": {
                "label": "SPOILED",
                "confidence": 0.9234,
                "model_version": "2.0.0",
                "preprocessing_version": "2.0.0",
                "timestamp": "2026-09-23T10:30:00"
            }
        }


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


# Load models on startup
@app.on_event("startup")
async def startup_event():
    """Load ML models during application startup"""
    # CRITICAL: Set working directory to project root BEFORE loading models
    os.chdir(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
    
    print("=" * 70)
    print(f"Working directory: {os.getcwd()}")
    print("KitchenGuard CSM - Loading ML Models...")
    print("=" * 70)
    
    try:
        # Check multiple possible locations for models
        # Get absolute paths from current working directory (project root)
        current_dir = os.getcwd()
        model_locations = [
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
                    print(f"✅ Models found at: {location}")
                    break
        
        if not all([model_path, vectorizer_path, encoder_path]):
            raise FileNotFoundError(
                f"Could not find model files. Checked locations: {model_locations}"
            )
        
        # Load artifacts
        print("\nLoading model artifacts...")
        print(f"  • Model: {model_path}")
        print(f"  • Vectorizer: {vectorizer_path}")
        print(f"  • Encoder: {encoder_path}")
        
        global ml_model, tfidf_vectorizer, label_encoder, metadata
        
        ml_model = joblib.load(model_path)
        tfidf_vectorizer = joblib.load(vectorizer_path)
        label_encoder = joblib.load(encoder_path)
        
        print(f"✅ Loaded ensemble model: {type(ml_model).__name__}")
        print(f"✅ Loaded TF-IDF vectorizer with vocabulary size: {len(tfidf_vectorizer.vocabulary_)}")
        print(f"✅ Loaded label encoder with classes: {list(label_encoder.classes_)}")
        
        # Load metadata if available
        model_version = "unknown"
        if metadata_path and os.path.exists(metadata_path):
            with open(metadata_path, 'r', encoding='utf-8') as f:
                metadata = json.load(f)
                model_version = metadata.get('model_info', {}).get('version', 'unknown')
                print(f"✅ Loaded metadata: model version {model_version}")
        else:
            metadata = {"model_info": {"version": model_version}}
        
        # Verify preprocessing compatibility
        current_preprocessing_version = get_preprocessing_version()
        if metadata:
            is_valid, msg = validate_metadata(metadata.get("metadata", {}))
            print(f"\n✅ Preprocessing version validation: v{current_preprocessing_version}")
        
        print("\n" + "=" * 70)
        print("ML Models loaded successfully!")
        print("=" * 70 + "\n")
        
    except Exception as e:
        print(f"\n❌ Error loading models: {str(e)}\n")
        raise


# Global variables for loaded models
ml_model = None
tfidf_vectorizer = None
label_encoder = None
metadata = None


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


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
