package com.csm.kitchenguard.presentation.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.csm.kitchenguard.data.local.preferences.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * State profil pengguna yang ditampilkan di layar Profile (Figma M09).
 *
 * Semua nilai berasal dari sumber nyata ([SessionManager]); tidak ada data
 * persona hardcoded (Issue #21). Field yang belum tersedia ditampilkan netral.
 */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val role: String = "",
    val employeeId: String = "",
    val stationName: String = "",
    val activeShiftId: Long? = null,
    /** True bila sesi pengguna belum/tidak tersedia. */
    val isSessionMissing: Boolean = false
) {
    /** Inisial nama untuk fallback avatar (tanpa foto). */
    val initials: String
        get() = name.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }

    /** Deskripsi jabatan; fallback netral bila role kosong. */
    val roleLabel: String
        get() = role.ifBlank { "—" }

    /** Nama stasiun; fallback netral bila kosong. */
    val stationLabel: String
        get() = stationName.ifBlank { "—" }

    /** Label shift aktif dari data nyata sesi; netral bila belum ada shift. */
    val activeShiftLabel: String
        get() = activeShiftId?.let { "#$it" } ?: "—"

    /** Subtitle shift; jujur menyatakan belum ada shift aktif bila null. */
    val activeShiftSubtitle: String
        get() = if (activeShiftId != null) "Shift Aktif" else "Belum ada shift"
}

/**
 * ViewModel untuk layar Profile.
 *
 * Membaca sesi pengguna nyata dari [SessionManager] (Issue #21) sehingga nama,
 * role, dan stasiun yang ditampilkan bukan data palsu.
 */
class ProfileViewModel(
    application: Application,
    sessionManager: SessionManager
) : AndroidViewModel(application) {

    val uiState: StateFlow<ProfileUiState> = sessionManager.getUserSession()
        .map { session ->
            if (session == null) {
                ProfileUiState(isLoading = false, isSessionMissing = true)
            } else {
                ProfileUiState(
                    isLoading = false,
                    name = session.name,
                    role = session.role,
                    employeeId = session.employeeId,
                    stationName = session.stationName,
                    activeShiftId = session.activeShiftId,
                    isSessionMissing = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState(isLoading = true)
        )
}
