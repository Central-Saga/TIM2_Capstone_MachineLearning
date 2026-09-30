package com.csm.kitchenguard.data.local.preferences

/**
 * Model abstraksi yang membungkus status sesi pengguna saat ini.
 * Memudahkan layer ViewModel dalam mengamati status login secara utuh (tidak terpisah-pisah per field).
 * PRD Section 9 — Authentication & Role Context.
 */
data class UserSessionModel(
    val employeeId: String,
    val name: String,
    val role: String,
    val stationId: Long,
    val stationName: String,
    val activeShiftId: Long?
)
