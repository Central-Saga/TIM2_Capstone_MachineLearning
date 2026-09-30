package com.csm.kitchenguard.domain.usecase

import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.entity.ActiveShiftEntity
import com.csm.kitchenguard.domain.repository.WasteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * State class untuk merangkum seluruh kondisi Kitchen Station Hub.
 */
data class StationSummaryState(
    val activeShift: ActiveShiftEntity?,
    val totalWasteQuantity: Double,
    val pendingSyncCount: Int
)

/**
 * Use case untuk membentuk agregasi data (Dashboard Hub)
 * dengan menggabungkan tiga sumber Flow berbeda.
 * PRD Section 10 & 11.
 */
class GetStationSummaryUseCase(
    private val shiftDao: ShiftDao,
    private val wasteRepository: WasteRepository,
    private val syncQueueDao: SyncQueueDao
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<StationSummaryState> {
        
        // 1. Pantau Shift Aktif
        val shiftFlow = shiftDao.getActiveShift()
        
        // 2. Pantau Antrean Pending (jumlah item yang belum disinkronisasi)
        val pendingCountFlow = syncQueueDao.getPendingQueue().map { it.size }
        
        // 3. Gunakan flatMapLatest: setiap kali shiftFlow berubah, ambil total waste
        //    untuk shift tersebut. Jika tidak ada shift, waste = 0.0
        val totalWasteFlow = shiftFlow.flatMapLatest { shift ->
            if (shift != null) {
                wasteRepository.getTotalShiftWaste(shift.shiftId)
            } else {
                flowOf(0.0)
            }
        }

        // 4. Kombinasikan ketiga Flow menjadi satu state utuh.
        //    UI (ViewModel) hanya perlu `.collect()` pada fungsi ini satu kali.
        return combine(
            shiftFlow, 
            totalWasteFlow, 
            pendingCountFlow
        ) { shift, totalWaste, pendingCount ->
            StationSummaryState(
                activeShift = shift,
                totalWasteQuantity = totalWaste,
                pendingSyncCount = pendingCount
            )
        }
    }
}
