package com.csm.kitchenguard.presentation.components.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test untuk [resolveCameraPermissionState] (Issue #18).
 *
 * Memverifikasi pemetaan sinyal Android → state UI, khususnya deteksi
 * "ditolak permanen" yang harus mengarahkan user ke Settings.
 */
class CameraPermissionStateTest {

    @Test
    fun `GIVEN granted WHEN resolve THEN GRANTED`() {
        val state = resolveCameraPermissionState(
            granted = true,
            hasRequestedBefore = false,
            showRationale = false
        )

        assertEquals(CameraPermissionState.GRANTED, state)
    }

    @Test
    fun `GIVEN granted even after denial WHEN resolve THEN GRANTED`() {
        val state = resolveCameraPermissionState(
            granted = true,
            hasRequestedBefore = true,
            showRationale = false
        )

        assertEquals(
            "Izin yang sudah diberikan harus menang atas sinyal lain",
            CameraPermissionState.GRANTED,
            state
        )
    }

    @Test
    fun `GIVEN not granted and never requested WHEN resolve THEN DENIED`() {
        val state = resolveCameraPermissionState(
            granted = false,
            hasRequestedBefore = false,
            showRationale = false
        )

        assertEquals(
            "Belum pernah diminta → masih bisa minta izin",
            CameraPermissionState.DENIED,
            state
        )
    }

    @Test
    fun `GIVEN not granted but rationale needed WHEN resolve THEN DENIED`() {
        val state = resolveCameraPermissionState(
            granted = false,
            hasRequestedBefore = true,
            showRationale = true
        )

        assertEquals(
            "Rationale masih tampil → boleh minta ulang",
            CameraPermissionState.DENIED,
            state
        )
    }

    @Test
    fun `GIVEN not granted requested before and no rationale WHEN resolve THEN PERMANENTLY_DENIED`() {
        val state = resolveCameraPermissionState(
            granted = false,
            hasRequestedBefore = true,
            showRationale = false
        )

        assertEquals(
            "Ditolak permanen → arahkan ke Settings",
            CameraPermissionState.PERMANENTLY_DENIED,
            state
        )
    }
}
