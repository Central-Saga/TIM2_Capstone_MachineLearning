package com.csm.kitchenguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room untuk antrean sinkronisasi transaksi ke server.
 * PRD Section 19 — Sync State & Architecture.
 * PRD Section 20 — Idempotency & Peran Redis.
 * PRD Section 21 — Push Sync Batch: POST /api/v1/sync/batch.
 * PRD Section 18 — Offline Local Database (tabel: sync_queue).
 *
 * WorkManager membaca tabel ini saat perangkat terhubung ke jaringan,
 * mengambil record berstatus PENDING, dan mengirimkannya ke server.
 * Retry menggunakan [idempotencyKey] yang sama — Redis di server mencegah duplikasi.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(

    /**
     * Auto-generated primary key untuk antrean internal.
     * Berbeda dari [clientUuid] — satu clientUuid dapat memiliki
     * satu entry di sync_queue.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /**
     * UUID v4 yang sama dengan [WasteRecordEntity.clientUuid] atau
     * ID draft audit. Referensi ke entitas sumber.
     */
    @ColumnInfo(name = "client_uuid")
    val clientUuid: String,

    /**
     * Idempotency key yang akan dikirim ke server.
     * Format waste: "waste:<clientUuid>".
     * Tidak boleh berubah saat retry — Redis memvalidasi key ini.
     */
    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,

    /**
     * Tipe entity yang disinkronisasi.
     * Nilai: "WASTE" atau "AUDIT".
     */
    @ColumnInfo(name = "entity_type")
    val entityType: String,

    /**
     * Jenis operasi yang akan dilakukan di server.
     * Nilai: "CREATE" (saat ini hanya CREATE yang didukung di MVP).
     */
    @ColumnInfo(name = "operation")
    val operation: String,

    /**
     * JSON payload yang akan dikirim ke endpoint POST /api/v1/sync/batch.
     * Di-serialize dari WasteRecordEntity atau StockAuditDraftEntity.
     * PRD Section 12: payload minimum waste record.
     */
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,

    /**
     * Timestamp event asli di sisi client dalam format ISO-8601
     * (mis. "2026-09-16T13:30:00Z").
     *
     * Disalin dari [WasteRecordEntity.clientEventAt] saat enqueue agar nilai
     * asli tidak hilang dan tidak tergantikan oleh waktu server/placeholder.
     * PRD Section 10: device clock tidak boleh menggeser shift secara otomatis,
     * namun timestamp event tetap harus akurat untuk audit trail.
     *
     * Nullable untuk kompatibilitas data lama sebelum migrasi v2.
     */
    @ColumnInfo(name = "client_event_at")
    val clientEventAt: String? = null,

    /** Timestamp (epoch millis) saat entry ini pertama kali dibuat. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /**
     * Jumlah percobaan retry yang sudah dilakukan.
     * WorkManager menggunakan nilai ini untuk menerapkan exponential backoff.
     * PRD Section 19: FAILED → dijadwalkan ulang otomatis.
     */
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    /**
     * Status sinkronisasi saat ini. Nilai dari [com.csm.kitchenguard.data.local.entity.enums.SyncStatus].
     * Disimpan sebagai String: "PENDING", "SYNCING", "SYNCED", "FAILED", "CONFLICT".
     */
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING",

    /**
     * Pesan error terakhir dari server atau network (nullable).
     * Diisi saat [syncStatus] = "FAILED" atau "CONFLICT".
     * Contoh: "SYNC_CONFLICT_SHIFT_LOCKED", "FAILED_VALIDATION".
     */
    @ColumnInfo(name = "error_message")
    val errorMessage: String?
)
