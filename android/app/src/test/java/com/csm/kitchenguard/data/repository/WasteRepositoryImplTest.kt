package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.dao.WasteDao
import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk [WasteRepositoryImpl].
 *
 * Issue #9 — memastikan `clientEventAt` dari [WasteRecordEntity] ikut tersimpan
 * ke [SyncQueueEntity] sehingga tidak hilang di antrean.
 */
class WasteRepositoryImplTest {

    private val wasteDao: WasteDao = mockk(relaxed = true)
    private val syncQueueDao: SyncQueueDao = mockk(relaxed = true)
    private val gson = Gson()

    private val repository = WasteRepositoryImpl(wasteDao, syncQueueDao, gson)

    private fun wasteRecord(clientEventAt: String) = WasteRecordEntity(
        clientUuid = "c3a1b8e2-9f44-4e2b-b93d-8e42b10a2f91",
        idempotencyKey = "waste:c3a1b8e2-9f44-4e2b-b93d-8e42b10a2f91",
        shiftId = 10L,
        stationId = 2L,
        ingredientId = 1L,
        ingredientName = "Daging Ayam Broiler",
        batchId = 1L,
        quantity = 1.45,
        unit = "kg",
        reason = "SPOILED",
        clientEventAt = clientEventAt,
        aiClass = null,
        aiConfidence = null,
        aiModelVersion = null,
        ocrRawText = null,
        ocrConfidence = null,
        photoPath = null
    )

    @Test
    fun `GIVEN waste record WHEN saved THEN enqueues with original clientEventAt`() = runTest {
        coEvery { wasteDao.insertWaste(any()) } returns Unit
        coEvery { syncQueueDao.enqueue(any()) } returns 1L

        val record = wasteRecord("2026-09-16T13:30:00Z")
        val result = repository.saveWasteRecord(record)
        assertTrue(result.isSuccess)

        val captured = slot<SyncQueueEntity>()
        coVerify { syncQueueDao.enqueue(capture(captured)) }

        assertEquals(
            "clientEventAt asli harus tersalin ke antrean",
            "2026-09-16T13:30:00Z",
            captured.captured.clientEventAt
        )
    }

    @Test
    fun `GIVEN waste record WHEN saved THEN queue payload contains record json`() = runTest {
        coEvery { wasteDao.insertWaste(any()) } returns Unit
        coEvery { syncQueueDao.enqueue(any()) } returns 1L

        repository.saveWasteRecord(wasteRecord("2026-09-16T13:30:00Z"))

        val captured = slot<SyncQueueEntity>()
        coVerify { syncQueueDao.enqueue(capture(captured)) }

        // payloadJson harus memuat nilai clientEventAt yang benar (serialisasi Gson).
        assertTrue(
            "payloadJson harus memuat client_event_at yang benar",
            captured.captured.payloadJson.contains("2026-09-16T13:30:00Z")
        )
    }

    @Test
    fun `GIVEN waste record WHEN saved THEN queue metadata is correct`() = runTest {
        coEvery { wasteDao.insertWaste(any()) } returns Unit
        coEvery { syncQueueDao.enqueue(any()) } returns 1L

        val record = wasteRecord("2026-09-16T13:30:00Z")
        repository.saveWasteRecord(record)

        val captured = slot<SyncQueueEntity>()
        coVerify { syncQueueDao.enqueue(capture(captured)) }

        assertEquals(record.clientUuid, captured.captured.clientUuid)
        assertEquals(record.idempotencyKey, captured.captured.idempotencyKey)
        assertEquals("WASTE_RECORD", captured.captured.entityType)
        assertEquals("CREATE", captured.captured.operation)
    }
}
