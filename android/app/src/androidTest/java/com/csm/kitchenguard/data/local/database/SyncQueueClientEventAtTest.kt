package com.csm.kitchenguard.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test untuk tabel `sync_queue` (Issue #9).
 * Memverifikasi kolom `client_event_at` tersimpan & terbaca kembali.
 */
@RunWith(AndroidJUnit4::class)
class SyncQueueClientEventAtTest {

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

    private fun entity(clientEventAt: String?) = SyncQueueEntity(
        clientUuid = "uuid-1",
        idempotencyKey = "waste:uuid-1",
        entityType = "WASTE_RECORD",
        operation = "CREATE",
        payloadJson = "{}",
        clientEventAt = clientEventAt,
        createdAt = 1_700_000_000_000L,
        errorMessage = null
    )

    @Test
    fun `GIVEN clientEventAt set WHEN enqueued THEN persisted and read back`() = runTest {
        val dao = database.syncQueueDao()
        dao.enqueue(entity("2026-09-16T13:30:00Z"))

        val pending = dao.getPendingQueue().first()

        assertEquals(1, pending.size)
        assertEquals("2026-09-16T13:30:00Z", pending.first().clientEventAt)
    }

    @Test
    fun `GIVEN clientEventAt null WHEN enqueued THEN persisted as null`() = runTest {
        val dao = database.syncQueueDao()
        dao.enqueue(entity(null))

        val pending = dao.getPendingQueue().first()

        assertNull(pending.first().clientEventAt)
    }
}
