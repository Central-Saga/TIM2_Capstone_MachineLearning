"""
KitchenGuard CSM - Barcode Scanning & Inventory Lookup Service
Sesuai PRD KitchenGuard Bagian 38, 41, & 42
Mendukung format barcode: EAN-13, EAN-8, UPC-A, CODE-128, QR Code
"""

import time
from typing import Optional, Dict, Any

# Katalog Database Barcode Bahan Baku Dapur Komersial KitchenGuard
BARCODE_DATABASE = {
    "8991234567890": {
        "format": "EAN_13",
        "ingredient_id": "ING-MEAT-001",
        "name": "Daging Sapi Wagyu Ribeye MB7",
        "category": "Daging & Unggas",
        "batch_id": "WG-0915-A",
        "supplier": "PT Agro Boga Utama",
        "base_unit": "kg",
        "unit_price": 485000,
        "expiry_at": "2026-09-22",
        "storage": "Chiller Daging (0°C - 2°C)"
    },
    "8992345678901": {
        "format": "EAN_13",
        "ingredient_id": "ING-POUL-002",
        "name": "Daging Ayam Fillet Broiler",
        "category": "Daging & Unggas",
        "batch_id": "AY-0915-B",
        "supplier": "PT Japfa Comfeed",
        "base_unit": "kg",
        "unit_price": 48000,
        "expiry_at": "2026-09-18",
        "storage": "Chiller Unggas (1°C - 3°C)"
    },
    "8993456789012": {
        "format": "EAN_13",
        "ingredient_id": "ING-FRUT-003",
        "name": "Apel Fuji Merah Grade A",
        "category": "Buah-buahan",
        "batch_id": "APL-0914",
        "supplier": "Malang Fresh Fruit",
        "base_unit": "kg",
        "unit_price": 38000,
        "expiry_at": "2026-09-26",
        "storage": "Chiller Buah (4°C - 8°C)"
    },
    "8994567890234": {
        "format": "EAN_13",
        "ingredient_id": "ING-FRUT-004",
        "name": "Pisang Cavendish Super",
        "category": "Buah-buahan",
        "batch_id": "BAN-0914",
        "supplier": "Sunpride Indonesia",
        "base_unit": "kg",
        "unit_price": 24000,
        "expiry_at": "2026-09-20",
        "storage": "Suhu Ruang Sejuk (16°C - 18°C)"
    },
    "8995678901235": {
        "format": "EAN_13",
        "ingredient_id": "ING-VEGE-005",
        "name": "Tomat Ceri Merah Segar",
        "category": "Sayuran",
        "batch_id": "TMT-0915",
        "supplier": "Bedugul Hydroponics Bali",
        "base_unit": "kg",
        "unit_price": 32000,
        "expiry_at": "2026-09-21",
        "storage": "Chiller Sayur (8°C - 11°C)"
    },
    "8996789012346": {
        "format": "EAN_13",
        "ingredient_id": "ING-DAIR-006",
        "name": "Susu UHT Full Cream 1 Liter",
        "category": "Dairy & Olahan",
        "batch_id": "UHT-0820-C",
        "supplier": "PT Frisian Flag Indonesia",
        "base_unit": "liter",
        "unit_price": 21500,
        "expiry_at": "2026-09-14",
        "storage": "Dry Store / Chiller setelah buka"
    },
    "8997890123457": {
        "format": "EAN_13",
        "ingredient_id": "ING-VEGE-007",
        "name": "Wortel Brastagi Segar",
        "category": "Sayuran",
        "batch_id": "WRT-0915",
        "supplier": "CV Tani Makmur Berastagi",
        "base_unit": "kg",
        "unit_price": 18000,
        "expiry_at": "2026-09-25",
        "storage": "Chiller Sayur (2°C - 5°C)"
    },
    "8998901234568": {
        "format": "EAN_13",
        "ingredient_id": "ING-FISH-008",
        "name": "Ikan Salmon Fillet Norwegia",
        "category": "Seafood",
        "batch_id": "SLM-0915-A",
        "supplier": "Ocean Fresh Seafood",
        "base_unit": "kg",
        "unit_price": 320000,
        "expiry_at": "2026-09-17",
        "storage": "Chiller Seafood (-1°C - 2°C)"
    },
    "8999012345679": {
        "format": "EAN_13",
        "ingredient_id": "ING-VEGE-009",
        "name": "Kentang Granola Dieng Super",
        "category": "Sayuran",
        "batch_id": "KTG-0913",
        "supplier": "Koperasi Petani Dieng",
        "base_unit": "kg",
        "unit_price": 22000,
        "expiry_at": "2026-09-30",
        "storage": "Gudang Kering Sejuk Gelap"
    },
    "8990123456780": {
        "format": "EAN_13",
        "ingredient_id": "ING-VEGE-010",
        "name": "Paprika Merah Organik",
        "category": "Sayuran",
        "batch_id": "PAP-0915",
        "supplier": "Bali Organic Farm",
        "base_unit": "kg",
        "unit_price": 55000,
        "expiry_at": "2026-09-23",
        "storage": "Chiller Sayur (7°C - 10°C)"
    },
    # CODE-128 Internal Barcode Kitchen
    "KG-BEEF-9021": {
        "format": "CODE_128",
        "ingredient_id": "ING-MEAT-001",
        "name": "Daging Sapi Raw Prep Pack",
        "category": "Daging & Unggas",
        "batch_id": "INT-PREP-0915",
        "supplier": "Internal Butchery Station",
        "base_unit": "kg",
        "unit_price": 280000,
        "expiry_at": "2026-09-19",
        "storage": "Walk-in Chiller 1"
    }
}

