package com.csm.kitchenguard.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test (Room nyata) untuk Issue #13:
 * membuktikan bahwa `clearSubmittedDrafts` saja TIDAK menghapus draft
 * (karena is_submitted default false), dan bahwa alur mark->clear berhasil.
 */
@RunWith(AndroidJUnit4::class)
class StockAuditClearFlowTest {

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

    private fun draft(ingredientId: Long) = StockAuditDraftEntity(
        id = 0,
        shiftId = 10L,
        stationId = 2L,
        ingredientId = ingredientId,
        batchId = 5L,
        actualPhysical = 2.0,
        unit = "liter",
        countedAt = 1_700_000_000_000L,
        isSubmitted = false
    )

    @Test
    fun `GIVEN drafts WHEN only clear without mark THEN nothing deleted`() = runTest {
        val dao = database.stockAuditDao()
        dao.insertOrUpdateDraft(draft(1L))
        dao.insertOrUpdateDraft(draft(2L))

        // Regresi issue #13: clear tanpa mark tidak menghapus apa pun.
        dao.clearSubmittedDrafts(10L)

        val remaining = dao.getDraftsByShift(10L).first()
        assertEquals("Draft tetap ada karena is_submitted=false", 2, remaining.size)
    }

    @Test
    fun `GIVEN drafts WHEN mark then clear THEN all deleted`() = runTest {
        val dao = database.stockAuditDao()
        dao.insertOrUpdateDraft(draft(1L))
        dao.insertOrUpdateDraft(draft(2L))

        val marked = dao.markSubmitted(10L)
        dao.clearSubmittedDrafts(10L)

        assertEquals(2, marked)
        assertTrue(
            "Setelah mark->clear, draft harus terhapus",
            dao.getDraftsByShift(10L).first().isEmpty()
        )
    }

    @Test
    fun `GIVEN draft with batch and unit WHEN inserted THEN persisted correctly`() = runTest {
        val dao = database.stockAuditDao()
        dao.insertOrUpdateDraft(draft(1L))

        val stored = dao.getDraftsByShift(10L).first().first()

        assertEquals("batchId harus tersimpan", 5L, stored.batchId)
        assertEquals("unit harus tersimpan", "liter", stored.unit)
    }
}
