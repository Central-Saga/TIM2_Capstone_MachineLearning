package com.csm.kitchenguard.domain.repository

import com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak domain untuk proses Physical Stock Audit.
 * PRD Section 24.
 */
interface StockAuditRepository {

    /**
     * Menyimpan perhitungan stok sebagai draft ke database lokal.
     * Data ini dapat diubah berkali-kali sebelum di-submit akhir.
     */
    suspend fun saveDraft(draft: StockAuditDraftEntity): Result<Unit>

    /**
     * Mendapatkan list draft audit reaktif berdasarkan ID Shift.
     */
    fun getDrafts(shiftId: Long): Flow<List<StockAuditDraftEntity>>
    
    /**
     * Memicu proses submit akhir audit fisik ke server untuk shift tertentu.
     */
    suspend fun submitAudit(shiftId: Long, stationId: Long): Result<Unit>
}