class KitchenGuardBarcodeService:
    """Layanan Barcode Scanner & Inventori Lookup KitchenGuard CSM."""

    def __init__(self):
        self.db = BARCODE_DATABASE

    def lookup(self, barcode_value: str) -> Optional[Dict[str, Any]]:
        """Lookup langsung raw dictionary bahan baku berdasarkan nilai barcode."""
        if not barcode_value:
            return None
        clean_val = str(barcode_value).strip().replace(" ", "").replace("-", "")
        if barcode_value in self.db:
            return self.db[barcode_value]
        if clean_val in self.db:
            return self.db[clean_val]
        return None

    def scan_and_lookup(self, barcode_value: str, detected_format: str = "EAN_13") -> Dict[str, Any]:
        """
        Menerima nilai barcode yang dipindai kamera Android (Google ML Kit Barcode Scanning)
        dan melakukan lookup data bahan baku & batch.
        """
        start_time = time.perf_counter()
        clean_val = str(barcode_value).strip().replace(" ", "").replace("-", "")

        found = False
        item_data = None

        # Cek exact match
        if barcode_value in self.db:
            item_data = self.db[barcode_value]
            found = True
        elif clean_val in self.db:
            item_data = self.db[clean_val]
            found = True

        latency_ms = round((time.perf_counter() - start_time) * 1000, 2)

        if found and item_data:
            return {
                "status": "SUCCESS",
                "barcode": {
                    "value": barcode_value,
                    "format": item_data.get("format", detected_format),
                    "scan_timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
                    "latency_ms": latency_ms
                },
                "ingredient": {
                    "id": item_data["ingredient_id"],
                    "name": item_data["name"],
                    "category": item_data["category"],
                    "batch_id": item_data["batch_id"],
                    "supplier": item_data["supplier"],
                    "base_unit": item_data["base_unit"],
                    "unit_price": item_data["unit_price"],
                    "expiry_at": item_data["expiry_at"],
                    "storage_location": item_data["storage"]
                },
                "fallback_required": False
            }
        else:
            # Fallback manual sesuai PRD Bagian 38.1
            return {
                "status": "NOT_FOUND",
                "barcode": {
                    "value": barcode_value,
                    "format": detected_format,
                    "scan_timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
                    "latency_ms": latency_ms
                },
                "ingredient": None,
                "fallback_required": True,
                "message": "Barcode tidak terdaftar di katalog inventori. Silakan pilih bahan secara manual."
            }

barcode_service = KitchenGuardBarcodeService()
