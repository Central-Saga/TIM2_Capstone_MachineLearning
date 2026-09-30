package com.csm.kitchenguard.domain.repository

import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak domain untuk manajemen data Waste.
 * Murni OFFLINE-FIRST — tidak pernah menyentuh internet secara langsung.
 * PRD Section 12.
 */
interface WasteRepository {

    /**
     * Menyimpan data pencatatan waste secara lokal dan mengantrekannya
     * untuk disinkronkan ke server.
     * Operasi ini bersifat instan (sinkron terhadap Room) dan selalu sukses
     * selama kapasitas memori mencukupi.
     */
    suspend fun saveWasteRecord(waste: WasteRecordEntity): Result<Unit>

    /**
     * Mendapatkan riwayat waste untuk shift tertentu secara reaktif.
     * Flow akan emit list baru setiap kali ada penambahan data.
     */
    fun getWasteHistory(shiftId: Long): Flow<List<WasteRecordEntity>>

    /**
     * Mendapatkan total akumulasi waste (dalam kuantitas) untuk shift tertentu.
     * Data yang sedang berstatus CONFLICT akan diabaikan.
     */
    fun getTotalShiftWaste(shiftId: Long): Flow<Double>
}
