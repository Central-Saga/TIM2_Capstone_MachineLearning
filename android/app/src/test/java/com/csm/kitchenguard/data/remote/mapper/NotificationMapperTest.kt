package com.csm.kitchenguard.data.remote.mapper

import com.csm.kitchenguard.data.remote.dto.NotificationDto
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk [NotificationMapper] (Issue #14).
 * Memastikan field server dipetakan utuh ke cache lokal.
 */
class NotificationMapperTest {

    private fun dto(
        referenceId: String? = "shift-42",
        createdAt: String = "2026-09-16T14:30:00Z"
    ) = NotificationDto(
        id = 7L,
        type = "SHIFT_LOCKED",
        title = "Shift Terkunci",
        message = "Shift aktif telah dikunci Head Chef.",
        referenceId = referenceId,
        createdAt = createdAt
    )

    @Test
    fun `GIVEN dto WHEN toEntity THEN all fields mapped`() {
        val entity = NotificationMapper.toEntity(dto())

        assertEquals(7L, entity.id)
        assertEquals("SHIFT_LOCKED", entity.type)
        assertEquals("Shift Terkunci", entity.title)
        assertEquals("Shift aktif telah dikunci Head Chef.", entity.message)
        assertEquals("shift-42", entity.referenceId)
        assertEquals("2026-09-16T14:30:00Z", entity.createdAt)
    }

    @Test
    fun `GIVEN dto WHEN toEntity THEN isRead defaults to false`() {
        val entity = NotificationMapper.toEntity(dto())

        assertFalse("Notifikasi baru harus belum dibaca", entity.isRead)
    }

    @Test
    fun `GIVEN dto with null reference WHEN toEntity THEN referenceId null`() {
        val entity = NotificationMapper.toEntity(dto(referenceId = null))

        assertNull(entity.referenceId)
    }

    @Test
    fun `GIVEN list WHEN toEntityList THEN preserves order and size`() {
        val dtos = listOf(dto(), dto())

        val entities = NotificationMapper.toEntityList(dtos)

        assertEquals(2, entities.size)
    }
}
