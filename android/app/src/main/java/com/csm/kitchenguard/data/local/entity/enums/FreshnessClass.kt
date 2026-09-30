package com.csm.kitchenguard.data.local.entity.enums

/**
 * Kelas hasil klasifikasi kesegaran bahan oleh TFLite on-device.
 * PRD Section 16 — On-Device AI Freshness Classification.
 *
 * Confidence gate:
 *   - >= 85% → tampilkan kelas sebagai decision support
 *   - <  85% → output menjadi UNCERTAIN, staff observasi manual
 *
 * Disimpan sebagai String di Room DB (field `aiClass` di WasteRecordEntity).
 */
enum class FreshnessClass(val value: String) {
    /** Bahan dalam kondisi segar optimal. */
    FRESH("FRESH"),

    /** Bahan masih layak pakai namun perlu diperhatikan. */
    ACCEPTABLE("ACCEPTABLE"),

    /** Bahan mulai membusuk. */
    SPOILED("SPOILED"),

    /** Bahan tidak layak pakai sama sekali. */
    REJECT("REJECT"),

    /** Confidence < 85% — model tidak yakin, staff wajib observasi manual. */
    UNCERTAIN("UNCERTAIN");

    companion object {
        /** Confidence threshold sesuai PRD Section 16. */
        const val CONFIDENCE_THRESHOLD = 0.85

        fun fromValue(value: String): FreshnessClass =
            entries.firstOrNull { it.value == value } ?: UNCERTAIN

        /**
         * Evaluasi output model: jika confidence di bawah threshold,
         * kembalikan UNCERTAIN terlepas dari predicted class.
         */
        fun evaluate(predicted: String, confidence: Double): FreshnessClass {
            return if (confidence >= CONFIDENCE_THRESHOLD) fromValue(predicted)
            else UNCERTAIN
        }
    }
}
