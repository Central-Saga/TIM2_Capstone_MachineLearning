package com.csm.kitchenguard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity Room untuk profil pengguna yang ter-cache secara lokal.
 * PRD Section 18 ?" Offline Local Database (tabel: cached_user).
 * 
 * SECURITY UPDATE:
 * - Token JWT TIDAK lagi disimpan di sini (pindah ke EncryptedSharedPreferences)
 * - Hanya data profil non-sensitive yang disimpan untuk offline access
 */
@Entity(tableName = "cached_user")
data class UserEntity(

    /** ID pengguna dari server (bukan autoGenerate). */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,

    /** Employee ID (bisa berupa email atau nomor karyawan). */
    @ColumnInfo(name = "employee_id")
    val employeeId: String,

    /** Nama lengkap pengguna. */
    @ColumnInfo(name = "name")
    val name: String,

    /** Role pengguna: KITCHEN_STAFF, BARISTA, INVENTORY_CHECKER, dll. */
    @ColumnInfo(name = "role")
    val role: String,

    /** Nama / kode stasiun yang ditugaskan ke pengguna ini. */
    @ColumnInfo(name = "station_assignment")
    val stationAssignment: String,

    // ⚠️ TOKEN DIHAPUS - Pindah ke EncryptedSharedPreferences
    // TODO (Tahap 08): DONE - Token sekarang disimpan di Android Keystore
    
    /** Timestamp (epoch millis) saat data ini di-cache terakhir kali. */
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long
)
