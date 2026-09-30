package com.csm.kitchenguard.data.local.entity.enums

/**
 * Status batch bahan makanan.
 * PRD Section 14 — Batch & Expiry Context.
 *
 * Disimpan sebagai String di Room DB.
 */
enum class BatchStatus(val value: String) {
    /** Batch aktif, stok tersedia, belum mendekati kedaluwarsa. */
    ACTIVE("ACTIVE"),

    /** Mendekati tanggal kedaluwarsa (threshold ditentukan server). */
    NEAR_EXPIRY("NEAR_EXPIRY"),

    /** Melewati tanggal kedaluwarsa. */
    EXPIRED("EXPIRED"),

    /** Stok batch telah habis / depleted. */
    DEPLETED("DEPLETED");

    companion object {
        fun fromValue(value: String): BatchStatus =
            entries.firstOrNull { it.value == value } ?: ACTIVE
    }
}
