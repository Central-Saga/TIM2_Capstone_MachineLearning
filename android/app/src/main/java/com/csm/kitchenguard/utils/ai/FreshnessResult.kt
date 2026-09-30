package com.csm.kitchenguard.utils.ai

/**
 * Data class yang menampung hasil klasifikasi TFLite untuk modul Freshness AI.
 *
 * PRD Section 16 — On-Device AI Freshness Classification.
 *
 * Catatan integritas (Issue #7):
 * Hasil klasifikasi HANYA boleh berasal dari inferensi model `.tflite` yang benar.
 * Tidak ada fallback angka acak. Jika model gagal dimuat, [isModelAvailable] = false
 * dan [predictedClass] berisi [FreshnessClass.UNCERTAIN] secara jujur — bukan tebakan.
 */
data class FreshnessResult(
    /** Nama kelas hasil prediksi (FRESH/ACCEPTABLE/SPOILED/REJECT/UNCERTAIN). */
    val predictedClass: String,

    /** Skor keyakinan model (0.0 - 1.0). 0.0 bila model tidak tersedia. */
    val confidenceScore: Double,

    /** Versi model yang menjalankan inferensi. */
    val modelVersion: String,

    /** Durasi inferensi dalam milidetik. */
    val inferenceTimeMs: Long,

    /**
     * Apakah model TFLite benar-benar dimuat dan menjalankan inferensi.
     * false = model tidak tersedia / gagal dimuat; UI wajib menampilkan
     * pesan jujur dan meminta staff melakukan observasi manual.
     */
    val isModelAvailable: Boolean = true,

    /** Pesan error teknis bila [isModelAvailable] = false. Null bila sukses. */
    val errorMessage: String? = null
) {
    companion object {
        /**
         * Membuat hasil "model tidak tersedia" yang jujur.
         * Dipakai ketika file model gagal dimuat — TIDAK menebak kelas apa pun.
         *
         * @param modelVersion versi model yang diharapkan
         * @param reason       alasan teknis kegagalan pemuatan model
         */
        fun modelUnavailable(
            modelVersion: String,
            reason: String,
            inferenceTimeMs: Long = 0L
        ): FreshnessResult = FreshnessResult(
            predictedClass = com.csm.kitchenguard.data.local.entity.enums
                .FreshnessClass.UNCERTAIN.name,
            confidenceScore = 0.0,
            modelVersion = modelVersion,
            inferenceTimeMs = inferenceTimeMs,
            isModelAvailable = false,
            errorMessage = reason
        )
    }
}
