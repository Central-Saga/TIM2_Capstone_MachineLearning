package com.csm.kitchenguard.presentation.screens.profile

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk [ProfileUiState] (Issue #21).
 *
 * Memastikan state menampilkan nilai dari sesi nyata dan fallback netral
 * (bukan persona/hardcoded) bila data kosong.
 */
class ProfileUiStateTest {

    @Test
    fun `GIVEN session data WHEN mapped THEN name role id station real`() {
        val state = ProfileUiState(
            isLoading = false,
            name = "Siti Aminah",
            role = "Inventory Checker",
            employeeId = "CSM-KT-104",
            stationName = "Cold Kitchen"
        )

        assertEquals("Siti Aminah", state.name)
        assertEquals("Inventory Checker", state.roleLabel)
        assertEquals("CSM-KT-104", state.employeeId)
        assertEquals("Cold Kitchen", state.stationLabel)
    }

    @Test
    fun `GIVEN empty values WHEN labels THEN neutral not fake persona`() {
        val state = ProfileUiState(isLoading = false)

        assertEquals("—", state.roleLabel)
        assertEquals("—", state.stationLabel)
        // Tidak boleh menampilkan persona hardcoded.
        assertNotEquals("Marsel Lino Fulbertus", state.name)
    }

    @Test
    fun `GIVEN name WHEN initials THEN derived from real name`() {
        val state = ProfileUiState(name = "Budi Santoso")

        assertEquals("BS", state.initials)
    }

    @Test
    fun `GIVEN single name WHEN initials THEN single letter`() {
        val state = ProfileUiState(name = "Sukarno")

        assertEquals("S", state.initials)
    }

    @Test
    fun `GIVEN blank name WHEN initials THEN fallback question mark`() {
        val state = ProfileUiState(name = "")

        assertEquals("?", state.initials)
    }

    @Test
    fun `GIVEN no active shift WHEN shift labels THEN neutral`() {
        val state = ProfileUiState(activeShiftId = null)

        assertEquals("—", state.activeShiftLabel)
        assertEquals("Belum ada shift", state.activeShiftSubtitle)
    }

    @Test
    fun `GIVEN active shift WHEN shift labels THEN shows shift id`() {
        val state = ProfileUiState(activeShiftId = 42L)

        assertEquals("#42", state.activeShiftLabel)
        assertEquals("Shift Aktif", state.activeShiftSubtitle)
    }
}
