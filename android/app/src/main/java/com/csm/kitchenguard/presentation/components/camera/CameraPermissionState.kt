package com.csm.kitchenguard.presentation.components.camera

/**
 * Status izin kamera yang memengaruhi UI (Issue #18).
 *
 * Logika pemetaan dari hasil `checkSelfPermission` + `shouldShowRequestPermissionRationale`
 * dipisahkan sebagai fungsi murni agar dapat diuji tanpa runtime Android.
 */
enum class CameraPermissionState {
    /** Izin sudah diberikan — kamera boleh dibuka. */
    GRANTED,

    /**
     * Izin belum diberikan, tetapi masih layak menampilkan rationale & meminta ulang.
     * (Belum pernah ditolak permanen.)
     */
    DENIED,

    /**
     * Izin ditolak permanen (user memilih "Don't ask again" atau menolak total).
     * Permintaan ulang tidak akan menampilkan dialog; arahkan ke Settings.
     */
    PERMANENTLY_DENIED
}

/**
 * Logika penentuan [CameraPermissionState] dari sinyal Android.
 *
 * @param granted            hasil `ContextCompat.checkSelfPermission(...) == GRANTED`.
 * @param hasRequestedBefore true bila dialog permintaan pernah ditampilkan.
 * @param showRationale      hasil `shouldShowRequestPermissionRationale(...)`.
 */
fun resolveCameraPermissionState(
    granted: Boolean,
    hasRequestedBefore: Boolean,
    showRationale: Boolean
): CameraPermissionState = when {
    granted -> CameraPermissionState.GRANTED
    // Belum pernah diminta, atau sistem meminta menampilkan rationale → masih bisa minta ulang.
    !hasRequestedBefore || showRationale -> CameraPermissionState.DENIED
    // Sudah pernah diminta, tidak granted, dan rationale tidak lagi ditampilkan → ditolak permanen.
    else -> CameraPermissionState.PERMANENTLY_DENIED
}
