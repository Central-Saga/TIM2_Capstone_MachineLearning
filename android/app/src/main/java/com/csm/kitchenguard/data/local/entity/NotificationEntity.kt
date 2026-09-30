package com.csm.kitchenguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room untuk cache riwayat notifikasi lokal.
 * PRD Section 18 — Offline Local Database (tabel: notifications_cache).
 * PRD Section P1 — Notification Center (fitur P1, entity disiapkan di MVP).
 *
 * Notifikasi di-pull dari server via POST /api/v1/sync/pull.
 * Status [isRead] dikelola di sisi client dan disinkronisasi saat online.
 */
@Entity(tableName = "notifications_cache")
data class NotificationEntity(

    /** ID notifikasi dari server PostgreSQL. */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,

    /**
     * Tipe notifikasi untuk routing UI.
     * Contoh: "SHIFT_LOCKED", "SYNC_CONFLICT", "LOW_STOCK", "AUDIT_REMINDER".
     */
    @ColumnInfo(name = "type")
    val type: String,

    /** Judul notifikasi untuk ditampilkan di Notification Center. */
    @ColumnInfo(name = "title")
    val title: String,

    /** Isi pesan notifikasi. */
    @ColumnInfo(name = "message")
    val message: String,

    /**
     * ID referensi ke entitas terkait (nullable).
     * Contoh: shift_id, waste_record_uuid, atau batch_id.
     * Digunakan untuk deep-link ke layar yang relevan.
     */
    @ColumnInfo(name = "reference_id")
    val referenceId: String?,

    /**
     * Status baca notifikasi.
     * False = belum dibaca (ditampilkan sebagai unread di UI).
     * True  = sudah dibaca.
     */
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,

    /**
     * Timestamp notifikasi dibuat di server, dalam format ISO-8601.
     * Contoh: "2026-09-16T14:30:00Z".
     */
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
