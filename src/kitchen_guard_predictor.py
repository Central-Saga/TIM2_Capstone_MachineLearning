"""
KitchenGuard CSM - Inference & Prediction Module (Alias/Wrapper)
Disediakan untuk backward compatibility dengan script legacy.
Implementasi inti didelegasikan ke src/predict.py.
"""
from src.predict import (
    KitchenGuardPredictor,
    KitchenGuardPredictor as KitchenGuardTextPredictor
)

__all__ = ["KitchenGuardPredictor", "KitchenGuardTextPredictor"]
