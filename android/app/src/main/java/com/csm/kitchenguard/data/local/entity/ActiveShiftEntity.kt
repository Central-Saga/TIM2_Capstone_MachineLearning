package com.csm.kitchenguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room untuk metadata shift aktif yang sedang berjalan.
 * PRD Section 10 — Active Shift Context.
 * PRD Section 18 — Offline Local Database (tabel: active_shift).
 *
 * Hanya satu record aktif per sesi. Shift dikontrol server (PostgreSQL/Redis).
 * Client mengikat setiap transaksi waste ke [shiftId] ini.
 */
@Entity(tableName = "active_shift")
data class ActiveShiftEntity(

    /**
     * ID shift dari server. Digunakan sebagai PrimaryKey —
     * hanya ada satu shift aktif, data lama diganti via upsert di DAO.
     */
    @PrimaryKey
    @ColumnInfo(name = "shift_id")
    val shiftId: Long,

    /** ID stasiun dapur yang menjalankan shift ini. */
    @ColumnInfo(name = "station_id")
    val stationId: Long,

    /** Nama tampilan stasiun (contoh: "Meat Station", "Bar & Citrus"). */
    @ColumnInfo(name = "station_name")
    val stationName: String,

    /** Timestamp mulai shift dalam format ISO-8601 (contoh: "2026-09-16T08:00:00Z"). */
    @ColumnInfo(name = "started_at")
    val startedAt: String,

    /**
     * Versi shift dari server, digunakan untuk optimistic concurrency check
     * saat sync ke backend (PostgreSQL).
     */
    @ColumnInfo(name = "server_version")
    val serverVersion: Int,

    /**
     * Menandakan apakah shift telah dikunci oleh server.
     * Jika true, transaksi offline baru yang dikirim akan menghasilkan
     * status CONFLICT (HTTP 409 — SYNC_CONFLICT_SHIFT_LOCKED).
     */
    @ColumnInfo(name = "is_locked")
    val isLocked: Boolean = false
)
