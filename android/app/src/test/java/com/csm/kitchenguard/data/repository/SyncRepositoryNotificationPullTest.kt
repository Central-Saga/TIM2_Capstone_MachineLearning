package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.data.local.dao.NotificationDao
import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.dao.WasteDao
import com.csm.kitchenguard.data.local.entity.NotificationEntity
import com.csm.kitchenguard.data.remote.api.KitchenGuardApiService
import com.csm.kitchenguard.data.remote.dto.NotificationDto
import com.csm.kitchenguard.data.remote.dto.SyncPullResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

/**
 * Unit test untuk [SyncRepositoryImpl.pullMasterData] — pemetaan notifikasi (Issue #14).
 *
 * Membuktikan cache `notifications_cache` kini diisi dari hasil pull sync,
 * tidak lagi selalu kosong.
 */
class SyncRepositoryNotificationPullTest {

    private val apiService: KitchenGuardApiService = mockk()
    private val syncQueueDao: SyncQueueDao = mockk(relaxed = true)
    private val wasteDao: WasteDao = mockk(relaxed = true)
    private val masterDataDao: MasterDataDao = mockk(relaxed = true)
    private val shiftDao: ShiftDao = mockk(relaxed = true)
    private val notificationDao: NotificationDao = mockk(relaxed = true)

    private val repository = SyncRepositoryImpl(
        apiService, syncQueueDao, wasteDao, masterDataDao, shiftDao, notificationDao
    )

    private fun pullResponse(notifications: List<NotificationDto>?) = SyncPullResponse(
        nextSyncCursor = 1L,
        ingredients = emptyList(),
        batches = emptyList(),
        activeShift = null,
        notifications = notifications
    )

    private val sampleNotification = NotificationDto(
        id = 1L,
        type = "SYNC_CONFLICT",
        title = "Konflik Sinkronisasi",
        message = "Ada transaksi tertahan.",
        referenceId = "uuid-1",
        createdAt = "2026-09-16T14:30:00Z"
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Notifikasi tersimpan (inti issue #14)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN pull has notifications WHEN pullMasterData THEN saved via notificationDao`() = runTest {
        coEvery { apiService.pullSync(any()) } returns
            Response.success(pullResponse(listOf(sampleNotification)))

        val result = repository.pullMasterData()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { notificationDao.insertNotifications(any()) }
    }

    @Test
    fun `GIVEN pull has notifications WHEN pullMasterData THEN fields mapped correctly`() = runTest {
        coEvery { apiService.pullSync(any()) } returns
            Response.success(pullResponse(listOf(sampleNotification)))

        repository.pullMasterData()

        val slot = slot<List<NotificationEntity>>()
        coVerify { notificationDao.insertNotifications(capture(slot)) }

        val saved = slot.captured.single()
        assertEquals(1L, saved.id)
        assertEquals("SYNC_CONFLICT", saved.type)
        assertEquals("Konflik Sinkronisasi", saved.title)
        assertEquals("uuid-1", saved.referenceId)
        assertFalse("Notifikasi baru belum dibaca", saved.isRead)
    }

    @Test
    fun `GIVEN pull has null notifications WHEN pullMasterData THEN does not touch cache`() = runTest {
        coEvery { apiService.pullSync(any()) } returns
            Response.success(pullResponse(null))

        repository.pullMasterData()

        // notifications null → jangan hapus/timpa cache lama.
        coVerify(exactly = 0) { notificationDao.insertNotifications(any()) }
    }

    @Test
    fun `GIVEN pull has empty notifications WHEN pullMasterData THEN inserts empty list`() = runTest {
        coEvery { apiService.pullSync(any()) } returns
            Response.success(pullResponse(emptyList()))

        repository.pullMasterData()

        val slot = slot<List<NotificationEntity>>()
        coVerify { notificationDao.insertNotifications(capture(slot)) }
        assertTrue(slot.captured.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Query baca
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `WHEN getNotifications THEN delegates to dao`() = runTest {
        repository.getNotifications()
        coVerify { notificationDao.getAllNotifications() }
    }

    @Test
    fun `GIVEN 3 unread WHEN getUnreadNotificationCount THEN emits 3`() = runTest {
        coEvery { notificationDao.getUnreadNotifications() } returns
            kotlinx.coroutines.flow.flowOf(
                listOf(
                    sampleNotificationEntity(1L),
                    sampleNotificationEntity(2L),
                    sampleNotificationEntity(3L)
                )
            )

        val count = repository.getUnreadNotificationCount().first()

        assertEquals(3, count)
    }

    @Test
    fun `WHEN markNotificationAsRead THEN delegates to dao`() = runTest {
        repository.markNotificationAsRead(9L)
        coVerify { notificationDao.markAsRead(9L) }
    }

    private fun sampleNotificationEntity(id: Long) = NotificationEntity(
        id = id,
        type = "T",
        title = "t",
        message = "m",
        referenceId = null,
        isRead = false,
        createdAt = "2026-09-16T14:30:00Z"
    )
}
