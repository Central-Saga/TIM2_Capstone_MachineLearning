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
 * Instrumented test (Room nyata) untuk jalur enqueue (Issue #15).
 *
 * Membuktikan item yang ditulis dengan field sebagaimana dipakai
 * `SyncRepositoryImpl.enqueueSyncItem` benar-benar berada di `sync_queue`
 * dan siap diambil batch berikutnya.
 */
@RunWith(AndroidJUnit4::class)
class EnqueueSyncItemFlowTest {

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

    /** Membentuk entity persis seperti yang ditulis `enqueueSyncItem`. */
    private fun enqueued(entityType: String, uuid: String, clientEventAt: String?) =
        SyncQueueEntity(
            clientUuid = uuid,
            idempotencyKey = "$entityType:$uuid",
            entityType = entityType,
            operation = "CREATE",
            payloadJson = "{}",
            clientEventAt = clientEventAt,
            createdAt = 1_700_000_000_000L,
            errorMessage = null
        )

    @Test
    fun `GIVEN audit item enqueued WHEN read THEN present in queue`() = runTest {
        val dao = database.syncQueueDao()
        dao.enqueue(enqueued("AUDIT", "audit-1", "2026-09-16T13:30:00Z"))

        val pending = dao.getPendingQueue().first()

        assertEquals(1, pending.size)
        assertEquals("AUDIT", pending.first().entityType)
        assertEquals("AUDIT:audit-1", pending.first().idempotencyKey)
        assertEquals("2026-09-16T13:30:00Z", pending.first().clientEventAt)
    }

    @Test
    fun `GIVEN enqueued item WHEN getNextPendingBatch THEN fetchable`() = runTest {
        val dao = database.syncQueueDao()
        dao.enqueue(enqueued("AUDIT", "audit-1", "2026-09-16T13:30:00Z"))

        val batch = dao.getNextPendingBatch(20)

        assertEquals("Item harus siap dikirim setelah enqueue", 1, batch.size)
        assertEquals("PENDING", batch.first().syncStatus)
    }

    @Test
    fun `GIVEN item enqueued WHEN read by clientUuid THEN retrievable`() = runTest {
        val dao = database.syncQueueDao()
        dao.enqueue(enqueued("AUDIT", "audit-1", null))

        val found = dao.getByClientUuid("audit-1")

        assertNotNull(found)
        assertEquals("AUDIT", found!!.entityType)
    }
}
