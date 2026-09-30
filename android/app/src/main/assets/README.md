# Assets — On-Device AI Freshness Model

Folder ini menampung model TensorFlow Lite yang dijalankan **offline di perangkat**
(PRD Section 16 — On-Device AI Freshness Classification).

## File yang dibutuhkan

| File | Status | Keterangan |
|---|---|---|
| `freshness-v1.tflite` | **WAJIB disediakan oleh tim ML** | Model klasifikasi kesegaran bahan |

## Cara mengaktifkan AI Freshness

1. Latih / dapatkan model `freshness-v1.tflite` (lihat spesifikasi di bawah).
2. Letakkan file tersebut **tepat di folder ini**, dengan nama persis:
   ```
   app/src/main/assets/freshness-v1.tflite
   ```
3. Rebuild aplikasi. `FreshnessClassifier` akan otomatis memuatnya saat startup.

Tidak ada perubahan kode yang diperlukan — aplikasi mendeteksi keberadaan file
saat runtime.

## Spesifikasi model yang diharapkan

| Aspek | Nilai |
|---|---|
| Format | TensorFlow Lite (`.tflite`) |
| Input | `[1, 224, 224, 3]`, Float32, normalisasi `[-1.0, 1.0]` |
| Output | `[1, 4]`, probabilitas softmax |
| Urutan kelas (index 0–3) | `FRESH`, `ACCEPTABLE`, `SPOILED`, `REJECT` |
| Versi dicatat | `freshness-v1.0` (lihat `FreshnessClassifier.MODEL_VERSION`) |

Aturan **confidence gate** (PRD Section 16):

- `confidence >= 0.85` → kelas ditampilkan sebagai *decision support*.
- `confidence < 0.85` → output menjadi `UNCERTAIN`; staff wajib observasi manual.
- AI **tidak boleh** melakukan auto-reject secara mandiri.

## Perilaku bila model tidak ada (by design)

Sesuai Issue #7, aplikasi **tidak** lagi memakai hasil acak (mock) sebagai
pengganti. Bila `freshness-v1.tflite` tidak tersedia:

- `FreshnessClassifier.isReady` bernilai `false`.
- `classifyImage()` mengembalikan `FreshnessResult` dengan
  `isModelAvailable = false`, `predictedClass = "UNCERTAIN"`, dan `errorMessage` berisi alasan.
- UI (`AiVerificationScreen`) menampilkan status **"AI Tidak Tersedia — Menunggu
  Observasi Manual"** secara jujur.

Lihat `presentation/screens/waste/AiVerificationViewModel.kt` untuk alur state-nya.
