package com.csm.kitchenguard.domain.repository

import com.csm.kitchenguard.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Ringkasan hasil dari proses push batch sync.
 */
data class SyncSummary(
    val totalProcessed: Int,
    val successCount: Int,
    val conflictCount: Int,
    val failedCount: Int,
    /**
     * Jumlah item yang ditolak validasi server (HTTP 422 / FAILED_VALIDATION).
     * Item ini bersifat TERMINAL — tidak akan di-retry otomatis (Issue #12).
     */
    val validationFailedCount: Int = 0,
    /**
     * True bila batch ini mengandung kegagalan yang masih layak di-retry
     * (mis. 5xx / jaringan). WorkManager sebaiknya menjadwalkan ulang hanya
     * bila nilai ini true.
     */
    val hasRetryableFailure: Boolean = false
)

/**
 * Kontrak domain untuk sinkronisasi data master (Pull) dan transaksi (Push).
 * PRD Section 19-22.
 */
interface SyncRepository {

    /**
     * Memasukkan item ke dalam antrean sinkronisasi lokal.
     * PRD Section 20 — Idempotency Key Setup.
     *
     * Digunakan untuk entitas non-waste (mis. AUDIT). Entitas waste memakai
     * jalur [com.csm.kitchenguard.domain.repository.WasteRepository.saveWasteRecord],
     * namun tetap dapat memakai fungsi ini bila diperlukan.
     *
     * @param clientUuid    UUID v4 entitas sumber (idempotent enqueue).
     * @param idempotencyKey kunci idempotency final (mis. "waste:<uuid>") — dikirim apa adanya.
     * @param entityType    tipe entitas, mis. "WASTE_RECORD" / "AUDIT".
     * @param operation     operasi, mis. "CREATE".
     * @param payloadJson   JSON payload yang akan dikirim ke server.
     * @param clientEventAt timestamp event asli ISO-8601. KRUSIAL: harus berasal dari
     *                      waktu event nyata, bukan placeholder konstan (lihat Issue #9).
     *                      Bila null/kosong, mapper akan fallback ke waktu enqueue.
     * @return [Result] berisi rowId antrean. `-1` berarti item dengan clientUuid
     *         yang sama sudah ada (enqueue di-IGNORE / idempotent).
     */
    suspend fun enqueueSyncItem(
        clientUuid: String,
        idempotencyKey: String,
        entityType: String,
        operation: String,
        payloadJson: String,
        clientEventAt: String?
    ): Result<Long>

    /**
     * Menarik item dari antrean lokal (status PENDING/FAILED) dan mengirimkannya
     * ke API POST /api/v1/sync/batch. Update database lokal sesuai respon server.
     * Biasa dipanggil oleh WorkManager secara periodik.
     */
    suspend fun pushBatchSync(): Result<SyncSummary>

    /**
     * Mengambil master data (ingredients, batches, shift aktif) dari backend
     * dan memperbarui cache Room secara lokal.
     * Biasa dipanggil saat Splash Screen atau tombol Sync manual ditekan.
     */
    suspend fun pullMasterData(): Result<Unit>

    /**
     * Mengembalikan seluruh item berstatus CONFLICT ke PENDING agar dapat dikirim
     * ulang pada batch sinkronisasi berikutnya.
     *
     * WAJIB mereset DUA tempat sekaligus (Issue #11):
     * 1. Tabel antrean `sync_queue` — agar item benar-benar diambil oleh
     *    `getNextPendingBatch` (yang hanya membaca PENDING/FAILED).
     * 2. Tabel sumber `waste_records` — agar status di UI/konsumen konsisten.
     *
     * Mereset salah satu saja membuat tombol "Kirim Ulang Semua Konflik" tidak
     * berefek.
     *
     * @return jumlah item antrean yang di-reset.
     */
    suspend fun retryConflictedItems(): Result<Int>

    /**
     * Stream notifikasi ter-cache (hasil pull sync) untuk Notification Center.
     * PRD Section P1 — Notification Center.
     *
     * Issue #14: cache diisi oleh [pullMasterData]; query ini membacanya untuk UI.
     */
    fun getNotifications(): Flow<List<NotificationEntity>>

    /** Stream jumlah notifikasi belum dibaca (badge). */
    fun getUnreadNotificationCount(): Flow<Int>

    /** Tandai satu notifikasi sebagai sudah dibaca. */
    suspend fun markNotificationAsRead(id: Long)
}
