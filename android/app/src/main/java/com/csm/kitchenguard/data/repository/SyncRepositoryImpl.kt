package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.data.local.dao.NotificationDao
import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.dao.WasteDao
import com.csm.kitchenguard.data.local.entity.ActiveShiftEntity
import com.csm.kitchenguard.data.local.entity.BatchEntity
import com.csm.kitchenguard.data.local.entity.IngredientEntity
import com.csm.kitchenguard.data.local.entity.NotificationEntity
import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import com.csm.kitchenguard.data.local.entity.enums.SyncStatus
import com.csm.kitchenguard.data.remote.api.KitchenGuardApiService
import com.csm.kitchenguard.data.remote.dto.SyncBatchRequest
import com.csm.kitchenguard.data.remote.dto.SyncPullRequest
import com.csm.kitchenguard.data.remote.mapper.NotificationMapper
import com.csm.kitchenguard.data.remote.mapper.SyncFailureDisposition
import com.csm.kitchenguard.data.remote.mapper.SyncHttpErrorMapper
import com.csm.kitchenguard.data.remote.mapper.SyncItemMapper
import com.csm.kitchenguard.domain.repository.SyncRepository
import com.csm.kitchenguard.domain.repository.SyncSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SyncRepositoryImpl(
    private val apiService: KitchenGuardApiService,
    private val syncQueueDao: SyncQueueDao,
    private val wasteDao: WasteDao,
    private val masterDataDao: MasterDataDao,
    private val shiftDao: ShiftDao,
    private val notificationDao: NotificationDao
) : SyncRepository {

    override suspend fun enqueueSyncItem(
        clientUuid: String,
        idempotencyKey: String,
        entityType: String,
        operation: String,
        payloadJson: String,
        clientEventAt: String?
    ): Result<Long> {
        return try {
            // Tulis item ke antrean agar benar-benar dikirim pada push batch
            // berikutnya (Issue #15 — sebelumnya fungsi ini no-op / kosong).
            val rowId = syncQueueDao.enqueue(
                SyncQueueEntity(
                    clientUuid = clientUuid,
                    idempotencyKey = idempotencyKey,
                    entityType = entityType,
                    operation = operation,
                    payloadJson = payloadJson,
                    // Salin waktu event asli; null diperbolehkan dan akan
                    // di-fallback ke waktu enqueue oleh SyncItemMapper.
                    clientEventAt = clientEventAt,
                    createdAt = System.currentTimeMillis(),
                    errorMessage = null
                )
            )
            Result.success(rowId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushBatchSync(): Result<SyncSummary> {
        return try {
            // 1. Ambil batch dari queue lokal (max 20).
            //    CATATAN: hanya PENDING dan FAILED (retryable) yang diambil.
            //    FAILED_VALIDATION dan CONFLICT TIDAK diambil (terminal).
            val pendingItems = syncQueueDao.getNextPendingBatch(20)
            if (pendingItems.isEmpty()) {
                return Result.success(SyncSummary(0, 0, 0, 0)) // Tidak ada yang perlu dikirim
            }

            // 2. Transformasi ke DTO Network.
            //    clientEventAt diambil dari waktu event ASLI yang tersimpan di
            //    antrean (SyncQueueEntity.clientEventAt) — bukan placeholder.
            //    Lihat [SyncItemMapper] untuk aturan resolusi & fallback.
            val requestItems = pendingItems.map { SyncItemMapper.toDto(it) }

            // 3. Panggil API Push Batch
            val response = apiService.pushSyncBatch(SyncBatchRequest(requestItems))

            // ── Error di level HTTP (seluruh batch) ──────────────────────────
            // PRD Section 26: bedakan error retryable (5xx) dari terminal
            // (422/401/403) dan konflik (409) berdasarkan kode HTTP.
            if (!response.isSuccessful || response.body() == null) {
                return handleHttpError(response.code(), pendingItems)
            }

            val results = response.body()!!.results

            var successCount = 0
            var conflictCount = 0
            var failedCount = 0
            var validationFailedCount = 0

            // 4. Update status lokal berdasarkan hasil tiap item
            for (res in results) {
                val dbItem = pendingItems.find { it.clientUuid == res.clientUuid } ?: continue

                when (res.status) {
                    "SYNCED", "ALREADY_PROCESSED" -> {
                        syncQueueDao.deleteSynced(dbItem.id)
                        wasteDao.updateSyncStatus(res.clientUuid, SyncStatus.SYNCED.value)
                        successCount++
                    }
                    "CONFLICT" -> {
                        // Konflik: menunggu review manual, bukan retry otomatis.
                        syncQueueDao.updateStatus(
                            dbItem.id, SyncStatus.CONFLICT.value, res.message, dbItem.retryCount + 1
                        )
                        wasteDao.updateSyncStatus(res.clientUuid, SyncStatus.CONFLICT.value)
                        conflictCount++
                    }
                    "FAILED_VALIDATION" -> {
                        // TERMINAL: payload ditolak validasi (422). Tidak akan
                        // pernah berhasil bila di-retry dengan payload sama.
                        // Tandai FAILED_VALIDATION agar TIDAK diambil batch
                        // berikutnya (Issue #12).
                        syncQueueDao.updateStatus(
                            dbItem.id,
                            SyncStatus.FAILED_VALIDATION.value,
                            res.message,
                            dbItem.retryCount // tidak di-increment: bukan untuk di-retry
                        )
                        wasteDao.updateSyncStatus(res.clientUuid, SyncStatus.FAILED_VALIDATION.value)
                        validationFailedCount++
                    }
                    else -> {
                        // Status tak dikenal: perlakukan sebagai kegagalan validasi
                        // (lebih aman tidak me-retry selamanya).
                        syncQueueDao.updateStatus(
                            dbItem.id,
                            SyncStatus.FAILED_VALIDATION.value,
                            res.message,
                            dbItem.retryCount
                        )
                        wasteDao.updateSyncStatus(res.clientUuid, SyncStatus.FAILED_VALIDATION.value)
                        validationFailedCount++
                    }
                }
            }

            Result.success(
                SyncSummary(
                    totalProcessed = pendingItems.size,
                    successCount = successCount,
                    conflictCount = conflictCount,
                    failedCount = failedCount,
                    validationFailedCount = validationFailedCount,
                    // Item yang tersisa di antrean (CONFLICT/FAILED_VALIDATION)
                    // bukan alasan untuk retry seluruh batch.
                    hasRetryableFailure = false
                )
            )
        } catch (e: Exception) {
            // Kegagalan jaringan/IO → retryable (WorkManager akan menjadwalkan ulang).
            Result.failure(e)
        }
    }

    /**
     * Menangani error HTTP yang memengaruhi SELURUH batch, mengikuti
     * PRD Section 26.
     *
     * - 409 CONFLICT → semua item ditandai CONFLICT (menunggu review manual).
     * - 422 / 401 / 403 (TERMINAL) → semua item ditandai FAILED_VALIDATION agar
     *   tidak di-retry otomatis tanpa batas.
     * - 5xx (RETRYABLE) → kembalikan `Result.failure` supaya WorkManager retry;
     *   status item dibiarkan PENDING/FAILED agar tetap diambil batch berikutnya.
     */
    private suspend fun handleHttpError(
        code: Int,
        pendingItems: List<SyncQueueEntity>
    ): Result<SyncSummary> = when (SyncHttpErrorMapper.classify(code)) {
        SyncFailureDisposition.CONFLICT -> {
            val conflictMsg =
                "Shift aktif telah dikunci di server. Transaksi memerlukan review Head Chef."
            for (item in pendingItems) {
                syncQueueDao.updateStatus(item.id, SyncStatus.CONFLICT.value, conflictMsg, item.retryCount)
                wasteDao.updateSyncStatus(item.clientUuid, SyncStatus.CONFLICT.value)
            }
            // Return success (bukan failure/retry) agar WorkManager tidak infinite-loop.
            Result.success(
                SyncSummary(pendingItems.size, 0, pendingItems.size, 0)
            )
        }

        SyncFailureDisposition.TERMINAL -> {
            val msg = "Payload ditolak server (HTTP $code). Perlu review/perbaikan data."
            for (item in pendingItems) {
                syncQueueDao.updateStatus(
                    item.id, SyncStatus.FAILED_VALIDATION.value, msg, item.retryCount
                )
                wasteDao.updateSyncStatus(item.clientUuid, SyncStatus.FAILED_VALIDATION.value)
            }
            // Terminal → sukses dari sisi "sudah ditangani", agar tidak di-retry terus.
            Result.success(
                SyncSummary(
                    totalProcessed = pendingItems.size,
                    successCount = 0,
                    conflictCount = 0,
                    failedCount = 0,
                    validationFailedCount = pendingItems.size,
                    hasRetryableFailure = false
                )
            )
        }

        SyncFailureDisposition.RETRYABLE -> {
            // Pertahankan item di antrean; sinyal retry ke WorkManager.
            Result.failure(Exception("HTTP Error (retryable): $code"))
        }
    }

    /**
     * Mengembalikan item CONFLICT → PENDING di DUA tempat sekaligus (Issue #11).
     *
     * ── Mengapa harus dua tabel? ─────────────────────────────────────────────
     * - `sync_queue` adalah sumber yang dibaca [SyncRepositoryImpl.pushBatchSync]
     *   untuk membentuk batch. Bila tabel ini tetap CONFLICT, item tidak akan
     *   pernah dikirim ulang walau WorkManager dijalankan.
     * - `waste_records` adalah status yang dilihat UI/konsumen; harus konsisten.
     *
     * Urutan: reset antrean lebih dulu (agar bisa dikirim), lalu tabel sumber.
     * Keduanya idempotent — aman bila dipanggil berkali-kali.
     */
    override suspend fun retryConflictedItems(): Result<Int> {
        return try {
            val queueResetCount = syncQueueDao.resetConflictToPending()
            wasteDao.resetConflictToPending()
            Result.success(queueResetCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getNotifications(): Flow<List<NotificationEntity>> =
        notificationDao.getAllNotifications()

    override fun getUnreadNotificationCount(): Flow<Int> =
        notificationDao.getUnreadNotifications().map { it.size }

    override suspend fun markNotificationAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    override suspend fun pullMasterData(): Result<Unit> {
        return try {
            val response = apiService.pullSync(SyncPullRequest(lastSyncCursor = null))
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                // Mapper sederhana DTO ke Entity untuk Ingredients
                val ingredients = body.ingredients.map {
                    IngredientEntity(it.id, it.name, it.category, it.canonicalUnit, it.minimumStock)
                }
                masterDataDao.insertIngredients(ingredients)

                // Mapper sederhana DTO ke Entity untuk Batches
                val batches = body.batches.map {
                    BatchEntity(it.id, it.ingredientId, it.batchCode, it.expiryAt, it.remainingQuantity, it.status)
                }
                masterDataDao.insertBatches(batches)
                
                // Update active shift context
                if (body.activeShift != null) {
                    val s = body.activeShift
                    shiftDao.insertOrUpdateActiveShift(
                        ActiveShiftEntity(s.id, s.stationId, s.stationName, s.startedAt, s.serverVersion, s.isLocked)
                    )
                } else {
                    shiftDao.clearActiveShift() // Jika server mengembalikan null, berarti shift ditutup
                }

                // Mapper notifikasi server → cache lokal (Issue #14).
                // Hanya diproses bila field ada (nullable); bila null, cache lama
                // dibiarkan utuh alih-alih dikosongkan.
                body.notifications?.let { notifications ->
                    notificationDao.insertNotifications(
                        NotificationMapper.toEntityList(notifications)
                    )
                }

                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
