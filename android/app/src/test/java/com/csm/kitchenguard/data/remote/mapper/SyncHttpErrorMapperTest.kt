package com.csm.kitchenguard.data.remote.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test untuk [SyncHttpErrorMapper].
 *
 * Issue #12 / PRD Section 26 — membedakan error RETRYABLE (5xx) dari
 * TERMINAL (422/401/403) dan CONFLICT (409).
 */
class SyncHttpErrorMapperTest {

    @Test
    fun `GIVEN 422 WHEN classify THEN terminal`() {
        assertEquals(
            SyncFailureDisposition.TERMINAL,
            SyncHttpErrorMapper.classify(422)
        )
    }

    @Test
    fun `GIVEN 500 WHEN classify THEN retryable`() {
        assertEquals(
            SyncFailureDisposition.RETRYABLE,
            SyncHttpErrorMapper.classify(500)
        )
    }

    @Test
    fun `GIVEN 503 WHEN classify THEN retryable`() {
        assertEquals(
            SyncFailureDisposition.RETRYABLE,
            SyncHttpErrorMapper.classify(503)
        )
    }

    @Test
    fun `GIVEN any 5xx WHEN classify THEN retryable`() {
        (500..599).forEach { code ->
            assertEquals(
                "HTTP $code harus retryable",
                SyncFailureDisposition.RETRYABLE,
                SyncHttpErrorMapper.classify(code)
            )
        }
    }

    @Test
    fun `GIVEN 409 WHEN classify THEN conflict`() {
        assertEquals(
            SyncFailureDisposition.CONFLICT,
            SyncHttpErrorMapper.classify(409)
        )
    }

    @Test
    fun `GIVEN 401 WHEN classify THEN terminal not retryable`() {
        assertEquals(
            "Token kedaluwarsa butuh aksi user, bukan retry",
            SyncFailureDisposition.TERMINAL,
            SyncHttpErrorMapper.classify(401)
        )
    }

    @Test
    fun `GIVEN 403 WHEN classify THEN terminal not retryable`() {
        assertEquals(
            SyncFailureDisposition.TERMINAL,
            SyncHttpErrorMapper.classify(403)
        )
    }

    @Test
    fun `GIVEN unknown 4xx WHEN classify THEN terminal`() {
        // Retry payload yang sama untuk 4xx tak dikenal kemungkinan tetap gagal.
        assertEquals(SyncFailureDisposition.TERMINAL, SyncHttpErrorMapper.classify(404))
        assertEquals(SyncFailureDisposition.TERMINAL, SyncHttpErrorMapper.classify(418))
    }

    @Test
    fun `GIVEN 422 WHEN classify THEN never retryable`() {
        // Regresi issue #12: 422 TIDAK boleh dianggap retryable.
        assertEquals(
            "422 harus terminal",
            SyncFailureDisposition.TERMINAL,
            SyncHttpErrorMapper.classify(422)
        )
    }
}
