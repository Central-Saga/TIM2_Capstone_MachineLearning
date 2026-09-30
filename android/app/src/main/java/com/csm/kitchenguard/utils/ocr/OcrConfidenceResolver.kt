package com.csm.kitchenguard.utils.ocr

/**
 * Logika murni untuk menurunkan confidence OCR dari nilai-nilai yang disediakan
 * ML Kit Text Recognition.
 *
 * Dipisahkan dari [com.csm.kitchenguard.presentation.components.camera.ScaleOcrAnalyzer]
 * agar dapat diuji tanpa Android/ML Kit runtime.
 *
 * Strategi (Issue #8):
 * 1. Prioritaskan confidence level baris (`Text.Line.getConfidence()`).
 * 2. Bila tidak valid, agregasikan (rata-rata) confidence level elemen
 *    (`Text.Element.getConfidence()`) yang valid.
 * 3. Bila keduanya tidak tersedia, kembalikan [OcrScaleReading.CONFIDENCE_UNAVAILABLE]
 *    (tidak ada tebakan / heuristik rasio regex).
 */
object OcrConfidenceResolver {

    /**
     * @param rawText            teks baris OCR
     * @param lineConfidence     nilai `Line.getConfidence()`
     * @param elementConfidences daftar nilai `Element.getConfidence()` untuk baris tsb
     */
    fun resolve(
        rawText: String,
        lineConfidence: Float,
        elementConfidences: List<Float>
    ): OcrScaleReading {
        // 1. Utamakan confidence level baris.
        if (isUsable(lineConfidence)) {
            return OcrScaleReading.of(rawText, lineConfidence)
        }

        // 2. Fallback: rata-rata confidence elemen yang valid.
        val validElements = elementConfidences.filter { isUsable(it) }
        if (validElements.isNotEmpty()) {
            val average = validElements.average().toFloat()
            return OcrScaleReading.of(rawText, average)
        }

        // 3. Tidak ada confidence yang dapat diandalkan → tandai tidak tersedia.
        return OcrScaleReading(
            rawText = rawText,
            confidence = OcrScaleReading.CONFIDENCE_UNAVAILABLE,
            confidenceAvailable = false
        )
    }

    /** True bila nilai confidence dari ML Kit valid dan dapat dipakai. */
    private fun isUsable(value: Float): Boolean =
        !value.isNaN() && value in 0.0f..1.0f
}
