package com.csm.kitchenguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.csm.kitchenguard.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk tabel `notifications_cache`.
 * PRD Section 18 — Offline Local Database.
 * PRD Section P1 — Notification Center.
 *
 * Notifikasi di-pull dari server via POST /api/v1/sync/pull dan di-cache di sini
 * agar Notification Center dapat menampilkan riwayat secara offline.
 */
@Dao
interface NotificationDao {

    /**
     * Bulk upsert notifikasi hasil pull sync.
     * Menggunakan REPLACE agar data server menimpa cache lama berdasarkan `id`.
     *
     * @param items daftar notifikasi terbaru dari server
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(items: List<NotificationEntity>)

    /**
     * Stream seluruh notifikasi ter-cache, terbaru lebih dulu.
     * Dibandingkan berdasarkan [NotificationEntity.createdAt] (ISO-8601,
     * sehingga urutan leksikografis = kronologis untuk format UTC konsisten).
     */
    @Query("SELECT * FROM notifications_cache ORDER BY created_at DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    /** Stream hanya notifikasi yang belum dibaca (untuk badge/unread counter). */
    @Query("SELECT * FROM notifications_cache WHERE is_read = 0 ORDER BY created_at DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>

    /** Jumlah notifikasi belum dibaca (one-shot) untuk badge. */
    @Query("SELECT COUNT(*) FROM notifications_cache WHERE is_read = 0")
    suspend fun getUnreadCount(): Int

    /** Tandai satu notifikasi sebagai sudah dibaca. */
    @Query("UPDATE notifications_cache SET is_read = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    /** Tandai semua notifikasi sebagai sudah dibaca. */
    @Query("UPDATE notifications_cache SET is_read = 1")
    suspend fun markAllAsRead()

    /** Hapus seluruh cache notifikasi (mis. saat logout). */
    @Query("DELETE FROM notifications_cache")
    suspend fun clearAll()
}
