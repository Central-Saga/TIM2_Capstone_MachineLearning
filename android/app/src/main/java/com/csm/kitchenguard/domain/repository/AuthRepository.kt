package com.csm.kitchenguard.domain.repository

import com.csm.kitchenguard.data.local.preferences.UserSessionModel
import com.csm.kitchenguard.utils.result.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak domain untuk proses Autentikasi.
 * PRD Section 9.
 */
interface AuthRepository {
    
    /**
     * Melakukan login ke backend menggunakan identifier (email/NIP) dan password.
     * Mengembalikan stream status (Loading -> Success/Error).
     * Jika sukses, token dan data sesi disimpan otomatis ke SessionManager.
     */
    fun login(identifier: String, password: String): Flow<Resource<UserSessionModel>>

    /**
     * Menghapus sesi lokal secara permanen.
     */
    suspend fun logout()
}
