package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.dao.WasteDao
import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import com.csm.kitchenguard.domain.repository.WasteRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WasteRepositoryImpl(
    private val wasteDao: WasteDao,
    private val syncQueueDao: SyncQueueDao,
    private val gson: Gson
) : WasteRepository {

    /**
     * OFFLINE-FIRST LOGIC:
     * Menyimpan data langsung ke database lokal dan memasukkannya ke antrean sinkronisasi.
     * Tidak menunggu koneksi internet atau respon server.
     */
    override suspend fun saveWasteRecord(waste: WasteRecordEntity): Result<Unit> {
        return try {
            // 1. Simpan ke Database Lokal (tampil seketika di UI)
            wasteDao.insertWaste(waste)

            // 2. Siapkan payload JSON untuk sinkronisasi
            val payload = gson.toJson(waste)

            // 3. Masukkan ke antrean Push Sync (WorkManager akan membaca ini)
            val syncQueueEntity = SyncQueueEntity(
                clientUuid = waste.clientUuid,
                idempotencyKey = waste.idempotencyKey,
                entityType = "WASTE_RECORD",
                operation = "CREATE",
                payloadJson = payload,
                // Salin timestamp event ASLI dari entitas — jangan hilang di antrean.
                clientEventAt = waste.clientEventAt,
                createdAt = System.currentTimeMillis(),
                errorMessage = null
            )
            syncQueueDao.enqueue(syncQueueEntity)

            // 4. Selesai — kembalikan Result Sukses tanpa nge-block thread network
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getWasteHistory(shiftId: Long): Flow<List<WasteRecordEntity>> {
        return wasteDao.getWasteByShift(shiftId)
    }

    override fun getTotalShiftWaste(shiftId: Long): Flow<Double> {
        return wasteDao.getTotalWasteQuantity(shiftId).map { it ?: 0.0 }
    }
}
