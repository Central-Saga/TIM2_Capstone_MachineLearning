package com.csm.kitchenguard.presentation.screens.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csm.kitchenguard.data.local.entity.NotificationEntity
import com.csm.kitchenguard.domain.repository.SyncRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State layar Notification Center (Figma M08).
 *
 * Notifikasi berasal dari cache lokal (`notifications_cache`) yang diisi oleh
 * pull sync — bukan data hardcoded (Issue #21).
 */
data class NotificationCenterUiState(
    val isLoading: Boolean = true,
    val notifications: List<NotificationEntity> = emptyList(),
    val unreadCount: Int = 0
) {
    /** True bila belum ada notifikasi ter-cache. */
    val isEmpty: Boolean get() = notifications.isEmpty()
}

/**
 * ViewModel untuk Notification Center.
 *
 * Membaca notifikasi nyata dari [SyncRepository] (Issue #14 & #21).
 */
class NotificationViewModel(
    private val syncRepository: SyncRepository
) : ViewModel() {

    val uiState: StateFlow<NotificationCenterUiState> = syncRepository.getNotifications()
        .map { list ->
            NotificationCenterUiState(
                isLoading = false,
                notifications = list,
                unreadCount = list.count { !it.isRead }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationCenterUiState(isLoading = true)
        )

    /** Tandai satu notifikasi sebagai sudah dibaca. */
    fun onNotificationClicked(id: Long) {
        viewModelScope.launch {
            syncRepository.markNotificationAsRead(id)
        }
    }
}
