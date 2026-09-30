package com.csm.kitchenguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room untuk draft penghitungan fisik stok di akhir shift.
 * PRD Section 24 — Physical Stock Audit.
 * PRD Section 18 — Offline Local Database (tabel: draft_stock_audit).
 *
 * Inventory Checker menginput [actualPhysical] per bahan dan batch.
 * Data disimpan sebagai draft hingga dikonfirmasi dan dikirim ke
 * endpoint POST /api/v1/stock-audits.
 *
 * Kalkulasi variance dilakukan di server (PostgreSQL):
 *   Variance = Actual Physical − Expected Closing
 *   Expected Closing = Opening + Received − POS Consumption − Recorded Waste + Adjustments
 */
@Entity(tableName = "draft_stock_audit")
data class StockAuditDraftEntity(

    /** Auto-generated primary key untuk draft lokal. */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** ID shift saat audit fisik dilakukan. */
    @ColumnInfo(name = "shift_id")
    val shiftId: Long,

    /** ID stasiun tempat audit dilakukan. */
    @ColumnInfo(name = "station_id")
    val stationId: Long,

    /** ID bahan yang dihitung secara fisik. */
    @ColumnInfo(name = "ingredient_id")
    val ingredientId: Long,

    /**
     * ID batch yang dihitung (nullable — tidak semua bahan memiliki batch aktif).
     * PRD Section 24: input per bahan dan batch.
     */
    @ColumnInfo(name = "batch_id")
    val batchId: Long?,

    /**
     * Jumlah fisik aktual hasil penghitungan langsung oleh Inventory Checker.
     * Nilai ini yang dikirim ke server sebagai `actual_physical`.
     */
    @ColumnInfo(name = "actual_physical")
    val actualPhysical: Double,

    /**
     * Satuan penghitungan fisik (mengikuti canonical unit bahan).
     * Nilai: "kg", "liter", "pcs", dll.
     */
    @ColumnInfo(name = "unit")
    val unit: String,

    /** Timestamp (epoch millis) saat penghitungan fisik dilakukan. */
    @ColumnInfo(name = "counted_at")
    val countedAt: Long,

    /**
     * Menandakan apakah draft ini sudah dikirim ke server.
     * False = masih draft lokal.
     * True  = sudah dikirim ke POST /api/v1/stock-audits.
     */
    @ColumnInfo(name = "is_submitted")
    val isSubmitted: Boolean = false
)
