package com.csm.kitchenguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.csm.kitchenguard.data.local.entity.BatchEntity
import com.csm.kitchenguard.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk operasi cache master data: tabel `ingredients` dan `batches`.
 * PRD Section 13 — Unit Conversion (ingredients canonical unit).
 * PRD Section 14 — Batch & Expiry Context.
 * PRD Section 22 — Pull Sync: POST /api/v1/sync/pull.
 *
 * Data ini bersifat read-only di client — hanya diperbarui via pull sync
 * dari server (PostgreSQL). Tidak ada insert manual oleh pengguna.
 */
@Dao
interface MasterDataDao {

    // ── Ingredients ──────────────────────────────────────────────────────────

    /**
     * Bulk upsert daftar bahan makanan dari hasil pull sync.
     * Menggunakan REPLACE agar data server selalu menimpa cache lokal.
     * Dipanggil oleh WorkManager setelah POST /api/v1/sync/pull berhasil.
     *
     * @param items Daftar ingredient terbaru dari server
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(items: List<IngredientEntity>)

    /**
     * Stream semua bahan makanan, diurutkan berdasarkan kategori lalu nama.
     * Digunakan untuk dropdown pemilihan bahan di form Waste Logging.
     * Flow otomatis emit ulang saat cache diperbarui via pull sync.
     */
    @Query("SELECT * FROM ingredients ORDER BY category ASC, name ASC")
    fun getAllIngredients(): Flow<List<IngredientEntity>>

    /**
     * One-shot fetch bahan berdasarkan ID.
     * Digunakan untuk denormalisasi nama bahan ke [WasteRecordEntity.ingredientName]
     * saat waste record dibuat — agar nama tampil offline tanpa JOIN.
     *
     * @param id ID bahan dari server
     * @return Entity jika ditemukan, null jika belum ada di cache
     */
    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getIngredientById(id: Long): IngredientEntity?

    // ── Batches ───────────────────────────────────────────────────────────────

    /**
     * Bulk upsert daftar batch dari hasil pull sync.
     * Menggunakan REPLACE agar status batch (ACTIVE, NEAR_EXPIRY, EXPIRED, DEPLETED)
     * selalu sinkron dengan data server terbaru.
     *
     * @param batches Daftar batch terbaru dari server
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<BatchEntity>)

    /**
     * Stream batch yang relevan untuk bahan tertentu.
     * Hanya mengembalikan batch berstatus ACTIVE dan NEAR_EXPIRY —
     * EXPIRED dan DEPLETED tidak ditampilkan untuk input waste baru.
     *
     * Diurutkan berdasarkan [BatchEntity.expiryAt] ASC (FEFO — First Expired First Out)
     * agar batch yang paling mendekati kedaluwarsa muncul sebagai pilihan utama.
     * Digunakan di form pemilihan batch pada Waste Logging flow.
     *
     * @param ingredientId ID bahan yang batch-nya ingin diambil
     */
    @Query(
        """
        SELECT * FROM batches
        WHERE ingredient_id = :ingredientId
        AND status IN ('ACTIVE', 'NEAR_EXPIRY')
        ORDER BY expiry_at ASC
        """
    )
    fun getBatchesByIngredient(ingredientId: Long): Flow<List<BatchEntity>>

    /**
     * One-shot fetch batch AKTIF (ACTIVE/NEAR_EXPIRY) untuk sebuah bahan.
     *
     * Digunakan oleh validasi bisnis (PRD Section 14): alasan waste SPOILED/EXPIRED
     * mewajibkan pemilihan `batch_id` bila bahan memiliki batch aktif.
     *
     * @return daftar batch aktif; kosong bila bahan tidak punya batch aktif.
     */
    @Query(
        """
        SELECT * FROM batches
        WHERE ingredient_id = :ingredientId
        AND status IN ('ACTIVE', 'NEAR_EXPIRY')
        """
    )
    suspend fun getActiveBatchesOnce(ingredientId: Long): List<BatchEntity>
}
