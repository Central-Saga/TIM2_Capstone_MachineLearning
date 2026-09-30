package com.csm.kitchenguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk operasi tabel `waste_records`.
 * PRD Section 12 — Waste Logging Flow.
 * PRD Section 19 — Sync State & Architecture.
 */
@Dao
interface WasteDao {

    /**
     * Simpan waste record baru ke local DB secara instan (offline-first).
     * Menggunakan REPLACE agar idempotent: jika [WasteRecordEntity.clientUuid]
     * (PrimaryKey) sudah ada, data lama diganti — retry aman tanpa duplikasi lokal.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaste(waste: WasteRecordEntity)

    /**
     * Stream semua waste record untuk shift tertentu, diurutkan terbaru duluan.
     * Digunakan di Hub Screen untuk menampilkan "Aktivitas Terbaru".
     * Flow otomatis emit ulang saat data berubah (insert/update).
     */
    @Query("SELECT * FROM waste_records WHERE shift_id = :shiftId ORDER BY client_event_at DESC")
    fun getWasteByShift(shiftId: Long): Flow<List<WasteRecordEntity>>

    /**
     * Stream waste record yang belum berhasil tersinkronisasi ke server.
     * Mencakup status PENDING (belum pernah dicoba) dan FAILED (perlu retry).
     * Digunakan untuk menampilkan Pending Sync Counter di Hub Screen.
     */
    @Query(
        """
        SELECT * FROM waste_records
        WHERE sync_status IN ('PENDING', 'FAILED')
        ORDER BY client_event_at ASC
        """
    )
    fun getPendingWaste(): Flow<List<WasteRecordEntity>>

    /**
     * Update sync status setelah WorkManager memproses pengiriman ke server.
     * Dipanggil dengan status baru berdasarkan response API:
     * SYNCED, FAILED, SYNCING, atau CONFLICT.
     *
     * @param clientUuid UUID record yang diperbarui
     * @param status     Nilai dari [com.csm.kitchenguard.data.local.entity.enums.SyncStatus]
     */
    @Query("UPDATE waste_records SET sync_status = :status WHERE client_uuid = :clientUuid")
    suspend fun updateSyncStatus(clientUuid: String, status: String)

    /**
     * Stream total kuantitas waste (SUM) untuk shift tertentu.
     * Digunakan di Hub Screen: "Total Waste Shift Ini".
     * Nullable karena SUM mengembalikan null jika belum ada record sama sekali.
     *
     * Record berstatus CONFLICT dikecualikan — belum divalidasi server,
     * tidak boleh masuk total display agar tidak misleading.
     */
    /**
     * Stream semua waste record yang mengalami konflik dengan server (Shift Locked).
     * Digunakan oleh Hub Screen untuk menampilkan ConflictWarningBanner.
     * Record ini TIDAK boleh dihapus sampai Head Chef menyelesaikan konflik.
     */
    @Query("SELECT * FROM waste_records WHERE sync_status = 'CONFLICT' ORDER BY client_event_at DESC")
    fun getConflictedWaste(): Flow<List<WasteRecordEntity>>

    /**
     * Reset status CONFLICT kembali ke PENDING agar WorkManager bisa mencoba ulang.
     * Dipanggil saat user menekan tombol "Kirim Ulang" di ConflictWarningBanner.
     *
     * WAJIB dipanggil bersama [SyncQueueDao.resetConflictToPending] — mereset tabel
     * ini saja TIDAK cukup karena antrean (`sync_queue`) tetap berstatus CONFLICT
     * sehingga tidak akan diambil oleh batch berikutnya (Issue #11).
     *
     * @return jumlah baris yang di-reset.
     */
    @Query("UPDATE waste_records SET sync_status = 'PENDING' WHERE sync_status = 'CONFLICT'")
    suspend fun resetConflictToPending(): Int

    /**
     * Stream total kuantitas waste (SUM) untuk shift tertentu.
     * Digunakan di Hub Screen: "Total Waste Shift Ini".
     * Record berstatus CONFLICT dikecualikan.
     */
    @Query(
        """
        SELECT SUM(quantity) FROM waste_records
        WHERE shift_id = :shiftId
        AND sync_status != 'CONFLICT'
        """
    )
    fun getTotalWasteQuantity(shiftId: Long): Flow<Double?>
}
