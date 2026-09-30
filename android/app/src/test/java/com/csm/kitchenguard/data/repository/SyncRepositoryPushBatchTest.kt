package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.data.local.dao.NotificationDao
import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.dao.WasteDao
import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import com.csm.kitchenguard.data.local.entity.enums.SyncStatus
import com.csm.kitchenguard.data.remote.api.KitchenGuardApiService
import com.csm.kitchenguard.data.remote.dto.SyncBatchRequest
import com.csm.kitchenguard.data.remote.dto.SyncBatchResponse
import com.csm.kitchenguard.data.remote.dto.SyncItemResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Unit test untuk [SyncRepositoryImpl.pushBatchSync] — verifikasi idempotency key
 * dan client_event_at diambil dari nilai TERSIMPAN di antrean, BUKAN di-rekonstruksi
 * saat push (Issue #9 & #10).
 *
 * Juga memverifikasi bahwa FAILED_VALIDATION tidak masuk ke antrean berikutnya
 * (Issue #12), dan SYNCED dihapus dari antrean (Issue #15 — idempotency).
 *
 * Issue #28 — DoD #3: test sync, idempotency, audit.
 */
class SyncRepositoryPushBatchTest {

    private val apiService: KitchenGuardApiService = mockk()
    private val syncQueueDao: SyncQueueDao = mockk(relaxed = true)
    private val wasteDao: WasteDao = mockk(relaxed = true)
    private val masterDataDao: MasterDataDao = mockk(relaxed = true)
    private val shiftDao: ShiftDao = mockk(relaxed = true)
    private val notificationDao: NotificationDao = mockk(relaxed = true)

    private val repo = SyncRepositoryImpl(
        apiService, syncQueueDao, wasteDao, masterDataDao, shiftDao, notificationDao
    )

    private fun makeQueuedItem(
        id: Long = 1L,
        clientUuid: String = "uuid-001",
        idempotencyKey: String = "stored-idem-key-001",
        clientEventAt: String? = "2026-09-01T08:00:00Z"
    ) = SyncQueueEntity(
        id = id,
        clientUuid = clientUuid,
        idempotencyKey = idempotencyKey,
        entityType = "waste_record",
        operation = "CREATE",
        payloadJson = "{}",
        clientEventAt = clientEventAt,
        createdAt = System.currentTimeMillis()
    )

    // ── Issue #10: idempotency key dari DB, bukan rekonstruksi ───────────────

    @Test
    fun `pushBatchSync sends stored idempotency key, not a reconstructed one`() = runTest {
        val stored = makeQueuedItem(idempotencyKey = "STORED-KEY-XYZ")
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(stored)

        val capturedRequest = slot<SyncBatchRequest>()
        val successResponse = Response.success(
            SyncBatchResponse(results = listOf(SyncItemResult(stored.clientUuid, "SYNCED", null)))
        )
        coEvery { apiService.pushSyncBatch(capture(capturedRequest)) } returns successResponse

        repo.pushBatchSync()

        val sentItem = capturedRequest.captured.items.first()
        assertEquals(
            "Idempotency key harus berasal dari nilai tersimpan di DB (Issue #10)",
            "STORED-KEY-XYZ",
            sentItem.idempotencyKey
        )
    }

    // ── Issue #9: client_event_at dari DB, bukan hardcode saat push ──────────

    @Test
    fun `pushBatchSync sends stored client_event_at, not a hardcoded placeholder`() = runTest {
        val originalTimestamp = "2026-09-01T06:30:00Z"
        val stored = makeQueuedItem(clientEventAt = originalTimestamp)
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(stored)

        val capturedRequest = slot<SyncBatchRequest>()
        val successResponse = Response.success(
            SyncBatchResponse(results = listOf(SyncItemResult(stored.clientUuid, "SYNCED", null)))
        )
        coEvery { apiService.pushSyncBatch(capture(capturedRequest)) } returns successResponse

        repo.pushBatchSync()

        val sentItem = capturedRequest.captured.items.first()
        assertEquals(
            "client_event_at harus timestamp asli dari waktu transaksi (Issue #9)",
            originalTimestamp,
            sentItem.clientEventAt
        )
    }

    // ── Issue #12: FAILED_VALIDATION ditandai terminal, tidak di-retry ────────

    @Test
    fun `pushBatchSync marks FAILED_VALIDATION as terminal and does not re-enqueue`() = runTest {
        val stored = makeQueuedItem()
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(stored)

        val failedValidationResponse = Response.success(
            SyncBatchResponse(results = listOf(
                SyncItemResult(stored.clientUuid, "FAILED_VALIDATION", "Schema error")
            ))
        )
        coEvery { apiService.pushSyncBatch(any()) } returns failedValidationResponse

        val result = repo.pushBatchSync()

        assertTrue("pushBatchSync harus sukses (bukan failure) untuk FAILED_VALIDATION", result.isSuccess)

        // Verifikasi status di DB ditandai FAILED_VALIDATION (bukan PENDING)
        coVerify {
            syncQueueDao.updateStatus(
                stored.id,
                SyncStatus.FAILED_VALIDATION.value,
                any(),
                stored.retryCount
            )
        }
    }

    // ── Idempotency: SYNCED & ALREADY_PROCESSED dihapus dari antrean ─────────

    @Test
    fun `pushBatchSync removes SYNCED item from queue (idempotency gate)`() = runTest {
        val stored = makeQueuedItem()
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(stored)

        val syncedResponse = Response.success(
            SyncBatchResponse(results = listOf(
                SyncItemResult(stored.clientUuid, "SYNCED", null)
            ))
        )
        coEvery { apiService.pushSyncBatch(any()) } returns syncedResponse

        repo.pushBatchSync()

        coVerify { syncQueueDao.deleteSynced(stored.id) }
        coVerify { wasteDao.updateSyncStatus(stored.clientUuid, SyncStatus.SYNCED.value) }
    }

    @Test
    fun `pushBatchSync treats ALREADY_PROCESSED same as SYNCED (idempotency)`() = runTest {
        val stored = makeQueuedItem()
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(stored)

        val alreadyProcessedResponse = Response.success(
            SyncBatchResponse(results = listOf(
                SyncItemResult(stored.clientUuid, "ALREADY_PROCESSED", null)
            ))
        )
        coEvery { apiService.pushSyncBatch(any()) } returns alreadyProcessedResponse

        repo.pushBatchSync()

        // ALREADY_PROCESSED = item sudah ada di server; hapus dari antrian lokal
        coVerify { syncQueueDao.deleteSynced(stored.id) }
    }

    // ── Empty queue: no-op, returns zero counts ───────────────────────────────

    @Test
    fun `pushBatchSync returns zero summary when queue is empty`() = runTest {
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns emptyList()

        val result = repo.pushBatchSync()

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.totalProcessed)
    }
}
