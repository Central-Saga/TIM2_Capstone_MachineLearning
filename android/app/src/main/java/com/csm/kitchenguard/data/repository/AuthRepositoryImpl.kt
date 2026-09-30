package com.csm.kitchenguard.data.repository

import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.preferences.SessionManager
import com.csm.kitchenguard.data.local.preferences.UserSessionModel
import com.csm.kitchenguard.data.remote.api.KitchenGuardApiService
import com.csm.kitchenguard.data.remote.dto.LoginRequest
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
            emit(Resource.Error("Koneksi bermasalah: ${e.localizedMessage}"))
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
