"""
Integration and regression test suite for KitchenGuard CSM FastAPI endpoints.
Ensures that all endpoints are functional, models are loaded, and regressions (such as 503 errors) are caught in CI.
"""
import pytest
from fastapi.testclient import TestClient
from app import app

client = TestClient(app)

class TestHealthEndpoint:
    def test_health_check_healthy(self):
        """Verifies /api/health reports healthy and ML model is ready."""
        resp = client.get("/api/health")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "healthy"
        assert data["ml_available"] is True
        assert data["categories_count"] == 6

class TestPredictEndpoint:
    def test_predict_spoiled_meat(self):
        """Verifies /api/predict correctly classifies spoiled meat."""
        payload = {
            "text": "Daging sapi berbau busuk menyengat dan berlendir kehijauan di chiller",
            "threshold": 0.85,
            "estimated_weight_kg": 2.5
        }
        resp = client.post("/api/predict", json=payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["success"] is True
        assert data["ai"]["predicted_class"] == "SPOILED"
        assert data["ai"]["confidence"] >= 0.85
        assert data["ai"]["gate_status"] == "APPROVED"
        assert data["financial_impact"] is not None
        assert data["financial_impact"]["total_loss_rupiah"] > 0

    def test_predict_expired_dairy(self):
        """Verifies /api/predict correctly classifies expired milk."""
        payload = {
            "text": "Susu UHT telah melewati tanggal kadaluarsa expired date 3 hari lalu",
            "threshold": 0.85
        }
        resp = client.post("/api/predict", json=payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["ai"]["predicted_class"] == "EXPIRED"

    def test_predict_prep_waste(self):
        """Verifies /api/predict correctly classifies vegetable trimming."""
        payload = {
            "text": "Kulit kentang dan sisa potongan wortel dari prep pagi",
            "threshold": 0.85
        }
        resp = client.post("/api/predict", json=payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["ai"]["predicted_class"] == "PREP_WASTE"

    def test_predict_empty_text_raises_422_or_400(self):
        """Verifies /api/predict rejects empty or whitespace-only inputs."""
        resp = client.post("/api/predict", json={"text": ""})
        assert resp.status_code in [400, 422]

    def test_predict_oov_uncertain(self):
        """Verifies /api/predict handles out-of-vocabulary inputs gracefully as UNCERTAIN."""
        payload = {
            "text": "asdfghjk qwertyuiop zxcvbnm",
            "threshold": 0.85
        }
        resp = client.post("/api/predict", json=payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["ai"]["gate_status"] == "UNCERTAIN"

class TestBatchPredictEndpoint:
    def test_batch_predict(self):
        """Verifies /api/predict/batch processes multiple items."""
        payload = {
            "items": [
                "Ayam fillet berlendir bau busuk",
                "Susu rusak expired date lewat",
                "Kulit wortel kupasan"
            ],
            "threshold": 0.85
        }
        resp = client.post("/api/predict/batch", json=payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["total_processed"] == 3
        assert len(data["results"]) == 3

class TestBarcodeEndpoint:
    def test_barcode_scan_success(self):
        """Verifies /api/barcode/scan returns item for known barcode."""
        resp = client.post("/api/barcode/scan", json={"barcode_value": "8991234567890"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "SUCCESS"
        assert data["ingredient"]["name"] == "Daging Sapi Wagyu Ribeye MB7"

    def test_barcode_scan_unknown_not_found(self):
        """Verifies unknown barcode returns NOT_FOUND and does not falsely partial match."""
        resp = client.post("/api/barcode/scan", json={"barcode_value": "8999999999999"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "NOT_FOUND"

class TestScaleOCREndpoint:
    def test_scale_ocr_with_hint(self):
        """Verifies scale OCR parses valid reading."""
        resp = client.post("/api/ocr/scan-scale", json={"scale_value_hint": "2.75 kg"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "SUCCESS"
        assert data["ocr"]["weight"] == 2.75
        assert data["ocr"]["unit"] == "kg"

    def test_scale_ocr_missing_input_raises_400(self):
        """Verifies scale OCR requires either image or hint."""
        resp = client.post("/api/ocr/scan-scale", json={})
        assert resp.status_code == 400
