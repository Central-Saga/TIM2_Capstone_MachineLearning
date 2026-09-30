package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.data.local.dao.StockAuditDao
import com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity
import com.csm.kitchenguard.data.remote.api.KitchenGuardApiService
import com.csm.kitchenguard.data.remote.dto.StockAuditRequest
import com.csm.kitchenguard.data.remote.mapper.StockAuditItemMapper
import com.csm.kitchenguard.domain.repository.StockAuditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class StockAuditRepositoryImpl(
    private val auditDao: StockAuditDao,
    private val apiService: KitchenGuardApiService
) : StockAuditRepository {

    override suspend fun saveDraft(draft: StockAuditDraftEntity): Result<Unit> {
        return try {
            auditDao.insertOrUpdateDraft(draft)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDrafts(shiftId: Long): Flow<List<StockAuditDraftEntity>> {
        return auditDao.getDraftsByShift(shiftId)
    }

    override suspend fun submitAudit(shiftId: Long, stationId: Long): Result<Unit> {
        return try {
            // Ambil snapshot data draft saat ini (satu kali)
            val drafts = auditDao.getDraftsByShift(shiftId).firstOrNull() ?: emptyList()
            
            if (drafts.isEmpty()) {
                return Result.failure(Exception("Tidak ada data draft untuk shift ini."))
            }

            // Petakan ke model Request DTO.
            // batchId & unit diambil dari draft (Issue #13) — TIDAK di-hardcode,
            // agar variance di server dihitung dengan satuan/batch yang benar.
            val auditItems = StockAuditItemMapper.toDtoList(drafts)

            val request = StockAuditRequest(
                shiftId = shiftId,
                stationId = stationId,
                items = auditItems
            )

            // Submit ke API 
            val response = apiService.submitStockAudit(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                // Tandai submitted LALU hapus (Issue #13):
                // is_submitted default-nya false, sehingga clearSubmittedDrafts
                // saja TIDAK akan menghapus apa pun tanpa langkah penandaan ini.
                auditDao.markSubmitted(shiftId)
                auditDao.clearSubmittedDrafts(shiftId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Gagal submit audit: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
