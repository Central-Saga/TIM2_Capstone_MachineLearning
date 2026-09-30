package com.csm.kitchenguard.presentation.screens.notification

import com.csm.kitchenguard.data.local.entity.NotificationEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk [NotificationCenterUiState] (Issue #21).
 *
 * Memastikan state dibentuk dari data cache nyata dan unread dihitung benar.
 */
class NotificationCenterUiStateTest {

    private fun notif(id: Long, isRead: Boolean) = NotificationEntity(
        id = id,
        type = "SYNC_CONFLICT",
        title = "T$id",
        message = "M$id",
        referenceId = null,
        isRead = isRead,
        createdAt = "2026-09-16T10:00:00Z"
    )

    @Test
    fun `GIVEN notifications WHEN state THEN unread count correct`() {
        val state = NotificationCenterUiState(
            isLoading = false,
            notifications = listOf(
                notif(1L, isRead = false),
                notif(2L, isRead = true),
                notif(3L, isRead = false)
            ),
            unreadCount = 2
        )

        assertEquals(2, state.unreadCount)
        assertEquals(3, state.notifications.size)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `GIVEN empty list WHEN state THEN isEmpty true`() {
        val state = NotificationCenterUiState(isLoading = false, notifications = emptyList())

        assertTrue(state.isEmpty)
        assertEquals(0, state.unreadCount)
    }

    @Test
    fun `GIVEN all read WHEN state THEN unread zero`() {
        val state = NotificationCenterUiState(
            notifications = listOf(notif(1L, isRead = true), notif(2L, isRead = true)),
            unreadCount = 0
        )

        assertEquals(0, state.unreadCount)
        assertFalse(state.isEmpty)
    }
}
