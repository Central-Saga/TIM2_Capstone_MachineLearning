package com.csm.kitchenguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk operasi tabel `sync_queue`.
 * PRD Section 19 — Sync State & Architecture.
 * PRD Section 20 — Idempotency & Peran Redis.
 * PRD Section 21 — Push Sync Batch: POST /api/v1/sync/batch.
 *
 * WorkManager membaca DAO ini untuk mengirim batch transaksi ke server.
 */
@Dao
interface SyncQueueDao {

    /**
     * Tambahkan item baru ke antrean sinkronisasi.
     * Menggunakan IGNORE — jika [SyncQueueEntity.clientUuid] sudah ada di queue,
     * entry yang sedang SYNCING TIDAK di-overwrite (idempotent enqueue).
     *
     * @return rowId auto-generated, atau -1 jika entry sudah ada (IGNORE)
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    /**
     * Stream seluruh item dengan status PENDING di antrean, diurutkan FIFO.
     * Digunakan untuk menampilkan "Pending Sync Counter" di Hub Screen.
     * Flow otomatis emit ulang saat antrean berubah.
     */
    @Query("SELECT * FROM sync_queue WHERE sync_status = 'PENDING' ORDER BY created_at ASC")
    fun getPendingQueue(): Flow<List<SyncQueueEntity>>

    /**
     * Ambil batch berikutnya untuk dikirim ke server via WorkManager.
     *
     * HANYA mengambil status yang layak di-retry:
     * - `PENDING` — belum pernah dicoba.
     * - `FAILED`  — gagal jaringan/5xx (retryable, dengan exponential backoff
     *   berbasis [SyncQueueEntity.retryCount]).
     *
     * SECARA SENGAJA TIDAK MENGAMBIL (terminal, Issue #12):
     * - `FAILED_VALIDATION` — payload ditolak validasi (422); retry tak akan berhasil.
     * - `CONFLICT` — shift lock; menunggu review manual.
     *
     * Diurutkan FIFO dengan limit untuk mencegah payload terlalu besar.
     *
     * @param limit Jumlah maksimum item per batch (default: 20 sesuai kapasitas API)
     */
    @Query(
        """
        SELECT * FROM sync_queue
        WHERE sync_status IN ('PENDING', 'FAILED')
        ORDER BY created_at ASC
        LIMIT :limit
        """
    )
    suspend fun getNextPendingBatch(limit: Int = 20): List<SyncQueueEntity>

    /**
     * Update status item setelah menerima response dari server.
     * Dipanggil untuk setiap item dalam batch response PRD Section 21:
     * SYNCED, ALREADY_PROCESSED → hapus via [deleteSynced]
     * CONFLICT → tandai CONFLICT (menunggu review manual)
     * FAILED_VALIDATION → tandai FAILED_VALIDATION (TERMINAL, tidak di-retry)
     * network error / 5xx → tandai FAILED, increment retryCount
     *
     * @param id         Primary key item
     * @param status     Nilai baru SyncStatus
     * @param error      Pesan error dari server (null jika sukses)
     * @param retryCount Jumlah retry terkini
     */
    @Query(
        """
        UPDATE sync_queue
        SET sync_status = :status,
            error_message = :error,
            retry_count = :retryCount
        WHERE id = :id
        """
    )
    suspend fun updateStatus(id: Long, status: String, error: String?, retryCount: Int)

    /**
     * Hapus item dari antrean setelah server mengonfirmasi SYNCED atau ALREADY_PROCESSED.
     * PRD Section 20: ALREADY_PROCESSED diperlakukan sebagai SYNCED.
     * Menjaga tabel sync_queue tetap ringan dan tidak terakumulasi.
     *
     * @param id Primary key item yang akan dihapus
     */
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteSynced(id: Long)

    /**
     * Cek apakah clientUuid tertentu sudah terdaftar di antrean.
     * Digunakan untuk mencegah double-enqueue saat transaksi yang sama
     * dicoba dimasukkan ulang ke queue.
     *
     * @param uuid Client UUID yang dicari
     * @return Entity jika ditemukan, null jika belum ada di queue
     */
    @Query("SELECT * FROM sync_queue WHERE client_uuid = :uuid LIMIT 1")
    suspend fun getByClientUuid(uuid: String): SyncQueueEntity?

    /**
     * Reset status CONFLICT kembali ke PENDING agar WorkManager mengambilnya lagi
     * pada [getNextPendingBatch] berikutnya.
     *
     * WAJIB dipanggil bersama reset pada tabel sumber (`waste_records`) saat user
     * menekan "Kirim Ulang Semua Konflik". Tanpa ini, item tetap CONFLICT dan
     * tidak akan pernah dikirim ulang (Issue #11).
     *
     * `error_message` juga dibersihkan agar tidak menampilkan pesan konflik lama.
     *
     * @return jumlah baris yang di-reset.
     */
    @Query(
        """
        UPDATE sync_queue
        SET sync_status = 'PENDING', error_message = NULL
        WHERE sync_status = 'CONFLICT'
        """
    )
    suspend fun resetConflictToPending(): Int
}
