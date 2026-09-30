package com.csm.kitchenguard.data.local.entity.enums

/**
 * Status sinkronisasi transaksi lokal ke server.
 * PRD Section 19 — Sync State & Architecture.
 *
 * Disimpan sebagai String di Room DB.
 */
enum class SyncStatus(val value: String) {
    /** Tersimpan di Room, menunggu jadwal sync WorkManager. */
    PENDING("PENDING"),

    /** Sedang dalam proses HTTP request ke API. */
    SYNCING("SYNCING"),

    /** Dikonfirmasi sukses oleh server (tersimpan di PostgreSQL). */
    SYNCED("SYNCED"),

    /** Gagal jaringan / server 5xx — RETRYABLE, akan dijadwalkan ulang. */
    FAILED("FAILED"),

    /**
     * Payload ditolak validasi server (HTTP 422) — TERMINAL.
     *
     * Tidak akan pernah berhasil bila di-retry dengan payload yang sama, sehingga
     * TIDAK diambil oleh `getNextPendingBatch` dan tidak di-retry otomatis.
     * Menunggu perbaikan payload / review Head Chef.
     * PRD Section 26: `422` = kesalahan validasi payload data.
     */
    FAILED_VALIDATION("FAILED_VALIDATION"),

    /** Ditolak server (misal: shift telah dikunci). Perlu review manual. */
    CONFLICT("CONFLICT");

    companion object {
        fun fromValue(value: String): SyncStatus =
            entries.firstOrNull { it.value == value } ?: PENDING
    }
}
