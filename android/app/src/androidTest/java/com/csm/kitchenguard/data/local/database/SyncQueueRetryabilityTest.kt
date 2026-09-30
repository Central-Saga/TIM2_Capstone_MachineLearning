package com.csm.kitchenguard.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test (Room nyata) untuk Issue #12:
 * item terminal (FAILED_VALIDATION / CONFLICT) TIDAK boleh diambil kembali
 * oleh `getNextPendingBatch`, sementara status retryable (FAILED) tetap diambil.
 */
@RunWith(AndroidJUnit4::class)
class SyncQueueRetryabilityTest {

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

    private fun item(uuid: String, status: String) = SyncQueueEntity(
        clientUuid = uuid,
        idempotencyKey = "waste:$uuid",
        entityType = "WASTE_RECORD",
        operation = "CREATE",
        payloadJson = "{}",
        clientEventAt = "2026-09-16T13:30:00Z",
        createdAt = 1_700_000_000_000L,
        syncStatus = status,
        errorMessage = null
    )

    @Test
    fun `GIVEN item FAILED_VALIDATION WHEN getNextPendingBatch THEN excluded`() = runTest {
        val dao = database.syncQueueDao()
        dao.enqueue(item("uuid-1", "FAILED_VALIDATION"))

        val batch = dao.getNextPendingBatch(20)

        assertTrue(
            "Item terminal tidak boleh di-retry selamanya (Issue #12)",
            batch.isEmpty()
        )
    }

    @Test
    fun `GIVEN item PENDING WHEN getNextPendingBatch THEN included`() = runTest {
        val dao = database.syncQueueDao()
        dao.enqueue(item("uuid-1", "PENDING"))

        assertEquals(1, dao.getNextPendingBatch(20).size)
    }

    @Test
    fun `GIVEN item FAILED retryable WHEN getNextPendingBatch THEN included`() = runTest {
        val dao = database.syncQueueDao()
        dao.enqueue(item("uuid-1", "FAILED"))

        assertEquals(1, dao.getNextPendingBatch(20).size)
    }

    @Test
    fun `GIVEN item CONFLICT WHEN getNextPendingBatch THEN excluded`() = runTest {
        val dao = database.syncQueueDao()
        dao.enqueue(item("uuid-1", "CONFLICT"))

        assertTrue(dao.getNextPendingBatch(20).isEmpty())
    }

    @Test
    fun `GIVEN mixed statuses WHEN getNextPendingBatch THEN only retryable returned`() = runTest {
        val dao = database.syncQueueDao()
        dao.enqueue(item("p", "PENDING"))
        dao.enqueue(item("f", "FAILED"))
        dao.enqueue(item("v", "FAILED_VALIDATION"))
        dao.enqueue(item("c", "CONFLICT"))

        val batch = dao.getNextPendingBatch(20)

        assertEquals(2, batch.size)
        assertTrue(batch.map { it.clientUuid }.containsAll(listOf("p", "f")))
    }
}
