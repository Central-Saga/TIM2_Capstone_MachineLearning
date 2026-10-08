"""
KitchenGuard CSM - Inference & Prediction Module (Alias/Wrapper)
Disediakan untuk backward compatibility dengan script legacy.
Implementasi inti didelegasikan ke src/predict.py.
"""
try:
    from src.predict import (
        KitchenGuardPredictor,
        KitchenGuardTextPredictor
    )
except ImportError:
    from predict import (
        KitchenGuardPredictor,
        KitchenGuardTextPredictor
    )

__all__ = ["KitchenGuardPredictor", "KitchenGuardTextPredictor"]
