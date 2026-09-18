"""
KitchenGuard CSM - Barcode Scanning Service (Alias/Wrapper)
Disediakan untuk backward compatibility dengan script legacy.
Implementasi inti didelegasikan ke src/barcode_service.py.
"""
from src.barcode_service import (
    KitchenGuardBarcodeService,
    barcode_service,
    BARCODE_DATABASE
)

__all__ = ["KitchenGuardBarcodeService", "barcode_service", "BARCODE_DATABASE"]
