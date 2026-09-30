package com.csm.kitchenguard.domain.usecase

import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.data.local.entity.BatchEntity
import com.csm.kitchenguard.data.local.entity.IngredientEntity
import com.csm.kitchenguard.data.local.entity.enums.WasteReason
import com.csm.kitchenguard.domain.repository.WasteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit test untuk validasi batch pada [CreateWasteRecordUseCase]
 * (PRD Section 14 / Issue #22).
 *
 * Aturan: alasan SPOILED/EXPIRED mewajibkan `batchId` JIKA bahan memiliki
 * batch aktif. Bila bahan tidak punya batch aktif, boleh tanpa batch.
 */
class CreateWasteRecordBatchValidationTest {

    private lateinit var useCase: CreateWasteRecordUseCase

    private val wasteRepository: WasteRepository = mockk()
    private val masterDataDao: MasterDataDao = mockk()

    private val ingredient = IngredientEntity(
        id = 1L, name = "Daging Sapi", category = "Meat", canonicalUnit = "kg", minimumStock = 0.5
    )
    private val activeBatch = BatchEntity(
        id = 10L, ingredientId = 1L, batchCode = "B-001",
        expiryAt = "2026-09-20T00:00:00Z", remainingQuantity = 5.0, status = "ACTIVE"
    )

    @Before
    fun setUp() {
        useCase = CreateWasteRecordUseCase(wasteRepository, masterDataDao)
        coEvery { masterDataDao.getIngredientById(any()) } returns ingredient
        coEvery { wasteRepository.saveWasteRecord(any()) } returns Result.success(Unit)
    }

    private suspend fun callUseCase(batchId: Long?, reason: String) = useCase(
        ingredientId = 1L,
        batchId = batchId,
        quantity = 1.5,
        unit = "kg",
        reason = reason,
        shiftId = 101L,
        stationId = 1L,
        aiClass = null,
        aiConfidence = null,
        aiModelVersion = null,
        ocrRawText = null,
        ocrConfidence = null,
        photoPath = null
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Bahan PUNYA batch aktif → SPOILED/EXPIRED wajib batch
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN SPOILED without batch and active batch exists THEN fails`() = runTest {
        coEvery { masterDataDao.getActiveBatchesOnce(1L) } returns listOf(activeBatch)

        val result = callUseCase(batchId = null, reason = WasteReason.SPOILED.value)

        assertTrue("SPOILED tanpa batch harus gagal", result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("Batch wajib") == true
        )
        coVerify(exactly = 0) { wasteRepository.saveWasteRecord(any()) }
    }

    @Test
    fun `GIVEN EXPIRED without batch and active batch exists THEN fails`() = runTest {
        coEvery { masterDataDao.getActiveBatchesOnce(1L) } returns listOf(activeBatch)

        val result = callUseCase(batchId = null, reason = WasteReason.EXPIRED.value)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { wasteRepository.saveWasteRecord(any()) }
    }

    @Test
    fun `GIVEN SPOILED with valid batch THEN succeeds`() = runTest {
        coEvery { masterDataDao.getActiveBatchesOnce(1L) } returns listOf(activeBatch)

        val result = callUseCase(batchId = 10L, reason = WasteReason.SPOILED.value)

        assertTrue("SPOILED dengan batch valid harus sukses", result.isSuccess)
        coVerify(exactly = 1) { wasteRepository.saveWasteRecord(any()) }
    }

    @Test
    fun `GIVEN SPOILED with batch not belonging to ingredient THEN fails`() = runTest {
        coEvery { masterDataDao.getActiveBatchesOnce(1L) } returns listOf(activeBatch)

        val result = callUseCase(batchId = 999L, reason = WasteReason.SPOILED.value)

        assertTrue("Batch yang tidak dikenal harus ditolak", result.isFailure)
        coVerify(exactly = 0) { wasteRepository.saveWasteRecord(any()) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Bahan TIDAK punya batch aktif → boleh tanpa batch
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN SPOILED without batch and no active batch THEN succeeds`() = runTest {
        coEvery { masterDataDao.getActiveBatchesOnce(1L) } returns emptyList()

        val result = callUseCase(batchId = null, reason = WasteReason.SPOILED.value)

        assertTrue(
            "PRD: wajib batch hanya bila stok punya batch aktif",
            result.isSuccess
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 3: Alasan lain tidak mewajibkan batch
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN OVERPRODUCTION without batch THEN succeeds even if active batch exists`() = runTest {
        coEvery { masterDataDao.getActiveBatchesOnce(1L) } returns listOf(activeBatch)

        val result = callUseCase(batchId = null, reason = WasteReason.OVERPRODUCTION.value)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `GIVEN TRIMMING without batch THEN succeeds`() = runTest {
        coEvery { masterDataDao.getActiveBatchesOnce(1L) } returns listOf(activeBatch)

        val result = callUseCase(batchId = null, reason = WasteReason.TRIMMING.value)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `GIVEN unknown reason string THEN treated as OTHER and does not require batch`() = runTest {
        coEvery { masterDataDao.getActiveBatchesOnce(1L) } returns listOf(activeBatch)

        val result = callUseCase(batchId = null, reason = "SOMETHING_WEIRD")

        assertTrue(result.isSuccess)
    }
}
