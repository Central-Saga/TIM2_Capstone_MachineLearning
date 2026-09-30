package com.csm.kitchenguard.data.local.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Utilitas untuk mengelola WorkManager sync.
 * Menyediakan kapabilitas Periodic (rutin) dan Immediate (One-Time) Sync.
 */
object SyncScheduler {

    private const val SYNC_PERIODIC_WORK_NAME = "KITCHENGUARD_PERIODIC_SYNC"
    private const val SYNC_IMMEDIATE_WORK_NAME = "KITCHENGUARD_IMMEDIATE_SYNC"

    /**
     * Memulai tugas periodik setiap 15 menit.
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Memicu sinkronisasi instan sesegera mungkin jika terkoneksi internet.
     */
    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Timpa jika ada antrean immediate sebelumnya yang belum jalan
            syncRequest
        )
    }
}
