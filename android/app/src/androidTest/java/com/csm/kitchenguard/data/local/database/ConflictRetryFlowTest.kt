package com.csm.kitchenguard.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test end-to-end untuk alur "Kirim Ulang Semua Konflik" (Issue #11).
 *
 * Membuktikan dengan Room NYATA (bukan mock) bahwa:
 * 1. Item CONFLICT tidak terambil oleh `getNextPendingBatch`.
 * 2. Setelah reset pada KEDUA tabel, item kembali terambil (bisa dikirim ulang).
 */
@RunWith(AndroidJUnit4::class)
class ConflictRetryFlowTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun queueItem(uuid: String, status: String) = SyncQueueEntity(
        clientUuid = uuid,
        idempotencyKey = "waste:$uuid",
        entityType = "WASTE_RECORD",
        operation = "CREATE",
        payloadJson = "{}",
        clientEventAt = "2026-09-16T13:30:00Z",
        createdAt = 1_700_000_000_000L,
        syncStatus = status,
        errorMessage = if (status == "CONFLICT") "SHIFT_LOCKED" else null
    )

    private fun wasteRecord(uuid: String, status: String) = WasteRecordEntity(
        clientUuid = uuid,
        idempotencyKey = "waste:$uuid",
        shiftId = 10L,
        stationId = 2L,
        ingredientId = 1L,
        ingredientName = "Ayam",
        batchId = null,
        quantity = 1.0,
        unit = "kg",
        reason = "SPOILED",
        clientEventAt = "2026-09-16T13:30:00Z",
        aiClass = null,
        aiConfidence = null,
        aiModelVersion = null,
        ocrRawText = null,
        ocrConfidence = null,
        photoPath = null,
        syncStatus = status
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Membuktikan bug — CONFLICT tidak terambil
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN item CONFLICT WHEN getNextPendingBatch THEN not fetched`() = runTest {
        val syncDao = database.syncQueueDao()
        syncDao.enqueue(queueItem("uuid-1", "CONFLICT"))

        val batch = syncDao.getNextPendingBatch(20)

        assertTrue(
            "Item CONFLICT tidak boleh muncul di batch — inilah penyebab tombol tidak berefek",
            batch.isEmpty()
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Perbaikan — reset kedua tabel membuat item terkirim ulang
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN conflicts WHEN both tables reset THEN item becomes fetchable`() = runTest {
        val syncDao = database.syncQueueDao()
        val wasteDao = database.wasteDao()

        syncDao.enqueue(queueItem("uuid-1", "CONFLICT"))
        wasteDao.insertWaste(wasteRecord("uuid-1", "CONFLICT"))

        // Reset kedua tabel (perilaku retryConflictedItems).
        val queueReset = syncDao.resetConflictToPending()
        wasteDao.resetConflictToPending()

        assertEquals(1, queueReset)

        val batch = syncDao.getNextPendingBatch(20)
        assertEquals("Item harus terambil kembali setelah reset", 1, batch.size)
        assertEquals("PENDING", batch.first().syncStatus)
    }

    @Test
    fun `GIVEN conflicts WHEN only waste_reset THEN queue item stays unfetchable`() = runTest {
        // Regresi: memperlihatkan mengapa mereset waste_records SAJA tidak cukup
        // (perilaku kode lama yang salah).
        val syncDao = database.syncQueueDao()
        val wasteDao = database.wasteDao()

        syncDao.enqueue(queueItem("uuid-1", "CONFLICT"))
        wasteDao.insertWaste(wasteRecord("uuid-1", "CONFLICT"))

        // HANYA reset tabel waste (bug lama).
        wasteDao.resetConflictToPending()

        val batch = syncDao.getNextPendingBatch(20)
        assertTrue(
            "Tanpa reset antrean, item tetap tidak terkirim — bug issue #11",
            batch.isEmpty()
        )
    }

    @Test
    fun `GIVEN reset WHEN done THEN error_message cleared`() = runTest {
        val syncDao = database.syncQueueDao()
        syncDao.enqueue(queueItem("uuid-1", "CONFLICT"))

        syncDao.resetConflictToPending()

        val item = syncDao.getByClientUuid("uuid-1")
        assertNotNull(item)
        assertEquals("PENDING", item!!.syncStatus)
        assertNull("Pesan konflik lama harus dibersihkan", item.errorMessage)
    }

    @Test
    fun `GIVEN reset runs WHEN no conflicts THEN returns zero`() = runTest {
        val syncDao = database.syncQueueDao()
        syncDao.enqueue(queueItem("uuid-1", "PENDING"))

        val reset = syncDao.resetConflictToPending()

        assertEquals(0, reset)
    }
}
