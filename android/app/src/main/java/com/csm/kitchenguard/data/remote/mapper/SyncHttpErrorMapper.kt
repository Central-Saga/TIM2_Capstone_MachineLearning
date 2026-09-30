package com.csm.kitchenguard.data.remote.mapper

/**
 * Klasifikasi penanganan error sinkronisasi.
 *
 * PRD Section 26 — Error Handling & HTTP Mapping.
 */
enum class SyncFailureDisposition {
    /**
     * Kesalahan sementara (jaringan / server 5xx). Item dipertahankan di antrean
     * dan akan dicoba ulang otomatis.
     */
    RETRYABLE,

    /**
     * Kesalahan permanen untuk payload ini (422 validasi, 401/403 otorisasi).
     * Retry otomatis tidak akan pernah berhasil — tandai terminal, jangan di-retry.
     */
    TERMINAL,

    /**
     * Konflik state (409 shift lock / version mismatch). Ditandai CONFLICT,
     * menunggu review manual; bukan retry otomatis.
     */
    CONFLICT
}

/**
 * Memetakan kode HTTP dari `pushBatchSync` menjadi [SyncFailureDisposition].
 *
 * Dipisahkan sebagai logika murni agar dapat diuji tanpa network runtime.
 */
object SyncHttpErrorMapper {

    /** Kode HTTP 401 Unauthorized — token kedaluwarsa (butuh aksi login/refresh). */
    const val HTTP_UNAUTHORIZED = 401

    /** Kode HTTP 403 Forbidden — tidak punya hak akses stasiun. */
    const val HTTP_FORBIDDEN = 403

    /** Kode HTTP 409 Conflict — shift lock / version mismatch. */
    const val HTTP_CONFLICT = 409

    /** Kode HTTP 422 Unprocessable Entity — kesalahan validasi payload. */
    const val HTTP_UNPROCESSABLE = 422

    /**
     * Menentukan disposisi berdasarkan kode HTTP.
     *
     * Pemetaan (PRD Section 26):
     * - `409` → [SyncFailureDisposition.CONFLICT]
     * - `422` → [SyncFailureDisposition.TERMINAL]
     * - `401`, `403` → [SyncFailureDisposition.TERMINAL] (butuh aksi user, bukan retry)
     * - `500 / 503` dan error server lainnya (>= 500) → [SyncFailureDisposition.RETRYABLE]
     * - kode lain di luar 2xx (4xx tak dikenal) → [SyncFailureDisposition.TERMINAL]
     *   (retry payload yang sama kemungkinan besar tetap gagal)
     */
    fun classify(httpCode: Int): SyncFailureDisposition = when (httpCode) {
        HTTP_CONFLICT -> SyncFailureDisposition.CONFLICT
        HTTP_UNPROCESSABLE -> SyncFailureDisposition.TERMINAL
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> SyncFailureDisposition.TERMINAL
        in 500..599 -> SyncFailureDisposition.RETRYABLE
        else -> SyncFailureDisposition.TERMINAL
    }
}
