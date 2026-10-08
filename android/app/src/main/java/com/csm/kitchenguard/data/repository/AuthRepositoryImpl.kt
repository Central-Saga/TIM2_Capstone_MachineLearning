package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.BuildConfig
import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.entity.ActiveShiftEntity
import com.csm.kitchenguard.data.local.preferences.SessionManager
import com.csm.kitchenguard.data.local.preferences.UserSessionModel
import com.csm.kitchenguard.data.remote.api.KitchenGuardApiService
import com.csm.kitchenguard.data.remote.dto.LoginRequest
import com.csm.kitchenguard.domain.model.UserRole
import com.csm.kitchenguard.domain.repository.AuthRepository
import com.csm.kitchenguard.utils.result.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AuthRepositoryImpl(
    private val apiService: KitchenGuardApiService,
    private val sessionManager: SessionManager,
    private val shiftDao: ShiftDao,
    private val syncQueueDao: SyncQueueDao
) : AuthRepository {

    override fun login(identifier: String, password: String): Flow<Resource<UserSessionModel>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.login(LoginRequest(identifier, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                // Simpan token ke DataStore
                sessionManager.saveAuthToken(body.token)
                
                // Simpan profil & station ke DataStore
                sessionManager.saveUserSession(
                    employeeId = body.user.employeeId,
                    name = body.user.name,
                    role = body.user.role,
                    stationId = body.station.id,
                    stationName = body.station.name
                )
                
                val sessionModel = UserSessionModel(
                    employeeId = body.user.employeeId,
                    name = body.user.name,
                    role = body.user.role,
                    stationId = body.station.id,
                    stationName = body.station.name,
                    activeShiftId = null // Shift ditentukan via Pull Sync nanti
                )
                emit(Resource.Success(sessionModel))
            } else {
                emit(Resource.Error("Login gagal: ${response.message()}"))
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                // Demo / Offline fallback untuk pengujian UI & emulator tanpa koneksi backend server 8080
                val demoToken = "demo_bearer_token_csm_kitchenguard"
                sessionManager.saveAuthToken(demoToken)

                val isDbAdmin = identifier.lowercase().contains("admin")
                val empId = if (isDbAdmin) "EMP-ADM-001" else "EMP-CHF-001"
                val name = if (isDbAdmin) "Admin Dapur CSM" else "Chef Head CSM"
                val role = UserRole.HEAD_CHEF.value
                val stationId = 1L
                val stationName = "Hot Kitchen Station"

                sessionManager.saveUserSession(
                    employeeId = empId,
                    name = name,
                    role = role,
                    stationId = stationId,
                    stationName = stationName
                )

                // Pastikan ada shift aktif lokal untuk pengujian offline
                if (shiftDao.getActiveShiftOnce() == null) {
                    shiftDao.insertOrUpdateActiveShift(
                        ActiveShiftEntity(
                            shiftId = 101L,
                            stationId = stationId,
                            stationName = stationName,
                            startedAt = "2026-01-01T08:00:00Z",
                            serverVersion = 1,
                            isLocked = false
                        )
                    )
                }

                val sessionModel = UserSessionModel(
                    employeeId = empId,
                    name = name,
                    role = role,
                    stationId = stationId,
                    stationName = stationName,
                    activeShiftId = 101L
                )
                emit(Resource.Success(sessionModel))
            } else {
                emit(Resource.Error("Koneksi bermasalah: ${e.localizedMessage}"))
            }
        }
    }

    override suspend fun logout() {
        // Bersihkan sesi lokal
        sessionManager.clearSession()
        
        // Bersihkan shift yang tersisa
        shiftDao.clearActiveShift()
        
        // Pilihan arsitektural: jika ada pending sync, biarkan saja tersimpan,
        // namun untuk alasan keamanan data di aplikasi sharing device, lebih
        // baik data queue juga diflush atau divalidasi ke depannya.
        // Untuk tahap ini, kita hanya membersihkan sesi pengguna aktif.
    }
}
