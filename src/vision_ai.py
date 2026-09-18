"""
KitchenGuard CSM - Vision Ingredient & Freshness Service (Alias/Wrapper)
Disediakan untuk backward compatibility dengan script legacy.
Implementasi inti didelegasikan ke src/vision_service.py.
"""
from src.vision_service import (
    KitchenGuardVisionService,
    vision_service,
    INGREDIENTS_CATALOG
)

__all__ = ["KitchenGuardVisionService", "vision_service", "INGREDIENTS_CATALOG"]
