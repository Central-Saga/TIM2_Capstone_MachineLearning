package com.csm.kitchenguard.data.local.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.csm.kitchenguard.data.local.database.AppDatabase
import com.csm.kitchenguard.data.local.preferences.SessionManager
import com.csm.kitchenguard.data.remote.api.NetworkClient
import com.csm.kitchenguard.data.repository.SyncRepositoryImpl

/**
 * Worker di background yang berjalan untuk menangani 
 * antrean sinkronisasi (Push) dan pembaruan data master (Pull).
 * Terletak di modul lokal karena bertindak layaknya jembatan sinkronisasi data offline.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val sessionManager = SessionManager(applicationContext)
            val apiService = NetworkClient.createService(sessionManager)

            val syncRepository = SyncRepositoryImpl(
                apiService = apiService,
                syncQueueDao = database.syncQueueDao(),
                wasteDao = database.wasteDao(),
                masterDataDao = database.masterDataDao(),
                shiftDao = database.shiftDao(),
                notificationDao = database.notificationDao()
            )

            // Push Batch Sync
            val pushResult = syncRepository.pushBatchSync()

            // Pull Master Data
            val pullResult = syncRepository.pullMasterData()

            // Retry HANYA untuk kegagalan yang bersifat sementara.
            // pushBatchSync() sengaja mengembalikan Result.success untuk kondisi
            // terminal (422/401/403 → FAILED_VALIDATION, 409 → CONFLICT), sehingga
            // item tersebut TIDAK di-retry tanpa batas (Issue #12).
            // Result.failure hanya dipakai untuk error retryable (jaringan / 5xx).
            if (pushResult.isFailure || pullResult.isFailure) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            // Exception tak terduga (IO/network) → retryable.
            Result.retry()
        }
    }
}
