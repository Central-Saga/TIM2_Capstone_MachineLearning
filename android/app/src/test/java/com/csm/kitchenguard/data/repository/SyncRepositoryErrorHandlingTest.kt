package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.data.local.dao.NotificationDao
import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.dao.WasteDao
import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import com.csm.kitchenguard.data.remote.api.KitchenGuardApiService
import com.csm.kitchenguard.data.remote.dto.SyncBatchRequest
import com.csm.kitchenguard.data.remote.dto.SyncBatchResponse
import com.csm.kitchenguard.data.remote.dto.SyncResultDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

/**
 * Unit test untuk [SyncRepositoryImpl.pushBatchSync] — penanganan error terminal
 * vs retryable (Issue #12, PRD Section 26).
 */
class SyncRepositoryErrorHandlingTest {

    private val apiService: KitchenGuardApiService = mockk()
    private val syncQueueDao: SyncQueueDao = mockk(relaxed = true)
    private val wasteDao: WasteDao = mockk(relaxed = true)
    private val masterDataDao: MasterDataDao = mockk(relaxed = true)
    private val shiftDao: ShiftDao = mockk(relaxed = true)
    private val notificationDao: NotificationDao = mockk(relaxed = true)

    private val repository = SyncRepositoryImpl(
        apiService, syncQueueDao, wasteDao, masterDataDao, shiftDao, notificationDao
    )

    private fun queueItem(uuid: String, retryCount: Int = 0) = SyncQueueEntity(
        id = 1L,
        clientUuid = uuid,
        idempotencyKey = "waste:$uuid",
        entityType = "WASTE_RECORD",
        operation = "CREATE",
        payloadJson = "{}",
        clientEventAt = "2026-09-16T13:30:00Z",
        createdAt = 1_700_000_000_000L,
        retryCount = retryCount,
        errorMessage = null
    )

    private fun okBody(vararg results: SyncResultDto) =
        Response.success(SyncBatchResponse(results.toList()))

    private fun httpError(code: Int): Response<SyncBatchResponse> =
        Response.error(code, "{}".toResponseBody("application/json".toMediaType()))

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Per-item FAILED_VALIDATION bersifat TERMINAL
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN item status FAILED_VALIDATION WHEN push THEN marked terminal not FAILED`() = runTest {
        val item = queueItem("uuid-1")
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(item)
        coEvery { apiService.pushSyncBatch(any()) } returns okBody(
            SyncResultDto("uuid-1", "FAILED_VALIDATION", null, "invalid quantity")
        )

        repository.pushBatchSync()

        val statusSlot = slot<String>()
        coVerify {
            syncQueueDao.updateStatus(item.id, capture(statusSlot), any(), any())
        }
        assertEquals(
            "Item validasi gagal harus ditandai FAILED_VALIDATION (terminal), bukan FAILED",
            "FAILED_VALIDATION",
            statusSlot.captured
        )
    }

    @Test
    fun `GIVEN item status FAILED_VALIDATION WHEN push THEN retryCount NOT incremented`() = runTest {
        val item = queueItem("uuid-1", retryCount = 2)
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(item)
        coEvery { apiService.pushSyncBatch(any()) } returns okBody(
            SyncResultDto("uuid-1", "FAILED_VALIDATION", null, "bad payload")
        )

        repository.pushBatchSync()

        val retrySlot = slot<Int>()
        coVerify {
            syncQueueDao.updateStatus(item.id, any(), any(), capture(retrySlot))
        }
        assertEquals(
            "FAILED_VALIDATION bukan untuk di-retry → retryCount tidak ditambah",
            2,
            retrySlot.captured
        )
    }

    @Test
    fun `GIVEN item FAILED_VALIDATION WHEN push THEN summary reports validationFailed`() = runTest {
        val item = queueItem("uuid-1")
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(item)
        coEvery { apiService.pushSyncBatch(any()) } returns okBody(
            SyncResultDto("uuid-1", "FAILED_VALIDATION", null, "bad")
        )

        val summary = repository.pushBatchSync().getOrThrow()

        assertEquals(1, summary.validationFailedCount)
        assertFalse("Tidak ada kegagalan retryable", summary.hasRetryableFailure)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: HTTP-level mapping (PRD Section 26)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN HTTP 422 WHEN push THEN items marked FAILED_VALIDATION and no failure signal`() = runTest {
        val item = queueItem("uuid-1")
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(item)
        coEvery { apiService.pushSyncBatch(any()) } returns httpError(422)

        val result = repository.pushBatchSync()

        assertTrue("422 terminal → tidak boleh memicu retry", result.isSuccess)
        coVerify {
            syncQueueDao.updateStatus(item.id, "FAILED_VALIDATION", any(), any())
        }
    }

    @Test
    fun `GIVEN HTTP 503 WHEN push THEN returns failure for retry`() = runTest {
        val item = queueItem("uuid-1")
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(item)
        coEvery { apiService.pushSyncBatch(any()) } returns httpError(503)

        val result = repository.pushBatchSync()

        assertTrue("5xx harus retryable → Result.failure", result.isFailure)
    }

    @Test
    fun `GIVEN HTTP 500 WHEN push THEN does not mark items terminal`() = runTest {
        val item = queueItem("uuid-1")
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(item)
        coEvery { apiService.pushSyncBatch(any()) } returns httpError(500)

        repository.pushBatchSync()

        // Item harus dibiarkan (tidak ditandai FAILED_VALIDATION) agar bisa di-retry.
        coVerify(exactly = 0) {
            syncQueueDao.updateStatus(item.id, "FAILED_VALIDATION", any(), any())
        }
    }

    @Test
    fun `GIVEN HTTP 409 WHEN push THEN items marked CONFLICT`() = runTest {
        val item = queueItem("uuid-1")
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(item)
        coEvery { apiService.pushSyncBatch(any()) } returns httpError(409)

        val result = repository.pushBatchSync()

        assertTrue("409 ditangani (bukan retry loop)", result.isSuccess)
        coVerify {
            syncQueueDao.updateStatus(item.id, "CONFLICT", any(), any())
        }
    }

    @Test
    fun `GIVEN HTTP 401 WHEN push THEN terminal not retry-loop`() = runTest {
        val item = queueItem("uuid-1")
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(item)
        coEvery { apiService.pushSyncBatch(any()) } returns httpError(401)

        val result = repository.pushBatchSync()

        assertTrue("401 butuh aksi user, bukan retry tak terbatas", result.isSuccess)
        coVerify {
            syncQueueDao.updateStatus(item.id, "FAILED_VALIDATION", any(), any())
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 3: Sukses & tidak ada item
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN no pending items WHEN push THEN success with zero counts`() = runTest {
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns emptyList()

        val summary = repository.pushBatchSync().getOrThrow()

        assertEquals(0, summary.totalProcessed)
    }

    @Test
    fun `GIVEN SYNCED item WHEN push THEN queue entry deleted`() = runTest {
        val item = queueItem("uuid-1")
        coEvery { syncQueueDao.getNextPendingBatch(any()) } returns listOf(item)
        coEvery { apiService.pushSyncBatch(any()) } returns okBody(
            SyncResultDto("uuid-1", "SYNCED", 10L, null)
        )

        repository.pushBatchSync()

        coVerify { syncQueueDao.deleteSynced(item.id) }
    }
}
