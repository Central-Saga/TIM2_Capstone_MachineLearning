package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.data.local.dao.NotificationDao
import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.dao.WasteDao
import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import com.csm.kitchenguard.data.remote.api.KitchenGuardApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk [SyncRepositoryImpl.enqueueSyncItem].
 *
 * Issue #15 — fungsi ini sebelumnya kosong (no-op) sehingga tidak menulis apa pun
 * ke `sync_queue`. Test memverifikasi item benar-benar ditulis dengan field lengkap.
 */
class SyncRepositoryEnqueueItemTest {

    private val apiService: KitchenGuardApiService = mockk(relaxed = true)
    private val syncQueueDao: SyncQueueDao = mockk(relaxed = true)
    private val wasteDao: WasteDao = mockk(relaxed = true)
    private val masterDataDao: MasterDataDao = mockk(relaxed = true)
    private val shiftDao: ShiftDao = mockk(relaxed = true)
    private val notificationDao: NotificationDao = mockk(relaxed = true)

    private val repository = SyncRepositoryImpl(
        apiService, syncQueueDao, wasteDao, masterDataDao, shiftDao, notificationDao
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Item benar-benar ditulis (inti issue #15)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN enqueueSyncItem WHEN called THEN writes to syncQueueDao`() = runTest {
        coEvery { syncQueueDao.enqueue(any()) } returns 1L

        repository.enqueueSyncItem(
            clientUuid = "uuid-1",
            idempotencyKey = "audit:uuid-1",
            entityType = "AUDIT",
            operation = "CREATE",
            payloadJson = "{\"x\":1}",
            clientEventAt = "2026-09-16T13:30:00Z"
        )

        coVerify(exactly = 1) { syncQueueDao.enqueue(any()) }
    }

    @Test
    fun `GIVEN enqueueSyncItem WHEN called THEN all fields persisted`() = runTest {
        coEvery { syncQueueDao.enqueue(any()) } returns 1L

        repository.enqueueSyncItem(
            clientUuid = "uuid-1",
            idempotencyKey = "audit:uuid-1",
            entityType = "AUDIT",
            operation = "CREATE",
            payloadJson = "{\"x\":1}",
            clientEventAt = "2026-09-16T13:30:00Z"
        )

        val slot = slot<SyncQueueEntity>()
        coVerify { syncQueueDao.enqueue(capture(slot)) }
        val item = slot.captured

        assertEquals("uuid-1", item.clientUuid)
        assertEquals("audit:uuid-1", item.idempotencyKey)
        assertEquals("AUDIT", item.entityType)
        assertEquals("CREATE", item.operation)
        assertEquals("{\"x\":1}", item.payloadJson)
        assertEquals("2026-09-16T13:30:00Z", item.clientEventAt)
        assertEquals("PENDING", item.syncStatus)
        assertNull(item.errorMessage)
    }

    @Test
    fun `GIVEN clientEventAt WHEN enqueue THEN preserved not placeholder`() = runTest {
        coEvery { syncQueueDao.enqueue(any()) } returns 1L

        repository.enqueueSyncItem(
            clientUuid = "uuid-1",
            idempotencyKey = "audit:uuid-1",
            entityType = "AUDIT",
            operation = "CREATE",
            payloadJson = "{}",
            clientEventAt = "2026-09-16T13:30:00Z"
        )

        val slot = slot<SyncQueueEntity>()
        coVerify { syncQueueDao.enqueue(capture(slot)) }

        assertEquals(
            "clientEventAt asli harus tersimpan (tidak mengulang bug issue #9)",
            "2026-09-16T13:30:00Z",
            slot.captured.clientEventAt
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Nilai kembalian & error
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN enqueue success WHEN called THEN returns rowId`() = runTest {
        coEvery { syncQueueDao.enqueue(any()) } returns 42L

        val result = repository.enqueueSyncItem(
            "uuid-1", "audit:uuid-1", "AUDIT", "CREATE", "{}", null
        )

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull())
    }

    @Test
    fun `GIVEN dao returns -1 WHEN enqueue THEN surfaces -1`() = runTest {
        // `enqueue` memakai OnConflictStrategy.IGNORE; rowId -1 menandakan
        // konflik PrimaryKey (autoGenerate id) yang jarang terjadi.
        coEvery { syncQueueDao.enqueue(any()) } returns -1L

        val result = repository.enqueueSyncItem(
            "uuid-1", "audit:uuid-1", "AUDIT", "CREATE", "{}", null
        )

        assertEquals(-1L, result.getOrNull())
    }

    @Test
    fun `GIVEN null clientEventAt WHEN enqueue THEN accepted`() = runTest {
        coEvery { syncQueueDao.enqueue(any()) } returns 1L

        val result = repository.enqueueSyncItem(
            "uuid-1", "audit:uuid-1", "AUDIT", "CREATE", "{}", null
        )

        assertTrue(result.isSuccess)
        val slot = slot<SyncQueueEntity>()
        coVerify { syncQueueDao.enqueue(capture(slot)) }
        assertNull(slot.captured.clientEventAt)
    }

    @Test
    fun `GIVEN dao failure WHEN enqueue THEN returns failure`() = runTest {
        coEvery { syncQueueDao.enqueue(any()) } throws RuntimeException("db down")

        val result = repository.enqueueSyncItem(
            "uuid-1", "audit:uuid-1", "AUDIT", "CREATE", "{}", null
        )

        assertTrue(result.isFailure)
    }
}
