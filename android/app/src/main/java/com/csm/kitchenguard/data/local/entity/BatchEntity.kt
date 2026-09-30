package com.csm.kitchenguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room untuk cache daftar batch aktif dan tanggal kedaluwarsa bahan.
 * PRD Section 14 — Batch & Expiry Context.
 * PRD Section 18 — Offline Local Database (tabel: batches).
 *
 * Data di-pull dari server. Nilai [remainingQuantity] bersifat read-only cache
 * dari PostgreSQL — kalkulasi stok aktual dilakukan di server.
 *
 * Foreign key ke [IngredientEntity] via [ingredientId] TIDAK di-enforce
 * oleh Room (offline-first: urutan insert bisa tidak berurutan).
 */
@Entity(tableName = "batches")
data class BatchEntity(

    /** ID batch dari server PostgreSQL. */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,

    /** ID bahan yang terkait dengan batch ini. */
    @ColumnInfo(name = "ingredient_id")
    val ingredientId: Long,

    /** Kode batch unik (contoh: "BATCH-2026-09-001"). */
    @ColumnInfo(name = "batch_code")
    val batchCode: String,

    /**
     * Tanggal kedaluwarsa batch dalam format ISO-8601
     * (contoh: "2026-09-20T00:00:00Z").
     */
    @ColumnInfo(name = "expiry_at")
    val expiryAt: String,

    /**
     * Sisa kuantitas batch (read-only cache dari PostgreSQL).
     * Unit mengikuti [IngredientEntity.canonicalUnit].
     */
    @ColumnInfo(name = "remaining_quantity")
    val remainingQuantity: Double,

    /**
     * Status batch. Nilai dari [com.csm.kitchenguard.data.local.entity.enums.BatchStatus].
     * Disimpan sebagai String: "ACTIVE", "NEAR_EXPIRY", "EXPIRED", "DEPLETED".
     */
    @ColumnInfo(name = "status")
    val status: String
)
