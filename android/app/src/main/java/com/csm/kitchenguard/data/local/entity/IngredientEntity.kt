package com.csm.kitchenguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room untuk cache master data bahan makanan dari server.
 * PRD Section 18 — Offline Local Database (tabel: ingredients).
 *
 * Data ini di-pull dari server via POST /api/v1/sync/pull dan diperbarui
 * secara berkala oleh WorkManager. Bersifat read-only di client.
 */
@Entity(tableName = "ingredients")
data class IngredientEntity(

    /** ID bahan dari server PostgreSQL. */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,

    /** Nama bahan makanan (contoh: "Wagyu Ribeye MB7", "Salmon Fillet"). */
    @ColumnInfo(name = "name")
    val name: String,

    /** Kategori bahan (contoh: "Meat & Butcher", "Produce", "Dairy & Chilled"). */
    @ColumnInfo(name = "category")
    val category: String,

    /**
     * Satuan dasar kanonik yang digunakan server sebagai basis kalkulasi.
     * PRD Section 13 — Unit Conversion.
     * Contoh: "kg", "liter", "pcs".
     * Konversi unit hanya untuk display di client; kalkulasi loss di server.
     */
    @ColumnInfo(name = "canonical_unit")
    val canonicalUnit: String,

    /**
     * Batas stok minimum. Digunakan untuk alert visual di UI.
     * Nilai finansial tetap dikalkulasi di server (PostgreSQL).
     */
    @ColumnInfo(name = "minimum_stock")
    val minimumStock: Double
)
