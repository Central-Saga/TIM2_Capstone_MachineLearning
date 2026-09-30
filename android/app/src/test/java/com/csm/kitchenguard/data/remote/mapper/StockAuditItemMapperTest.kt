package com.csm.kitchenguard.data.remote.mapper

import com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk [StockAuditItemMapper].
 *
 * Issue #13 — batchId & unit harus diambil dari draft, bukan di-hardcode.
 */
class StockAuditItemMapperTest {

    private fun draft(
        ingredientId: Long = 1L,
        batchId: Long? = 7L,
        unit: String = "liter"
    ) = StockAuditDraftEntity(
        id = 0,
        shiftId = 10L,
        stationId = 2L,
        ingredientId = ingredientId,
        batchId = batchId,
        actualPhysical = 3.5,
        unit = unit,
        countedAt = 1_700_000_000_000L,
        isSubmitted = false
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Nilai asli diteruskan (inti issue #13)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN draft with batchId WHEN toDto THEN batchId preserved not null`() {
        val dto = StockAuditItemMapper.toDto(draft(batchId = 42L))

        assertEquals(
            "batchId harus dari draft, bukan di-hardcode null",
            42L,
            dto.batchId
        )
    }

    @Test
    fun `GIVEN draft with null batchId WHEN toDto THEN batchId stays null`() {
        val dto = StockAuditItemMapper.toDto(draft(batchId = null))

        assertNull("Audit level ingredient boleh null, tapi harus dari draft", dto.batchId)
    }

    @Test
    fun `GIVEN draft unit liter WHEN toDto THEN unit is liter not kg`() {
        val dto = StockAuditItemMapper.toDto(draft(unit = "liter"))

        assertEquals(
            "unit harus dari draft, bukan hardcoded 'kg'",
            "liter",
            dto.unit
        )
    }

    @Test
    fun `GIVEN draft unit pcs WHEN toDto THEN unit is pcs not kg`() {
        val dto = StockAuditItemMapper.toDto(draft(unit = "pcs"))

        assertEquals("pcs", dto.unit)
        assertNotEquals("kg", dto.unit)
    }

    @Test
    fun `GIVEN draft WHEN toDto THEN other fields mapped correctly`() {
        val dto = StockAuditItemMapper.toDto(draft(ingredientId = 99L))

        assertEquals(99L, dto.ingredientId)
        assertEquals(3.5, dto.actualPhysical, 0.0001)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Daftar
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN multiple drafts WHEN toDtoList THEN preserves order and values`() {
        val drafts = listOf(
            draft(ingredientId = 1L, unit = "kg", batchId = 1L),
            draft(ingredientId = 2L, unit = "liter", batchId = null),
            draft(ingredientId = 3L, unit = "pcs", batchId = 9L)
        )

        val dtos = StockAuditItemMapper.toDtoList(drafts)

        assertEquals(3, dtos.size)
        assertEquals("kg", dtos[0].unit)
        assertEquals("liter", dtos[1].unit)
        assertEquals("pcs", dtos[2].unit)
        assertNull(dtos[1].batchId)
        assertEquals(9L, dtos[2].batchId)
    }
}
