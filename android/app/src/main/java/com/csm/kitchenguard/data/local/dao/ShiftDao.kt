package com.csm.kitchenguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.csm.kitchenguard.data.local.entity.ActiveShiftEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk operasi tabel `active_shift`.
 * PRD Section 10 — Active Shift Context.
 *
 * Tabel ini dirancang untuk menyimpan SATU record aktif.
 * Shift dikontrol sepenuhnya oleh server (PostgreSQL/Redis).
 * Client hanya membaca dan mengikat transaksi ke [ActiveShiftEntity.shiftId].
 */
@Dao
interface ShiftDao {

    /**
     * Insert atau perbarui shift aktif.
     * Menggunakan REPLACE karena [ActiveShiftEntity.shiftId] adalah PrimaryKey —
     * jika shift baru diterima dari server, data lama langsung diganti.
     * Dipanggil setelah pull sync berhasil mendapatkan data shift dari server.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateActiveShift(shift: ActiveShiftEntity)

    /**
     * Stream shift aktif saat ini untuk observasi reaktif di UI.
     * Emit null jika tidak ada shift aktif (setelah logout atau shift ditutup).
     * Digunakan di Hub Screen untuk menampilkan status shift secara real-time.
     *
     * LIMIT 1 karena tabel hanya boleh memiliki satu record aktif.
     */
    @Query("SELECT * FROM active_shift LIMIT 1")
    fun getActiveShift(): Flow<ActiveShiftEntity?>

    /**
     * One-shot fetch shift aktif tanpa observasi.
     * Digunakan oleh WorkManager dan Use Case yang hanya perlu nilai
     * saat ini tanpa perlu di-observe secara reaktif.
     * Contoh: validasi shiftId sebelum enqueue sync item.
     */
    @Query("SELECT * FROM active_shift LIMIT 1")
    suspend fun getActiveShiftOnce(): ActiveShiftEntity?

    /**
     * Hapus semua data shift aktif.
     * Dipanggil saat:
     * - Pengguna logout
     * - Shift ditutup (tutup shift + audit fisik selesai)
     * - Conflict resolution: shift di server sudah tidak valid
     */
    @Query("DELETE FROM active_shift")
    suspend fun clearActiveShift()
}
