package com.csm.kitchenguard.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

// Ekstensi untuk menginisialisasi Singleton DataStore (untuk non-sensitive data)
private val Context.nonSensitiveDataStore: DataStore<Preferences> by preferencesDataStore(name = "kitchenguard_non_sensitive")

/**
 * Pengelola sesi lokal dengan ENCRYPTED SharedPreferences untuk token autentikasi.
 * PRD Section 9 — Authentication.
 *
 * SECURITY IMPLEMENTATION:
 * - Token JWT disimpan di EncryptedSharedPreferences menggunakan MasterKey (Android Keystore)
 * - Non-sensitive session data tetap di DataStore untuk performance
 * - Token tidak boleh disimpan di Room Database (dihapus dari UserEntity.token)
 */
class SessionManager(private val context: Context) {

    // ── Master Key untuk EncryptedSharedPreferences ────────────────────────────
    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // ── EncryptedSharedPreferences untuk TOKEN (HANYA token!) ─────────────────
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "kitchenguard_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Kunci Preferensi Sensitif (TOKEN) ─────────────────────────────────────
    private val KEY_AUTH_TOKEN_SECURE = "AUTH_TOKEN_SECURE"

    // ── Kunci Preferensi Non-Sensitive (DataStore) ────────────────────────────
    private companion object {
        val KEY_EMPLOYEE_ID = stringPreferencesKey("EMPLOYEE_ID")
        val KEY_USER_NAME = stringPreferencesKey("USER_NAME")
        val KEY_USER_ROLE = stringPreferencesKey("USER_ROLE")
        val KEY_STATION_ID = longPreferencesKey("ASSIGNED_STATION_ID")
        val KEY_STATION_NAME = stringPreferencesKey("ASSIGNED_STATION_NAME")
        val KEY_ACTIVE_SHIFT_ID = longPreferencesKey("ACTIVE_SHIFT_ID")
    }

    // ── Setter ────────────────────────────────────────────────────────────────

    /** Menyimpan JWT Bearer token ke ENCRYPTED SharedPreferences. */
    suspend fun saveAuthToken(token: String) {
        // ENCRYPTED penyimpanan token di Android Keystore (AES-256-GCM)
        encryptedPrefs.edit {
            putString(KEY_AUTH_TOKEN_SECURE, token)
        }
    }

    /**
     * Menyimpan profil sesi pengguna setelah login berhasil.
     * Tidak menyimpan shift ID karena login baru biasanya belum memiliki shift aktif.
     */
    suspend fun saveUserSession(
        employeeId: String,
        name: String,
        role: String,
        stationId: Long,
        stationName: String
    ) {
        context.nonSensitiveDataStore.edit { preferences ->
            preferences[KEY_EMPLOYEE_ID] = employeeId
            preferences[KEY_USER_NAME] = name
            preferences[KEY_USER_ROLE] = role
            preferences[KEY_STATION_ID] = stationId
            preferences[KEY_STATION_NAME] = stationName
        }
    }

    /** Memperbarui status ID shift yang sedang aktif (bisa dipanggil saat Clock-In/Out). */
    suspend fun saveActiveShift(shiftId: Long) {
        context.nonSensitiveDataStore.edit { preferences ->
            preferences[KEY_ACTIVE_SHIFT_ID] = shiftId
        }
    }

    /** Menghapus seluruh data sesi secara permanen saat pengguna logout. */
    suspend fun clearSession() {
        // Clear ENCRYPTED token dari Android Keystore
        encryptedPrefs.edit {
            clear()
        }

        // Clear non-sensitive data
        context.nonSensitiveDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // ── Getter / Observer ─────────────────────────────────────────────────────

    /**
     * Mengembalikan Flow token autentikasi terenkripsi yang sedang aktif.
     * Returns null jika user belum login.
     */
    fun getAuthToken(): Flow<String?> {
        return flow {
            emit(encryptedPrefs.getString(KEY_AUTH_TOKEN_SECURE, null))
        }
    }

    /**
     * Mendapatkan token secara synchronus (GUNAKAN DENGAN HATI-HATI).
     * @return Token atau null jika belum ada.
     */
    fun getTokenSync(): String? {
        return encryptedPrefs.getString(KEY_AUTH_TOKEN_SECURE, null)
    }

    /**
     * Mengembalikan aliran data model profil sesi pengguna.
     * Akan mereturn null jika ada field krusial yang kosong (asumsi belum login / sesi dihapus).
     */
    fun getUserSession(): Flow<UserSessionModel?> {
        return context.nonSensitiveDataStore.data.map { preferences ->
            val employeeId = preferences[KEY_EMPLOYEE_ID]
            val name = preferences[KEY_USER_NAME]
            val role = preferences[KEY_USER_ROLE]
            val stationId = preferences[KEY_STATION_ID]
            val stationName = preferences[KEY_STATION_NAME]

            if (employeeId == null || name == null || role == null ||
                stationId == null || stationName == null) {
                null
            } else {
                UserSessionModel(
                    employeeId = employeeId,
                    name = name,
                    role = role,
                    stationId = stationId,
                    stationName = stationName,
                    activeShiftId = preferences[KEY_ACTIVE_SHIFT_ID]
                )
            }
        }
    }
}
