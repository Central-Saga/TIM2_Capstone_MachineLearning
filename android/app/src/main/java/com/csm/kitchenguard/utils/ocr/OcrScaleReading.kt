package com.csm.kitchenguard.utils.ocr

/**
 * Hasil satu pembacaan OCR dari layar timbangan digital.
 *
 * PRD Section 15 — OCR Digital Scale.
 *
 * PENTING (Issue #8):
 * [confidence] adalah skor keyakinan yang berasal dari ML Kit Text Recognition
 * (`Text.Line.getConfidence()` / `Text.Element.getConfidence()`), BUKAN rasio
 * panjang teks terhadap regex. Nilainya merepresentasikan seberapa yakin mesin
 * OCR terhadap bacaan tersebut.
 *
 * Tipe data sengaja [Float] karena itulah tipe asli yang disediakan ML Kit
 * (`getConfidence(): float`). Menjaga tipe asli menghindari galat presisi saat
 * membandingkan dengan ambang batas 0.90.
 *
 * Bila ML Kit tidak menyediakan nilai confidence yang valid pada perangkat
 * tertentu (mis. `NaN` atau tidak diisi), [confidenceAvailable] bernilai `false`
 * dan [confidence] di-set ke [CONFIDENCE_UNAVAILABLE]. Dalam kondisi ini sistem
 * WAJIB meminta konfirmasi manual dan TIDAK boleh auto-fill.
 */
data class OcrScaleReading(
    /** Teks mentah baris yang terdeteksi (sudah di-lowercase & trim). */
    val rawText: String,

    /**
     * Confidence dari ML Kit (0.0 - 1.0), atau [CONFIDENCE_UNAVAILABLE]
     * bila mesin OCR tidak menyediakannya.
     */
    val confidence: Float,

    /** True bila [confidence] berasal dari ML Kit dan valid. */
    val confidenceAvailable: Boolean
) {
    companion object {
        /**
         * Penanda confidence tidak tersedia.
         * Sengaja negatif agar selalu gagal terhadap gate >= 0.90.
         */
        const val CONFIDENCE_UNAVAILABLE = -1.0f

        /**
         * Membentuk [OcrScaleReading] sambil memvalidasi kelayakan nilai confidence
         * dari ML Kit. Nilai `NaN` atau di luar rentang [0.0, 1.0] diperlakukan
         * sebagai "tidak tersedia".
         *
         * @param rawText teks baris hasil OCR
         * @param confidence nilai mentah dari `getConfidence()` ML Kit
         */
        fun of(rawText: String, confidence: Float): OcrScaleReading {
            val isUsable = !confidence.isNaN() && confidence in 0.0f..1.0f
            return OcrScaleReading(
                rawText = rawText,
                confidence = if (isUsable) confidence else CONFIDENCE_UNAVAILABLE,
                confidenceAvailable = isUsable
            )
        }
    }
}
