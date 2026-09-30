package com.csm.kitchenguard.data.remote.api

import com.csm.kitchenguard.data.remote.dto.LoginRequest
import com.csm.kitchenguard.data.remote.dto.LoginResponse
import com.csm.kitchenguard.data.remote.dto.StockAuditRequest
import com.csm.kitchenguard.data.remote.dto.StockAuditResponse
import com.csm.kitchenguard.data.remote.dto.SyncBatchRequest
import com.csm.kitchenguard.data.remote.dto.SyncBatchResponse
import com.csm.kitchenguard.data.remote.dto.SyncPullRequest
import com.csm.kitchenguard.data.remote.dto.SyncPullResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interface Retrofit untuk komunikasi dengan backend KitchenGuard CSM.
 * Semua function menggunakan `suspend` agar otomatis berjalan di background thread (I/O).
 */
interface KitchenGuardApiService {

    /**
     * Endpoint login untuk mendapatkan JWT Token dan konteks profil & stasiun.
     * PRD Section 9.
     */
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Endpoint untuk mengirim transaksi lokal (waste) ke server secara bulk.
     * Menggunakan konsep idempotency di server (Redis) untuk mencegah duplikasi.
     * PRD Section 21.
     */
    @POST("api/v1/sync/batch")
    suspend fun pushSyncBatch(@Body request: SyncBatchRequest): Response<SyncBatchResponse>

    /**
     * Endpoint untuk menarik data master terbaru (ingredients, batches, shift, notifikasi).
     * PRD Section 22.
     */
    @POST("api/v1/sync/pull")
    suspend fun pullSync(@Body request: SyncPullRequest): Response<SyncPullResponse>

    /**
     * Endpoint untuk mengirim hasil perhitungan fisik stok akhir shift.
     * PRD Section 24.
     */
    @POST("api/v1/stock-audits")
    suspend fun submitStockAudit(@Body request: StockAuditRequest): Response<StockAuditResponse>

}
