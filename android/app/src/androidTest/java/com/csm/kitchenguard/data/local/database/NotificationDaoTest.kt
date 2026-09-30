package com.csm.kitchenguard.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.csm.kitchenguard.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test (Room nyata) untuk `NotificationDao` (Issue #14).
 * Memverifikasi upsert, urutan, unread counter, dan mark-as-read.
 */
@RunWith(AndroidJUnit4::class)
class NotificationDaoTest {

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

    private fun entity(id: Long, isRead: Boolean = false, createdAt: String) =
        NotificationEntity(
            id = id,
            type = "SYNC_CONFLICT",
            title = "T$id",
            message = "M$id",
            referenceId = null,
            isRead = isRead,
            createdAt = createdAt
        )

    @Test
    fun `GIVEN notifications WHEN inserted THEN readable`() = runTest {
        val dao = database.notificationDao()
        dao.insertNotifications(
            listOf(
                entity(1L, createdAt = "2026-09-16T10:00:00Z"),
                entity(2L, createdAt = "2026-09-16T11:00:00Z")
            )
        )

        val all = dao.getAllNotifications().first()

        assertEquals(2, all.size)
    }

    @Test
    fun `GIVEN notifications WHEN getAll THEN newest first`() = runTest {
        val dao = database.notificationDao()
        dao.insertNotifications(
            listOf(
                entity(1L, createdAt = "2026-09-16T10:00:00Z"),
                entity(2L, createdAt = "2026-09-17T10:00:00Z"),
                entity(3L, createdAt = "2026-09-15T10:00:00Z")
            )
        )

        val all = dao.getAllNotifications().first()

        assertEquals(2L, all[0].id)
        assertEquals(1L, all[1].id)
        assertEquals(3L, all[2].id)
    }

    @Test
    fun `GIVEN same id WHEN inserted twice THEN replaced not duplicated`() = runTest {
        val dao = database.notificationDao()
        dao.insertNotifications(listOf(entity(1L, createdAt = "2026-09-16T10:00:00Z")))
        dao.insertNotifications(
            listOf(entity(1L, createdAt = "2026-09-16T10:00:00Z").copy(title = "UPDATED"))
        )

        val all = dao.getAllNotifications().first()
        assertEquals(1, all.size)
        assertEquals("UPDATED", all.first().title)
    }

    @Test
    fun `GIVEN unread mixed WHEN getUnreadNotifications THEN only unread and count correct`() = runTest {
        val dao = database.notificationDao()
        dao.insertNotifications(
            listOf(
                entity(1L, isRead = false, createdAt = "2026-09-16T10:00:00Z"),
                entity(2L, isRead = true, createdAt = "2026-09-16T11:00:00Z"),
                entity(3L, isRead = false, createdAt = "2026-09-16T12:00:00Z")
            )
        )

        val unread = dao.getUnreadNotifications().first()

        assertEquals(2, unread.size)
        assertEquals(2, dao.getUnreadCount())
    }

    @Test
    fun `GIVEN unread WHEN markAsRead THEN count decreases`() = runTest {
        val dao = database.notificationDao()
        dao.insertNotifications(listOf(entity(1L, isRead = false, createdAt = "2026-09-16T10:00:00Z")))

        dao.markAsRead(1L)

        assertEquals(0, dao.getUnreadCount())
    }

    @Test
    fun `GIVEN unread WHEN markAllAsRead THEN all read`() = runTest {
        val dao = database.notificationDao()
        dao.insertNotifications(
            listOf(
                entity(1L, isRead = false, createdAt = "2026-09-16T10:00:00Z"),
                entity(2L, isRead = false, createdAt = "2026-09-16T11:00:00Z")
            )
        )

        dao.markAllAsRead()

        assertEquals(0, dao.getUnreadCount())
    }
}
