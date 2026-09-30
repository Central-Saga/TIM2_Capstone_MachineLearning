package com.csm.kitchenguard.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.csm.kitchenguard.data.local.database.AppDatabase
import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test untuk [WasteDao] menggunakan In-Memory Room Database.
 * Harus dijalankan di environment Android (androidTest) karena membutuhkan Context.
 * PRD Section 18 — Offline Local Database.
 */
@RunWith(AndroidJUnit4::class)
class WasteDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var wasteDao: WasteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // In-Memory DB: tidak menulis ke disk, otomatis terhapus setelah test selesai
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Hanya untuk test, bukan production
            .build()
        wasteDao = database.wasteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Insert & Query
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun insertWaste_andRetrieveByShift_returnsCorrectRecord() = runTest {
        val waste = buildWasteRecord(clientUuid = "uuid-001", shiftId = 10L, quantity = 1.5)

        wasteDao.insertWaste(waste)

        val result = wasteDao.getWasteByShift(10L).first()
        assertEquals(1, result.size)
        assertEquals("uuid-001", result[0].clientUuid)
        assertEquals(1.5, result[0].quantity, 0.001)
    }

    @Test
    fun insertWaste_forDifferentShifts_queryReturnsOnlyMatchingShift() = runTest {
        wasteDao.insertWaste(buildWasteRecord(clientUuid = "uuid-A", shiftId = 1L))
        wasteDao.insertWaste(buildWasteRecord(clientUuid = "uuid-B", shiftId = 2L))
        wasteDao.insertWaste(buildWasteRecord(clientUuid = "uuid-C", shiftId = 1L))

        val result = wasteDao.getWasteByShift(1L).first()

        assertEquals("Hanya 2 record untuk shift 1", 2, result.size)
        assertTrue(result.all { it.shiftId == 1L })
    }

    @Test
    fun insertWaste_withSameUuid_replacesExistingRecord() = runTest {
        val original = buildWasteRecord(clientUuid = "uuid-dup", quantity = 1.0)
        val updated = buildWasteRecord(clientUuid = "uuid-dup", quantity = 3.0)

        wasteDao.insertWaste(original)
        wasteDao.insertWaste(updated) // OnConflictStrategy.REPLACE

        val result = wasteDao.getWasteByShift(1L).first()
        assertEquals("Harus tetap 1 record (idempotent)", 1, result.size)
        assertEquals(3.0, result[0].quantity, 0.001)
    }

    @Test
    fun getWasteByShift_whenNoRecords_returnsEmptyList() = runTest {
        val result = wasteDao.getWasteByShift(999L).first()
        assertTrue("Harus kosong jika tidak ada record", result.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Pembaruan Status Sync
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun updateSyncStatus_fromPendingToSynced_reflectsCorrectly() = runTest {
        val waste = buildWasteRecord(clientUuid = "sync-01", syncStatus = "PENDING")
        wasteDao.insertWaste(waste)

        wasteDao.updateSyncStatus("sync-01", "SYNCED")

        val result = wasteDao.getWasteByShift(1L).first()
        assertEquals("Status harus berubah menjadi SYNCED", "SYNCED", result[0].syncStatus)
    }

    @Test
    fun updateSyncStatus_toConflict_recordIsNotDeleted() = runTest {
        val waste = buildWasteRecord(clientUuid = "conflict-01", syncStatus = "PENDING")
        wasteDao.insertWaste(waste)

        wasteDao.updateSyncStatus("conflict-01", "CONFLICT")

        val result = wasteDao.getWasteByShift(1L).first()
        // Record harus tetap ada (tidak dihapus) dengan status CONFLICT
        assertEquals(1, result.size)
        assertEquals("CONFLICT", result[0].syncStatus)
    }

    @Test
    fun getConflictedWaste_afterUpdateToConflict_returnsRecord() = runTest {
        wasteDao.insertWaste(buildWasteRecord(clientUuid = "c1", syncStatus = "PENDING"))
        wasteDao.insertWaste(buildWasteRecord(clientUuid = "c2", syncStatus = "SYNCED"))
        wasteDao.updateSyncStatus("c1", "CONFLICT")

        val result = wasteDao.getConflictedWaste().first()
        assertEquals("Hanya record CONFLICT yang dikembalikan", 1, result.size)
        assertEquals("c1", result[0].clientUuid)
    }

    @Test
    fun resetConflictToPending_changesAllConflictsToPending() = runTest {
        wasteDao.insertWaste(buildWasteRecord(clientUuid = "r1", syncStatus = "CONFLICT"))
        wasteDao.insertWaste(buildWasteRecord(clientUuid = "r2", syncStatus = "CONFLICT"))

        wasteDao.resetConflictToPending()

        val conflicts = wasteDao.getConflictedWaste().first()
        assertTrue("Tidak boleh ada lagi record CONFLICT setelah reset", conflicts.isEmpty())
    }

    @Test
    fun getPendingWaste_returnsOnlyPendingAndFailed() = runTest {
        wasteDao.insertWaste(buildWasteRecord(clientUuid = "p1", syncStatus = "PENDING"))
        wasteDao.insertWaste(buildWasteRecord(clientUuid = "p2", syncStatus = "FAILED"))
        wasteDao.insertWaste(buildWasteRecord(clientUuid = "p3", syncStatus = "SYNCED"))
        wasteDao.insertWaste(buildWasteRecord(clientUuid = "p4", syncStatus = "CONFLICT"))

        val result = wasteDao.getPendingWaste().first()
        assertEquals("Hanya PENDING dan FAILED dikembalikan", 2, result.size)
        assertTrue(result.all { it.syncStatus in listOf("PENDING", "FAILED") })
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildWasteRecord(
        clientUuid: String = "test-uuid",
        shiftId: Long = 1L,
        quantity: Double = 1.0,
        syncStatus: String = "PENDING"
    ) = WasteRecordEntity(
        clientUuid = clientUuid,
        idempotencyKey = "waste:$clientUuid",
        shiftId = shiftId,
        stationId = 1L,
        ingredientId = 1L,
        ingredientName = "Test Ingredient",
        batchId = null,
        quantity = quantity,
        unit = "kg",
        reason = "TRIM",
        photoPath = null,
        aiClass = null,
        aiConfidence = null,
        aiModelVersion = null,
        ocrRawText = null,
        ocrConfidence = null,
        clientEventAt = "2026-09-17T02:00:00Z",
        syncStatus = syncStatus
    )
}
