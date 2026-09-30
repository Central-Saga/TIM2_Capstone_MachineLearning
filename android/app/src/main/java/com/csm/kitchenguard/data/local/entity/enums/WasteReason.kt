package com.csm.kitchenguard.data.local.entity.enums

/**
 * Alasan pencatatan waste bahan makanan.
 * PRD Section 12 — Waste Logging Flow.
 *
 * Disimpan sebagai String di Room DB.
 */
enum class WasteReason(val value: String) {
    /** Bahan membusuk / spoiled. */
    SPOILED("SPOILED"),

    /** Melewati tanggal kedaluwarsa. */
    EXPIRED("EXPIRED"),

    /** Kelebihan produksi. */
    OVERPRODUCTION("OVERPRODUCTION"),

    /** Bahan terjatuh / rusak fisik. */
    DROPPED("DROPPED"),

    /** Terkontaminasi. */
    CONTAMINATED("CONTAMINATED"),

    /** Sisa trimming / pemotongan bahan. */
    TRIMMING("TRIMMING"),

    /** Alasan lain yang tidak tercakup. */
    OTHER("OTHER");

    companion object {
        fun fromValue(value: String): WasteReason =
            entries.firstOrNull { it.value == value } ?: OTHER
    }
}
