# 📱 Android ML & Backend API Contract Synchronization
> **Target Repository**: `TIM_2_ANDROID`  
> **Android Lead**: Dewa  
> **Backend / ML Repository**: `TIM2_Capstone_MachineLearning`  
> **Specification Version**: `v3.1.0` (Updated: September 2026)

---

## 1. Ringkasan Status Sinkronisasi
Dokumen ini adalah acuan resmi sinkronisasi kontrak payload antara model Machine Learning (FastAPI Backend) dan aplikasi Android (`TIM_2_ANDROID` / Dewa).

Semua key JSON telah diselaraskan dengan data class Retrofit Kotlin (`com.kitchenguard.csm.model.Models.kt`) dan interface `ApiService.kt`.

---

## 2. Definisi Kategori Limbah (6 Kelas Standar PRD)

Model klasifikasi teks dan helper Android menggunakan urutan indeks dan label kelas berikut:

| Index | Kategori (`class`) | Deskripsi Dapur | Hygiene Priority | Threshold Disetujui |
|---|---|---|---|---|
| `0` | **`CONTAMINATED`** | Kontaminasi benda asing/kimia/jatuh ke lantai | `CRITICAL` | ≥ 0.85 (85%) |
| `1` | **`EXPIRED`** | Bahan melewati tanggal kadaluarsa (FIFO) | `HIGH` | ≥ 0.85 (85%) |
| `2` | **`OVERCOOKED`** | Kesalahan masak/gosong/hangus | `MEDIUM` | ≥ 0.85 (85%) |
| `3` | **`PREP_WASTE`** | Sisa kupasan/trimming bahan baku | `LOW` | ≥ 0.85 (85%) |
| `4` | **`SPOILED`** | Pembusukan alami/berlendir/jamur/bau basi | `HIGH` | ≥ 0.85 (85%) |
| `5` | **`SURPLUS`** | Makanan berlebih tidak terjual/sisa prasmanan | `LOW` | ≥ 0.85 (85%) |

---

## 3. Spesifikasi Endpoint Retrofit & Payload JSON

### A. Endpoint Utama: Submit Waste Log
- **HTTP Method**: `POST`
- **Path**: `/api/waste-log/analyze`
- **Kotlin Retrofit**: `ApiService.submitWasteLog(WasteLogSubmissionRequest): Call<WasteLogSubmissionResponse>`

#### Request Payload:
```json
{
  "barcode_value": "8991234567890",
  "ingredient_name": "Daging Sapi Wagyu Ribeye MB7",
  "batch_id": "WG-0915-A",
  "ocr_weight": 1.45,
  "ocr_unit": "kg",
  "ocr_confidence": 0.96,
  "note": "Daging sapi berbau busuk dan berlendir saat dikeluarkan dari chiller",
  "reported_by": "Staff Dapur"
}
```

#### Response Payload:
```json
{
  "timestamp": "2026-09-18T16:15:00.123456",
  "reported_by": "Staff Dapur",
  "ingredient": {
    "name": "Daging Sapi Wagyu Ribeye MB7",
    "batch_id": "WG-0915-A"
  },
  "barcode": {
    "value": "8991234567890",
    "detected": true
  },
  "ocr": {
    "weight": 1.45,
    "unit": "kg",
    "confidence": 0.96,
    "auto_filled": true
  },
  "ai": {
    "task": "waste_reason_classification",
    "class": "SPOILED",
    "raw_class": "SPOILED",
    "confidence": 0.992,
    "gate_status": "APPROVED",
    "model_version": "3.0.0",
    "requires_manual_observation": false
  },
  "note": "Daging sapi berbau busuk dan berlendir saat dikeluarkan dari chiller",
  "action_recommendation": "Reject & Pisahkan segera dari area penyimpanan dingin. Buang ke limbah organik untuk mencegah kontaminasi silang.",
  "financial_impact": {
    "category": "SPOILED",
    "weight_kg": 1.45,
    "cost_per_kg_rupiah": 485000.0,
    "disposal_factor": 1.15,
    "ingredient_loss_rupiah": 703250.0,
    "disposal_cost_rupiah": 91728.0,
    "total_loss_rupiah": 794978.0,
    "priority_level": "HIGH",
    "action_recommendation": "Segera pisahkan dari bahan segar lainnya untuk mencegah pembusukan silang."
  }
}
```

