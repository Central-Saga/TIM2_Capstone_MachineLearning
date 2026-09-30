package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.data.local.dao.NotificationDao
import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.dao.WasteDao
import com.csm.kitchenguard.data.remote.api.KitchenGuardApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk [SyncRepositoryImpl.retryConflictedItems].
 *
 * Issue #11 — memastikan "Kirim Ulang Semua Konflik" benar-benar mereset
 * antrean (`sync_queue`) DAN tabel sumber (`waste_records`). Mereset salah satu
 * saja membuat tombol tidak berefek.
 */
class SyncRepositoryRetryConflictsTest {

    private val apiService: KitchenGuardApiService = mockk(relaxed = true)
    private val syncQueueDao: SyncQueueDao = mockk(relaxed = true)
    private val wasteDao: WasteDao = mockk(relaxed = true)
    private val masterDataDao: MasterDataDao = mockk(relaxed = true)
    private val shiftDao: ShiftDao = mockk(relaxed = true)
    private val notificationDao: NotificationDao = mockk(relaxed = true)

    private val repository = SyncRepositoryImpl(
        apiService = apiService,
        syncQueueDao = syncQueueDao,
        wasteDao = wasteDao,
        masterDataDao = masterDataDao,
        shiftDao = shiftDao,
        notificationDao = notificationDao
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Kedua tabel harus di-reset (inti issue #11)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `WHEN retryConflictedItems THEN resets sync_queue conflicts`() = runTest {
        coEvery { syncQueueDao.resetConflictToPending() } returns 3
        coEvery { wasteDao.resetConflictToPending() } returns 3

        repository.retryConflictedItems()

        coVerify(exactly = 1) { syncQueueDao.resetConflictToPending() }
    }

    @Test
    fun `WHEN retryConflictedItems THEN resets waste_records conflicts too`() = runTest {
        coEvery { syncQueueDao.resetConflictToPending() } returns 3
        coEvery { wasteDao.resetConflictToPending() } returns 3

        repository.retryConflictedItems()

        coVerify(exactly = 1) { wasteDao.resetConflictToPending() }
    }

    @Test
    fun `WHEN retryConflictedItems THEN both tables are reset`() = runTest {
        coEvery { syncQueueDao.resetConflictToPending() } returns 2
        coEvery { wasteDao.resetConflictToPending() } returns 2

        val result = repository.retryConflictedItems()

        assertTrue(result.isSuccess)
        coVerify { syncQueueDao.resetConflictToPending() }
        coVerify { wasteDao.resetConflictToPending() }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Nilai kembalian & penanganan error
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `WHEN retryConflictedItems THEN returns queue reset count`() = runTest {
        coEvery { syncQueueDao.resetConflictToPending() } returns 5
        coEvery { wasteDao.resetConflictToPending() } returns 5

        val result = repository.retryConflictedItems()

        assertEquals(5, result.getOrNull())
    }

    @Test
    fun `GIVEN no conflicts WHEN retryConflictedItems THEN returns zero`() = runTest {
        coEvery { syncQueueDao.resetConflictToPending() } returns 0
        coEvery { wasteDao.resetConflictToPending() } returns 0

        val result = repository.retryConflictedItems()

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun `GIVEN dao failure WHEN retryConflictedItems THEN returns failure`() = runTest {
        coEvery { syncQueueDao.resetConflictToPending() } throws
            RuntimeException("db down")

        val result = repository.retryConflictedItems()

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }
}
