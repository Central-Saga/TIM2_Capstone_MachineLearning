package com.csm.kitchenguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk operasi tabel `draft_stock_audit`.
 * PRD Section 24 — Physical Stock Audit.
 *
 * Inventory Checker menginput penghitungan fisik yang disimpan sebagai draft
 * sebelum dikonfirmasi dan dikirim ke POST /api/v1/stock-audits.
 * Kalkulasi variance dilakukan sepenuhnya di server (PostgreSQL).
 */
@Dao
interface StockAuditDao {

    /**
     * Insert atau perbarui draft audit fisik.
     * Menggunakan REPLACE agar Inventory Checker dapat memperbarui hitungan
     * sebelum menekan tombol submit akhir.
     *
     * PrimaryKey adalah auto-generated [StockAuditDraftEntity.id].
     * Untuk update record yang sudah ada, pastikan [id] disertakan
     * (bukan 0) agar REPLACE menemukan row yang tepat.
     *
     * @param draft Data draft penghitungan fisik
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDraft(draft: StockAuditDraftEntity)

    /**
     * Stream semua draft audit untuk shift tertentu.
     * Diurutkan berdasarkan [StockAuditDraftEntity.ingredientId] ASC
     * untuk konsistensi urutan tampilan di Audit Form Screen.
     * Flow otomatis emit ulang saat draft ditambah atau diubah.
     *
     * @param shiftId ID shift yang sedang diaudit
     */
    @Query(
        """
        SELECT * FROM draft_stock_audit
        WHERE shift_id = :shiftId
        ORDER BY ingredient_id ASC
        """
    )
    fun getDraftsByShift(shiftId: Long): Flow<List<StockAuditDraftEntity>>

    /**
     * Hapus draft yang sudah berhasil dikirim ke server dari tabel lokal.
     * Hanya menghapus record dengan [StockAuditDraftEntity.isSubmitted] = true —
     * draft yang belum dikirim (is_submitted = 0) TIDAK terhapus untuk keamanan data.
     *
     * Dipanggil setelah server mengonfirmasi penerimaan audit via POST /api/v1/stock-audits.
     *
     * PENTING (Issue #13): metode ini WAJIB didahului [markSubmitted], karena
     * [isSubmitted] default-nya false dan tidak akan terhapus tanpa ditandai.
     *
     * @param shiftId ID shift yang draft-nya akan dibersihkan
     */
    @Query("DELETE FROM draft_stock_audit WHERE shift_id = :shiftId AND is_submitted = 1")
    suspend fun clearSubmittedDrafts(shiftId: Long)

    /**
     * Menandai seluruh draft pada shift tertentu sebagai sudah terkirim
     * (`is_submitted = 1`).
     *
     * Dipanggil SETELAH server mengonfirmasi sukses submit audit, lalu diikuti
     * [clearSubmittedDrafts] untuk membersihkannya (Issue #13).
     *
     * @return jumlah baris yang ditandai.
     */
    @Query("UPDATE draft_stock_audit SET is_submitted = 1 WHERE shift_id = :shiftId")
    suspend fun markSubmitted(shiftId: Long): Int
}
