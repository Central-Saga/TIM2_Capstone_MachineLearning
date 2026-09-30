package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.data.local.dao.StockAuditDao
import com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity
import com.csm.kitchenguard.data.remote.api.KitchenGuardApiService
import com.csm.kitchenguard.data.remote.dto.StockAuditRequest
import com.csm.kitchenguard.data.remote.dto.StockAuditResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

/**
 * Unit test untuk [StockAuditRepositoryImpl.submitAudit].
 *
 * Issue #13 — verifikasi:
 * 1. batchId & unit dikirim dari draft (bukan hardcoded).
 * 2. Draft ditandai submitted lalu dibersihkan setelah sukses.
 */
class StockAuditRepositoryImplTest {

    private val auditDao: StockAuditDao = mockk(relaxed = true)
    private val apiService: KitchenGuardApiService = mockk()

    private val repository = StockAuditRepositoryImpl(auditDao, apiService)

    private fun draft(
        ingredientId: Long = 1L,
        batchId: Long? = 5L,
        unit: String = "liter"
    ) = StockAuditDraftEntity(
        id = 0,
        shiftId = 10L,
        stationId = 2L,
        ingredientId = ingredientId,
        batchId = batchId,
        actualPhysical = 2.0,
        unit = unit,
        countedAt = 1_700_000_000_000L,
        isSubmitted = false
    )

    private fun successResponse() = Response.success(
        StockAuditResponse(auditId = 100L, success = true, message = null)
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Payload mapping (inti issue #13)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN drafts WHEN submit THEN request carries real unit and batchId`() = runTest {
        coEvery { auditDao.getDraftsByShift(10L) } returns flowOf(
            listOf(draft(ingredientId = 1L, batchId = 5L, unit = "liter"))
        )
        coEvery { apiService.submitStockAudit(any()) } returns successResponse()

        repository.submitAudit(shiftId = 10L, stationId = 2L)

        val requestSlot = slot<StockAuditRequest>()
        coVerify { apiService.submitStockAudit(capture(requestSlot)) }

        val item = requestSlot.captured.items.first()
        assertEquals("unit harus dari draft", "liter", item.unit)
        assertEquals("batchId harus dari draft", 5L, item.batchId)
    }

    @Test
    fun `GIVEN draft unit kg WHEN submit THEN unit is kg from draft not placeholder`() = runTest {
        coEvery { auditDao.getDraftsByShift(10L) } returns flowOf(
            listOf(draft(unit = "kg"))
        )
        coEvery { apiService.submitStockAudit(any()) } returns successResponse()

        repository.submitAudit(10L, 2L)

        val requestSlot = slot<StockAuditRequest>()
        coVerify { apiService.submitStockAudit(capture(requestSlot)) }
        assertEquals("kg", requestSlot.captured.items.first().unit)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Alur mark -> clear (inti issue #13)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN server success WHEN submit THEN drafts marked submitted`() = runTest {
        coEvery { auditDao.getDraftsByShift(10L) } returns flowOf(listOf(draft()))
        coEvery { apiService.submitStockAudit(any()) } returns successResponse()

        repository.submitAudit(10L, 2L)

        coVerify(exactly = 1) { auditDao.markSubmitted(10L) }
    }

    @Test
    fun `GIVEN server success WHEN submit THEN submitted drafts cleared`() = runTest {
        coEvery { auditDao.getDraftsByShift(10L) } returns flowOf(listOf(draft()))
        coEvery { apiService.submitStockAudit(any()) } returns successResponse()

        repository.submitAudit(10L, 2L)

        coVerify(exactly = 1) { auditDao.clearSubmittedDrafts(10L) }
    }

    @Test
    fun `GIVEN server success WHEN submit THEN mark happens before clear`() = runTest {
        coEvery { auditDao.getDraftsByShift(10L) } returns flowOf(listOf(draft()))
        coEvery { apiService.submitStockAudit(any()) } returns successResponse()

        repository.submitAudit(10L, 2L)

        // Verifikasi urutan: markSubmitted dipanggil lebih dulu dari clear.
        coVerify {
            auditDao.markSubmitted(10L)
            auditDao.clearSubmittedDrafts(10L)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 3: Kondisi gagal
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN server failure WHEN submit THEN drafts NOT marked or cleared`() = runTest {
        coEvery { auditDao.getDraftsByShift(10L) } returns flowOf(listOf(draft()))
        coEvery { apiService.submitStockAudit(any()) } returns Response.success(
            StockAuditResponse(auditId = 0L, success = false, message = "rejected")
        )

        val result = repository.submitAudit(10L, 2L)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { auditDao.markSubmitted(any()) }
        coVerify(exactly = 0) { auditDao.clearSubmittedDrafts(any()) }
    }

    @Test
    fun `GIVEN no drafts WHEN submit THEN fails without calling API`() = runTest {
        coEvery { auditDao.getDraftsByShift(10L) } returns flowOf(emptyList())

        val result = repository.submitAudit(10L, 2L)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { apiService.submitStockAudit(any()) }
    }
}
