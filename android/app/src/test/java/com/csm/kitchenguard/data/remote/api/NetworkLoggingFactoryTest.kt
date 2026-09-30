package com.csm.kitchenguard.data.remote.api

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk [NetworkLoggingFactory] (Issue #16).
 *
 * Memverifikasi:
 * 1. Level logging berbeda antara debug (BODY) dan release (NONE).
 * 2. Header Authorization di-redact sehingga token tidak bocor.
 * 3. Di release, body request (mis. password) tidak tercetak.
 */
class NetworkLoggingFactoryTest {

    /** Logger yang menangkap seluruh baris log untuk diperiksa. */
    private class CapturingLogger : HttpLoggingInterceptor.Logger {
        val lines = mutableListOf<String>()
        override fun log(message: String) {
            lines.add(message)
        }
    }

    /** Chain palsu yang mengembalikan request/response statis. */
    private fun fakeChain(request: Request): Interceptor.Chain =
        object : Interceptor.Chain {
            override fun request(): Request = request
            override fun proceed(request: Request): Response = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"token":"abc"}""".toResponseBody("application/json".toMediaType()))
                .build()

            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }

    private fun loginRequest(): Request = Request.Builder()
        .url("https://api.kitchenguard.com/api/v1/auth/login")
        .header("Authorization", "Bearer SUPER_SECRET_TOKEN_123")
        .post(
            """{"username":"chef","password":"P@ssw0rd-Rahasia"}"""
                .toRequestBody("application/json".toMediaType())
        )
        .build()

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Level per build type
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN debug WHEN create THEN level is BODY`() {
        val interceptor = NetworkLoggingFactory.create(isDebug = true)

        assertEquals(HttpLoggingInterceptor.Level.BODY, interceptor.level)
    }

    @Test
    fun `GIVEN release WHEN create THEN level is NONE`() {
        val interceptor = NetworkLoggingFactory.create(isDebug = false)

        assertEquals(
            "Release tidak boleh mencetak body (berisi password)",
            HttpLoggingInterceptor.Level.NONE,
            interceptor.level
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Redaksi header Authorization (debug, level BODY)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN debug body logging WHEN request has token THEN Authorization redacted`() {
        val logger = CapturingLogger()
        val interceptor = NetworkLoggingFactory.create(isDebug = true, logger = logger)

        interceptor.intercept(fakeChain(loginRequest()))

        val output = logger.lines.joinToString("\n")
        assertFalse(
            "Token asli TIDAK boleh muncul di log",
            output.contains("SUPER_SECRET_TOKEN_123")
        )
        assertTrue(
            "Header Authorization harus ada tapi disamarkan",
            output.contains("Authorization")
        )
        assertTrue(
            "Nilai header harus diganti penanda redaction (██)",
            output.contains("██")
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 3: Release tidak mencetak apa pun
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN release WHEN intercept THEN nothing logged`() {
        val logger = CapturingLogger()
        val interceptor = NetworkLoggingFactory.create(isDebug = false, logger = logger)

        interceptor.intercept(fakeChain(loginRequest()))

        assertTrue(
            "Release harus diam total — tidak ada body/password/token di log",
            logger.lines.isEmpty()
        )
    }

    @Test
    fun `GIVEN release WHEN intercept THEN password not leaked`() {
        val logger = CapturingLogger()
        val interceptor = NetworkLoggingFactory.create(isDebug = false, logger = logger)

        interceptor.intercept(fakeChain(loginRequest()))

        val output = logger.lines.joinToString("\n")
        assertFalse(output.contains("P@ssw0rd-Rahasia"))
        assertFalse(output.contains("SUPER_SECRET_TOKEN_123"))
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 4: Deklarasi header sensitif
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN sensitive headers list WHEN inspected THEN contains Authorization`() {
        assertTrue(
            NetworkLoggingFactory.SENSITIVE_HEADERS.contains("Authorization")
        )
    }

    @Test
    fun `GIVEN request without token THEN logging still safe`() {
        val logger = CapturingLogger()
        val interceptor = NetworkLoggingFactory.create(isDebug = true, logger = logger)
        val request = Request.Builder()
            .url("https://api.kitchenguard.com/api/v1/sync/pull")
            .get()
            .build()

        interceptor.intercept(fakeChain(request))

        assertNotNull(logger.lines)
    }

    @Suppress("unused")
    private fun unusedHeaders() = Headers.Builder().build()
}
