package com.csm.kitchenguard.di

import android.app.Application
import androidx.datastore.core.DataStore
import com.csm.kitchenguard.BuildConfig
import com.csm.kitchenguard.data.local.database.AppDatabase
import com.csm.kitchenguard.data.local.dao.*
import com.csm.kitchenguard.data.local.preferences.SessionManager
import com.csm.kitchenguard.data.remote.api.KitchenGuardApiService
import com.csm.kitchenguard.data.repository.AuthRepositoryImpl
import com.csm.kitchenguard.data.repository.StockAuditRepositoryImpl
import com.csm.kitchenguard.data.repository.SyncRepositoryImpl
import com.csm.kitchenguard.data.repository.WasteRepositoryImpl
import com.csm.kitchenguard.domain.repository.AuthRepository
import com.csm.kitchenguard.domain.repository.StockAuditRepository
import com.csm.kitchenguard.domain.repository.SyncRepository
import com.csm.kitchenguard.domain.repository.WasteRepository
import com.csm.kitchenguard.domain.usecase.CreateWasteRecordUseCase
import com.csm.kitchenguard.domain.usecase.GetStationSummaryUseCase
import com.csm.kitchenguard.domain.usecase.ValidateOcrWeightUseCase
import com.csm.kitchenguard.presentation.screens.auth.AuthViewModel
import com.csm.kitchenguard.presentation.screens.audit.StockAuditViewModel
import com.csm.kitchenguard.presentation.screens.hub.KitchenHubViewModel
import com.csm.kitchenguard.presentation.screens.notification.NotificationViewModel
import com.csm.kitchenguard.presentation.screens.profile.ProfileViewModel
import com.csm.kitchenguard.presentation.screens.waste.AiVerificationViewModel
import com.csm.kitchenguard.presentation.screens.waste.WasteLoggingViewModel
import com.csm.kitchenguard.utils.ai.FreshnessClassifier
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Dependency Injection Container untuk seluruh aplikasi KitchenGuard.
 * Singleton pattern untuk memastikan semua dependencies adalah shared instances.
 */
class AppModule private constructor(
    private val application: Application
) {
    // ── Database & DAOs ────────────────────────────────────────────────────────
    
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(application)
    }
    
    val wasteDao: WasteDao by lazy { database.wasteDao() }
    val syncQueueDao: SyncQueueDao by lazy { database.syncQueueDao() }
    val shiftDao: ShiftDao by lazy { database.shiftDao() }
    val masterDataDao: MasterDataDao by lazy { database.masterDataDao() }
    val stockAuditDao: StockAuditDao by lazy { database.stockAuditDao() }
    val notificationDao: NotificationDao by lazy { database.notificationDao() }
    
    // ── Remote API ─────────────────────────────────────────────────────────────
    
    val gson: Gson by lazy {
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .create()
    }
    
    // Logger HTTP sesuai build type + redaksi header Authorization (Issue #16).
    val httpLogger = com.csm.kitchenguard.data.remote.api.NetworkLoggingFactory.create(
        isDebug = BuildConfig.DEBUG
    )
    
    val httpClient = OkHttpClient.Builder()
        .addInterceptor(httpLogger)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    val apiService: KitchenGuardApiService by lazy {
        retrofit.create(KitchenGuardApiService::class.java)
    }
    
    // ── Local Preferences ──────────────────────────────────────────────────────
    
    val sessionManager: SessionManager by lazy {
        SessionManager(application)
    }
    
    // ── Repositories ───────────────────────────────────────────────────────────
    
    val wasteRepository: WasteRepository by lazy {
        WasteRepositoryImpl(wasteDao, syncQueueDao, gson)
    }
    
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(apiService, sessionManager, shiftDao, syncQueueDao)
    }
    
    val stockAuditRepository: StockAuditRepository by lazy {
        StockAuditRepositoryImpl(stockAuditDao, apiService)
    }
    
    val syncRepository: SyncRepository by lazy {
        SyncRepositoryImpl(apiService, syncQueueDao, wasteDao, masterDataDao, shiftDao, notificationDao)
    }
    
    // ── Use Cases ──────────────────────────────────────────────────────────────
    
    val getStationSummaryUseCase: GetStationSummaryUseCase by lazy {
        GetStationSummaryUseCase(shiftDao, wasteRepository, syncQueueDao)
    }
    
    val validateOcrWeightUseCase: ValidateOcrWeightUseCase by lazy {
        ValidateOcrWeightUseCase()
    }
    
    val createWasteRecordUseCase: CreateWasteRecordUseCase by lazy {
        CreateWasteRecordUseCase(wasteRepository, masterDataDao)
    }
    
    // ── ViewModels ─────────────────────────────────────────────────────────────
    
    val authViewModel: AuthViewModel by lazy {
        AuthViewModel(authRepository)
    }
    
    val kitchenHubViewModel: KitchenHubViewModel by lazy {
        KitchenHubViewModel(application, getStationSummaryUseCase, wasteRepository, wasteDao, syncRepository)
    }
    
    val wasteLoggingViewModel: WasteLoggingViewModel by lazy {
        WasteLoggingViewModel(
            application,
            masterDataDao,
            shiftDao,
            validateOcrWeightUseCase,
            createWasteRecordUseCase
        )
    }
    
    val stockAuditViewModel: StockAuditViewModel by lazy {
        StockAuditViewModel(shiftDao, stockAuditRepository)
    }

    /** Classifier TFLite on-device (dipakai AI Verification). Model .tflite opsional. */
    val freshnessClassifier: FreshnessClassifier by lazy {
        FreshnessClassifier(application)
    }

    val aiVerificationViewModel: AiVerificationViewModel by lazy {
        AiVerificationViewModel(application, freshnessClassifier)
    }

    val profileViewModel: ProfileViewModel by lazy {
        ProfileViewModel(application, sessionManager)
    }

    val notificationViewModel: NotificationViewModel by lazy {
        NotificationViewModel(syncRepository)
    }
    
    // ── Companion Object ───────────────────────────────────────────────────────
    
    companion object {
        @Volatile
        private var INSTANCE: AppModule? = null
        
        fun getInstance(application: Application): AppModule {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppModule(application).also { INSTANCE = it }
            }
        }
    }
}
