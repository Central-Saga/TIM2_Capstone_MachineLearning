package com.csm.kitchenguard.presentation

import com.csm.kitchenguard.domain.model.UserRole
import com.csm.kitchenguard.domain.model.UserRole.Companion.canAccessKitchenFeatures
import com.csm.kitchenguard.domain.model.UserRole.Companion.canManageConflicts
import com.csm.kitchenguard.domain.model.UserRole.Companion.isAdminLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test untuk [UserRole] RBAC helpers.
 *
 * Issue #26 — memverifikasi bahwa kontrol akses berbasis role bekerja dengan benar.
 * Issue #28 — DoD #3: test RBAC sebagai bagian dari cakupan test yang diperlukan.
 */
class UserRoleRbacTest {

    // ── fromValue parsing ─────────────────────────────────────────────────────

    @Test
    fun `fromValue parses STAFF case-insensitive`() {
        assertEquals(UserRole.STAFF, UserRole.fromValue("staff"))
        assertEquals(UserRole.STAFF, UserRole.fromValue("STAFF"))
        assertEquals(UserRole.STAFF, UserRole.fromValue("Staff"))
    }

    @Test
    fun `fromValue returns UNKNOWN for unrecognized role`() {
        assertEquals(UserRole.UNKNOWN, UserRole.fromValue("ADMIN"))
        assertEquals(UserRole.UNKNOWN, UserRole.fromValue(""))
        assertEquals(UserRole.UNKNOWN, UserRole.fromValue("manager"))
    }

    // ── canAccessKitchenFeatures ──────────────────────────────────────────────

    @Test
    fun `STAFF can access kitchen features`() {
        assertTrue(UserRole.STAFF.canAccessKitchenFeatures())
    }

    @Test
    fun `SUPERVISOR can access kitchen features`() {
        assertTrue(UserRole.SUPERVISOR.canAccessKitchenFeatures())
    }

    @Test
    fun `HEAD_CHEF can access kitchen features`() {
        assertTrue(UserRole.HEAD_CHEF.canAccessKitchenFeatures())
    }

    @Test
    fun `UNKNOWN cannot access kitchen features`() {
        assertFalse(UserRole.UNKNOWN.canAccessKitchenFeatures())
    }

    // ── canManageConflicts ────────────────────────────────────────────────────

    @Test
    fun `STAFF cannot manage conflicts`() {
        assertFalse(UserRole.STAFF.canManageConflicts())
    }

    @Test
    fun `SUPERVISOR can manage conflicts`() {
        assertTrue(UserRole.SUPERVISOR.canManageConflicts())
    }

    @Test
    fun `HEAD_CHEF can manage conflicts`() {
        assertTrue(UserRole.HEAD_CHEF.canManageConflicts())
    }

    // ── isAdminLevel ──────────────────────────────────────────────────────────

    @Test
    fun `only HEAD_CHEF is admin level`() {
        assertTrue(UserRole.HEAD_CHEF.isAdminLevel())
        assertFalse(UserRole.SUPERVISOR.isAdminLevel())
        assertFalse(UserRole.STAFF.isAdminLevel())
        assertFalse(UserRole.UNKNOWN.isAdminLevel())
    }
}
