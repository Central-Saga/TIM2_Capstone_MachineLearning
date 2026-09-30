package com.csm.kitchenguard.presentation.screens.hub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.csm.kitchenguard.data.local.dao.WasteDao
import com.csm.kitchenguard.data.local.sync.SyncScheduler
import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import com.csm.kitchenguard.domain.repository.SyncRepository
import com.csm.kitchenguard.domain.repository.WasteRepository
import com.csm.kitchenguard.domain.usecase.GetStationSummaryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** State gabungan untuk Dashboard Utama Stasiun. */
data class HubUiState(
    val stationName: String = "-",
    val shiftName: String = "No Active Shift",
    val totalWasteWeight: Double = 0.0,
    val pendingSyncCount: Int = 0,
    val recentWasteList: List<WasteRecordEntity> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * ViewModel untuk Dashboard / Hub layar utama.
 * Menggabungkan usecase ringkasan stasiun dengan histori waste.
 */
class KitchenHubViewModel(
    application: Application,
    private val getStationSummaryUseCase: GetStationSummaryUseCase,
    private val wasteRepository: WasteRepository,
    private val wasteDao: WasteDao,
    private val syncRepository: SyncRepository
) : AndroidViewModel(application) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HubUiState> = getStationSummaryUseCase()
        .flatMapLatest { summary ->
            // Ketika summary berubah, ambil list recent waste jika ada shift
            if (summary.activeShift != null) {
                wasteRepository.getWasteHistory(summary.activeShift.shiftId).map { historyList ->
                    HubUiState(
                        stationName = summary.activeShift.stationName,
                        shiftName = "Shift ID: ${summary.activeShift.shiftId}", // Bisa diganti format jam
                        totalWasteWeight = summary.totalWasteQuantity,
                        pendingSyncCount = summary.pendingSyncCount,
                        recentWasteList = historyList,
                        isLoading = false
                    )
                }
            } else {
                flowOf(
                    HubUiState(
                        pendingSyncCount = summary.pendingSyncCount,
                        isLoading = false
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Hemat resource saat background
            initialValue = HubUiState(isLoading = true)
        )

    /**
     * Stream reaktif daftar transaksi yang mengalami CONFLICT (Shift Locked).
     * Langsung terhubung ke Room DB — otomatis update saat status berubah.
     */
    val conflictedWaste = wasteDao.getConflictedWaste()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Reset semua record CONFLICT kembali ke PENDING, lalu trigger Immediate Sync.
     * Dipanggil saat user menekan tombol "Kirim Ulang" di ConflictWarningBanner.
     *
     * Issue #11: reset HARUS dilakukan pada antrean (`sync_queue`) DAN tabel
     * sumber (`waste_records`). Keduanya ditangani oleh
     * [SyncRepository.retryConflictedItems] agar konsisten.
     */
    fun retryConflicts() {
        viewModelScope.launch {
            syncRepository.retryConflictedItems()
            SyncScheduler.triggerImmediateSync(getApplication())
        }
    }
}