> **Catatan Kompatibilitas untuk Android (`WasteAIMeta`)**:  
> Backend menyediakan field **`"class"`** dan **`"predicted_class"`**, sehingga parsing Gson pada model Kotlin:
> ```kotlin
> data class WasteAIMeta(
>     @SerializedName("task") val task: String,
>     @SerializedName("class") val predictedClass: String,
>     @SerializedName("confidence") val confidence: Double,
>     @SerializedName("gate_status") val gateStatus: String,
>     @SerializedName("model_version") val modelVersion: String
> )
> ```
> bekerja 100% tanpa error mapping.

---

### B. Endpoint Barcode Scanner
- **HTTP Method**: `POST`
- **Path**: `/api/barcode/scan`
- **Kotlin Retrofit**: `ApiService.scanBarcode(BarcodeRequest): Call<BarcodeResponse>`

#### Request Payload:
```json
{
  "barcode_value": "8991234567890",
  "detected_format": "EAN_13"
}
```

#### Response Payload (Ditemukan):
```json
{
  "status": "SUCCESS",
  "barcode": {
    "value": "8991234567890",
    "format": "EAN_13",
    "scan_timestamp": "2026-09-18T16:15:00Z",
    "latency_ms": 1.45
  },
  "ingredient": {
    "id": "ING-MEAT-001",
    "name": "Daging Sapi Wagyu Ribeye MB7",
    "category": "Daging & Unggas",
    "batch_id": "WG-0915-A",
    "supplier": "PT Agro Boga Utama",
    "base_unit": "kg",
    "unit_price": 485000,
    "expiry_at": "2026-09-22",
    "storage_location": "Chiller Daging (0°C - 2°C)"
  },
  "fallback_required": false
}
```

---

### C. Endpoint Freshness Vision Analysis
- **HTTP Method**: `POST`
- **Path**: `/api/vision/scan-ingredient`
- **Kotlin Retrofit**: `ApiService.scanIngredient(VisionRequest): Call<VisionResponse>`

#### Request Payload:
```json
{
  "image_base64": "<BASE64_IMAGE_STRING>",
  "ingredient_hint": "daging_sapi",
  "threshold": 0.85
}
```

---

### D. Endpoint OCR Timbangan Digital
- **HTTP Method**: `POST`
- **Path**: `/api/ocr/scan-scale`
- **Kotlin Retrofit**: `ApiService.scanScale(OCRScaleRequest): Call<OCRScaleResponse>`

#### Request Payload:
```json
{
  "scale_value_hint": "1.45 kg"
}
```

---

### E. Endpoint Healthcheck
- **HTTP Method**: `GET`
- **Path**: `/api/health`

#### Response Payload:
```json
{
  "status": "healthy",
  "service": "KitchenGuard CSM Backend",
  "version": "3.0.0",
  "model_loaded": true,
  "active_classes": [
    "CONTAMINATED",
    "EXPIRED",
    "OVERCOOKED",
    "PREP_WASTE",
    "SPOILED",
    "SURPLUS"
  ]
}
```

---

## 4. Konfigurasi Jaringan & Base URL Android

Di Android `RetrofitClient.kt`:
```kotlin
object RetrofitClient {
    // Gunakan 10.0.2.2 untuk Android Emulator
    // Gunakan IP lokal PC (misal: 192.168.x.x) saat run di Device Fisik
    private const val BASE_URL = "http://10.0.2.2:8000/"
    ...
}
```

---

## 5. Sinkronisasi Artifact Offline / Android Helper

Bagi tim Android jika ingin menggunakan offline classification fallback di device:
- Map indeks: `models/android/android_classification_map.json`
- Class encoding: `models/android/android_encoding_map.json`
- Helper Java: `models/android/WasteClassifierHelper.java`
