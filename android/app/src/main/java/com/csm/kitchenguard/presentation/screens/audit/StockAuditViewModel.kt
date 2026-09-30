package com.csm.kitchenguard.presentation.screens.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity
import com.csm.kitchenguard.domain.repository.StockAuditRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuditUiState(
    val drafts: List<StockAuditDraftEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel untuk manajemen form stock opname / audit akhir shift.
 * PRD Section 24 — Physical Stock Audit.
 */
class StockAuditViewModel(
    private val shiftDao: ShiftDao,
    private val auditRepository: StockAuditRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AuditUiState> = shiftDao.getActiveShift()
        .flatMapLatest { shift ->
            if (shift != null) {
                auditRepository.getDrafts(shift.shiftId).flatMapLatest { draftList ->
                    flowOf(AuditUiState(drafts = draftList, isLoading = false))
                }
            } else {
                flowOf(AuditUiState(isLoading = false))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuditUiState(isLoading = true)
        )

    fun submitAuditAkhirShift() {
        viewModelScope.launch {
            val shift = shiftDao.getActiveShiftOnce() ?: return@launch
            
            val result = auditRepository.submitAudit(shift.shiftId, shift.stationId)
            
            // Note: Pada implementasi riil, state update harus dikombinasikan dengan StateFlow utama,
            // untuk kesederhanaan, kita bisa menyimpan state error tambahan di MutableStateFlow eksternal,
            // tapi karena Flow kita berasal dari DB, error state bisa di-handle di event channel / separate flow.
        }
    }
}
