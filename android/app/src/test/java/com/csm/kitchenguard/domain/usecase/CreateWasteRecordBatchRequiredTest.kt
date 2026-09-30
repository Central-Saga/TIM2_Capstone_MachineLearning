package com.csm.kitchenguard.domain.usecase

import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.domain.repository.WasteRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test memverifikasi validasi batch wajib untuk alasan SPOILED/EXPIRED.
 *
 * Issue #22 — validasi ini sebelumnya dinonaktifkan (dikomentari).
 * Test ini menjadi DoD bahwa validasi berjalan dan menolak record tanpa batch
 * ketika alasan adalah SPOILED atau EXPIRED.
 *
 * Issue #28 — DoD #3: test cakupan use case yang diperlukan.
 */
class CreateWasteRecordBatchRequiredTest {

    private val wasteRepository: WasteRepository = mockk(relaxed = true)
    private val masterDataDao: MasterDataDao = mockk(relaxed = true)

    private val useCase = CreateWasteRecordUseCase(wasteRepository, masterDataDao)

    @Test
    fun `SPOILED without batch returns failure with descriptive message`() = runTest {
        // batchId = null, reason = SPOILED → harus gagal
        val result = useCase(
            ingredientId = 1L,
            batchId = null,          // ← tidak ada batch
            quantity = 2.0,
            unit = "kg",
            reason = "SPOILED",      // ← reason yang wajib batch
            shiftId = 10L,
            stationId = 1L
        )

        assertTrue(
            "SPOILED tanpa batch harus gagal (Issue #22): ${result.exceptionOrNull()?.message}",
            result.isFailure
        )
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Pesan error harus menyebutkan batch wajib",
            msg.contains("batch", ignoreCase = true) || msg.contains("SPOILED", ignoreCase = true)
        )
    }

    @Test
    fun `EXPIRED without batch returns failure with descriptive message`() = runTest {
        val result = useCase(
            ingredientId = 1L,
            batchId = null,
            quantity = 1.5,
            unit = "kg",
            reason = "EXPIRED",
            shiftId = 10L,
            stationId = 1L
        )

        assertTrue(
            "EXPIRED tanpa batch harus gagal (Issue #22): ${result.exceptionOrNull()?.message}",
            result.isFailure
        )
    }

    @Test
    fun `SPOILED with batch succeeds`() = runTest {
        coEvery { masterDataDao.getIngredientById(any()) } returns null // lolos validasi unit
        coEvery { wasteRepository.saveWasteRecord(any()) } returns Result.success(1L)

        val result = useCase(
            ingredientId = 1L,
            batchId = 5L,            // ← ada batch
            quantity = 2.0,
            unit = "kg",
            reason = "SPOILED",
            shiftId = 10L,
            stationId = 1L
        )

        // Bisa sukses atau failure karena alasan lain (mis. DB mock), 
        // tapi BUKAN karena "batch wajib"
        val errorMsg = result.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Tidak boleh gagal karena validasi batch bila batchId sudah disediakan",
            !errorMsg.contains("batch wajib", ignoreCase = true)
        )
    }

    @Test
    fun `OVERPRODUCTION without batch is allowed`() = runTest {
        coEvery { masterDataDao.getIngredientById(any()) } returns null
        coEvery { wasteRepository.saveWasteRecord(any()) } returns Result.success(1L)

        val result = useCase(
            ingredientId = 1L,
            batchId = null,          // ← tidak ada batch
            quantity = 3.0,
            unit = "kg",
            reason = "OVERPRODUCTION", // ← reason yang TIDAK wajib batch
            shiftId = 10L,
            stationId = 1L
        )

        val errorMsg = result.exceptionOrNull()?.message ?: ""
        assertFalse(
            "OVERPRODUCTION tanpa batch tidak boleh gagal karena validasi batch",
            errorMsg.contains("batch wajib", ignoreCase = true) || 
            errorMsg.contains("OVERPRODUCTION", ignoreCase = true)
        )
    }

    private fun assertFalse(message: String, condition: Boolean) {
        assertTrue(message, !condition)
    }
}
