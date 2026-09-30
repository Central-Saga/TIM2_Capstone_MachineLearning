package com.csm.kitchenguard.data.remote.api

import android.util.Log
import com.csm.kitchenguard.BuildConfig
import com.csm.kitchenguard.data.local.preferences.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Konfigurasi singleton untuk Retrofit client.
 * Memuat URL dasar dari BuildConfig (per build type), logger, dan interceptor autentikasi.
 * 
 * SECURITY: Uses HTTPS in production, HTTP only for debug builds (localhost/emulator)
 */
object NetworkClient {

    /**
     * URL Endpoint Backend - Loaded from BuildConfig per build type.
     * Debug: HTTP for localhost (10.0.2.2:8080) for development testing
     * Release: HTTPS production API (https://api.kitchenguard.com/)
     */
    private const val BASE_URL = BuildConfig.BASE_URL
    
    init {
        Log.d("NetworkClient", "API Base URL configured: $BASE_URL")
    }

    fun createService(sessionManager: SessionManager): KitchenGuardApiService {

        // Logger HTTP sesuai build type (Issue #16).
        // - Debug  → Level.BODY untuk memudahkan debugging payload.
        // - Release→ Level.NONE; body login (password) TIDAK boleh tercetak.
        // - Header Authorization selalu di-redact pada kedua build type.
        val loggingInterceptor = NetworkLoggingFactory.create(isDebug = BuildConfig.DEBUG)

        // Interceptor dinamis untuk mengambil token dari DataStore
        val authInterceptor = Interceptor { chain ->
            /*
             * Karena interceptor OkHttp berjalan secara sinkron di background thread
             * dan DataStore adalah asinkron (Flow), kita menggunakan runBlocking
             * untuk menahan thread I/O ini sementara sampai token terbaca.
             * Ini aman karena Retrofit mengeksekusi request di luar Main Thread.
             */
            val token = runBlocking {
                sessionManager.getAuthToken().first()
            }

            val requestBuilder = chain.request().newBuilder()

            // Jika token ada, tambahkan header Authorization
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            // Lanjutkan eksekusi network request
            chain.proceed(requestBuilder.build())
        }

        // Konfigurasi HTTP Client dengan timeout 30 detik untuk menangani jaringan lambat dapur
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        // Builder utama Retrofit dengan Gson converter
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(KitchenGuardApiService::class.java)
    }
}
