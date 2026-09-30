package com.csm.kitchenguard.domain.usecase

import com.csm.kitchenguard.utils.ocr.OcrScaleReading

/**
 * Model hasil validasi dan ekstraksi dari mesin OCR.
 */
data class OcrValidationResult(
    val extractedQuantity: Double?,
    val extractedUnit: String?,

    /** True bila confidence OCR tersedia DAN >= 0.90 → nilai boleh mengisi form otomatis. */
    val isAutoFilled: Boolean,

    /** True bila staff wajib mengonfirmasi/mengoreksi nilai secara manual. */
    val needsManualConfirmation: Boolean,

    /**
     * True bila confidence asli tersedia dari mesin OCR (ML Kit).
     * False berarti confidence tidak dapat diandalkan → tidak boleh auto-fill.
     */
    val confidenceAvailable: Boolean,

    val originalText: String
)

/**
 * Use case untuk memvalidasi input teks dari ML Kit Text Recognition
 * di atas timbangan digital.
 * PRD Section 15 — OCR Digital Scale.
 *
 * KONTRAK CONFIDENCE (Issue #8):
 * [confidence] HARUS berasal dari ML Kit (`Text.Line.getConfidence()` /
 * `Text.Element.getConfidence()`), bukan rasio panjang teks terhadap regex.
 *
 * Aturan gate (PRD Section 15):
 * - `confidence >= 0.90` → angka boleh terisi otomatis (auto-fill).
 * - `confidence < 0.90` ATAU confidence tidak tersedia → wajib konfirmasi manual.
 *
 * OCR tidak pernah melakukan auto-submit; pengguna tetap menekan tombol konfirmasi.
 */
class ValidateOcrWeightUseCase {

    companion object {
        /** Ambang auto-fill sesuai PRD Section 15. Tipe Float mengikuti ML Kit. */
        const val OCR_CONFIDENCE_THRESHOLD = 0.90f

        // Regex untuk mencari pola angka desimal (termasuk koma eropa/indo)
        // yang mungkin diikuti oleh spasi dan karakter huruf.
        // Contoh: "1.25 kg", "500", "3,5g"
        private val WEIGHT_PATTERN = Regex("""(\d+[.,]?\d*)\s*([a-zA-Z]+)?""")
    }

    /**
     * Entry point utama: mengevaluasi pembacaan OCR yang membawa metadata
     * ketersediaan confidence.
     */
    operator fun invoke(reading: OcrScaleReading): OcrValidationResult {
        val cleanText = reading.rawText.replace("\n", " ").trim()

        // 1. Ekstraksi Angka dan Satuan via Regex
        var quantity: Double? = null
        var unit: String? = null

        val matchResult = WEIGHT_PATTERN.find(cleanText)
        if (matchResult != null) {
            val numberStr = matchResult.groupValues[1].replace(",", ".")
            quantity = numberStr.toDoubleOrNull()

            if (matchResult.groupValues.size >= 3 && matchResult.groupValues[2].isNotEmpty()) {
                unit = matchResult.groupValues[2].lowercase()
            }
        }

        // 2. Terapkan OCR Confidence Gate.
        //    Auto-fill HANYA bila confidence tersedia DAN >= threshold.
        val confidenceAvailable = reading.confidenceAvailable
        val isReliable = confidenceAvailable && reading.confidence >= OCR_CONFIDENCE_THRESHOLD
        val isAutoFilled = isReliable && quantity != null

        // 3. Wajib konfirmasi manual bila confidence tidak tersedia, di bawah
        //    threshold, atau angka gagal di-parse.
        val needsConfirmation = !isReliable || quantity == null

        return OcrValidationResult(
            extractedQuantity = quantity,
            extractedUnit = unit,
            isAutoFilled = isAutoFilled,
            needsManualConfirmation = needsConfirmation,
            confidenceAvailable = confidenceAvailable,
            originalText = cleanText
        )
    }

    /**
     * Overload kompatibilitas: menerima teks mentah + confidence (0.0 - 1.0).
     *
     * Nilai confidence di luar rentang [0.0, 1.0] (termasuk NaN) diperlakukan
     * sebagai "tidak tersedia" sehingga gate menuntut konfirmasi manual.
     */
    operator fun invoke(ocrText: String, confidence: Double): OcrValidationResult {
        val reading = OcrScaleReading.of(ocrText, confidence.toFloat())
        return invoke(reading)
    }
}
