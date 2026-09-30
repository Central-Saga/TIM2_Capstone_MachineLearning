package com.csm.kitchenguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room untuk pencatatan waste bahan makanan secara lokal (offline-first).
 * PRD Section 12 — Waste Logging Flow.
 * PRD Section 18 — Offline Local Database (tabel: waste_records).
 * PRD Section 20 — Idempotency: [clientUuid] adalah UUID v4 yang di-generate client.
 *
 * Primary Key adalah [clientUuid] (bukan auto-increment Long) karena
 * [idempotencyKey] = "waste:<clientUuid>" harus persisten untuk retry.
 *
 * Semua field AI/OCR bersifat nullable — tidak semua waste record
 * menggunakan AI scan atau OCR (tergantung SOP stasiun).
 */
@Entity(tableName = "waste_records")
data class WasteRecordEntity(

    /**
     * UUID v4 yang di-generate oleh client pada saat transaksi dibuat.
     * Digunakan sebagai basis [idempotencyKey].
     * PRD: "client_uuid": "c3a1b8e2-9f44-4e2b-b93d-8e42b10a2f91"
     */
    @PrimaryKey
    @ColumnInfo(name = "client_uuid")
    val clientUuid: String,

    /**
     * Idempotency key format: "waste:<clientUuid>".
     * Disimpan permanen — tidak boleh berubah saat retry.
     * PRD Section 20: Redis di server memvalidasi key ini untuk mencegah duplikasi.
     */
    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,

    /** ID shift aktif saat transaksi dibuat. */
    @ColumnInfo(name = "shift_id")
    val shiftId: Long,

    /** ID stasiun tempat waste terjadi. */
    @ColumnInfo(name = "station_id")
    val stationId: Long,

    /** ID bahan makanan yang di-waste. */
    @ColumnInfo(name = "ingredient_id")
    val ingredientId: Long,

    /**
     * Nama bahan di-denormalize di sini untuk tampilan offline
     * tanpa harus JOIN ke tabel ingredients.
     */
    @ColumnInfo(name = "ingredient_name")
    val ingredientName: String,

    /**
     * ID batch yang terkait (nullable — tidak semua bahan memiliki batch aktif).
     * PRD Section 14: wajib diisi jika stok memiliki batch aktif dan
     * reason = EXPIRED.
     */
    @ColumnInfo(name = "batch_id")
    val batchId: Long?,

    /**
     * Jumlah bahan yang di-waste dalam satuan [unit].
     * Konversi ke canonical unit dilakukan di server (PostgreSQL).
     */
    @ColumnInfo(name = "quantity")
    val quantity: Double,

    /**
     * Satuan input. Nilai yang didukung: "g", "kg", "ml", "liter", "pcs".
     * PRD Section 13 — Unit Conversion.
     */
    @ColumnInfo(name = "unit")
    val unit: String,

    /**
     * Alasan waste. Nilai dari [com.csm.kitchenguard.data.local.entity.enums.WasteReason].
     * Disimpan sebagai String: "SPOILED", "EXPIRED", "OVERPRODUCTION", dll.
     */
    @ColumnInfo(name = "reason")
    val reason: String,

    /**
     * Timestamp event di sisi client dalam format ISO-8601.
     * PRD: "client_event_at": "2026-09-16T13:30:00Z".
     * Device clock TIDAK boleh mengubah shift binding secara otomatis.
     */
    @ColumnInfo(name = "client_event_at")
    val clientEventAt: String,

    // ── AI Freshness Metadata (nullable — opsional sesuai SOP) ────────────────

    /**
     * Kelas prediksi AI. Nilai dari [com.csm.kitchenguard.data.local.entity.enums.FreshnessClass].
     * Disimpan sebagai String: "FRESH", "ACCEPTABLE", "SPOILED", "REJECT", "UNCERTAIN".
     * PRD Section 16: Confidence gate 85%.
     */
    @ColumnInfo(name = "ai_class")
    val aiClass: String?,

    /**
     * Confidence score prediksi AI (0.0 – 1.0).
     * Null jika AI scan tidak dilakukan.
     */
    @ColumnInfo(name = "ai_confidence")
    val aiConfidence: Double?,

    /**
     * Versi model TFLite yang digunakan.
     * PRD: "model_version": "freshness-v1.0".
     */
    @ColumnInfo(name = "ai_model_version")
    val aiModelVersion: String?,

    // ── OCR Metadata (nullable — opsional) ───────────────────────────────────

    /**
     * Raw text yang dihasilkan ML Kit OCR dari layar timbangan.
     * PRD Section 15: "ocr_raw_text": "1.450 kg".
     */
    @ColumnInfo(name = "ocr_raw_text")
    val ocrRawText: String?,

    /**
     * Confidence score OCR (0.0 – 1.0).
     * PRD Section 15: >= 0.90 → auto-fill; < 0.90 → perlu verifikasi manual.
     */
    @ColumnInfo(name = "ocr_confidence")
    val ocrConfidence: Double?,

    /** Path file foto evidence bahan (lokal di storage device). */
    @ColumnInfo(name = "photo_path")
    val photoPath: String?,

    // ── Sync State ────────────────────────────────────────────────────────────

    /**
     * Status sinkronisasi ke server. Nilai dari [com.csm.kitchenguard.data.local.entity.enums.SyncStatus].
     * Default: "PENDING" — langsung masuk antrean saat transaksi dibuat.
     * Disimpan sebagai String: "PENDING", "SYNCING", "SYNCED", "FAILED", "CONFLICT".
     */
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING"
)
