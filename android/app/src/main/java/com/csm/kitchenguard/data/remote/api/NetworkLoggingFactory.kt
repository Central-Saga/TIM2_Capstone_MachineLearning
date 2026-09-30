package com.csm.kitchenguard.data.remote.api

import okhttp3.logging.HttpLoggingInterceptor

/**
 * Factory terpusat untuk [HttpLoggingInterceptor] (Issue #16).
 *
 * KEBIJAKAN KEAMANAN:
 * - Body request/response (Level.BODY) HANYA di build debug. Body login
 *   mengandung password, sehingga TIDAK boleh tercetak di release.
 * - Build release memakai Level.NONE.
 * - Header `Authorization` (Bearer token) SELALU di-redact pada level manapun,
 *   agar token tidak bocor ke Logcat.
 *
 * `isDebug` diterima sebagai parameter eksplisit (bukan membaca BuildConfig
 * langsung) agar mudah diuji.
 */
object NetworkLoggingFactory {

    /** Nama header sensitif yang nilainya harus disamarkan di log. */
    val SENSITIVE_HEADERS = listOf("Authorization")

    /**
     * Membuat interceptor logging sesuai environment.
     *
     * @param isDebug true pada build debug (BODY), false pada release (NONE).
     * @param logger  opsional logger kustom (dipakai test); default ke Logcat.
     */
    fun create(
        isDebug: Boolean,
        logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT
    ): HttpLoggingInterceptor = HttpLoggingInterceptor(logger).apply {
        level = if (isDebug) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        // Selalu samarkan header sensitif, apa pun level-nya.
        SENSITIVE_HEADERS.forEach { header -> redactHeader(header) }
    }
}
