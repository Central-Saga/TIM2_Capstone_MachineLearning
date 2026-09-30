package com.csm.kitchenguard.utils.ai

import com.csm.kitchenguard.data.local.entity.enums.FreshnessClass
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk confidence gate (PRD Section 16) dan kontrak
 * [FreshnessResult] — termasuk jalur "model tidak tersedia" (Issue #7).
 */
class FreshnessGateTest {

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: FreshnessClass.evaluate — Confidence Gate (threshold 0.85)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN confidence above threshold WHEN evaluate THEN returns predicted class`() {
        val result = FreshnessClass.evaluate(FreshnessClass.FRESH.name, 0.90)

        assertEquals(FreshnessClass.FRESH, result)
    }

    @Test
    fun `GIVEN confidence exactly threshold WHEN evaluate THEN returns predicted class`() {
        // 0.85 tepat di batas harus diterima (>= bukan >)
        val result = FreshnessClass.evaluate(FreshnessClass.SPOILED.name, 0.85)

        assertEquals(FreshnessClass.SPOILED, result)
    }

    @Test
    fun `GIVEN confidence below threshold WHEN evaluate THEN downgrades to UNCERTAIN`() {
        val result = FreshnessClass.evaluate(FreshnessClass.REJECT.name, 0.84)

        assertEquals(
            "AI tidak boleh auto-reject di bawah threshold",
            FreshnessClass.UNCERTAIN,
            result
        )
    }

    @Test
    fun `GIVEN unknown label WHEN evaluate THEN falls back to UNCERTAIN`() {
        val result = FreshnessClass.evaluate("NOT_A_REAL_CLASS", 0.99)

        assertEquals(FreshnessClass.UNCERTAIN, result)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: FreshnessResult.modelUnavailable — status jujur
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN modelUnavailable factory WHEN called THEN flags unavailable and UNCERTAIN`() {
        val result = FreshnessResult.modelUnavailable(
            modelVersion = "freshness-v1.0",
            reason = "file missing"
        )

        assertFalse(result.isModelAvailable)
        assertEquals(FreshnessClass.UNCERTAIN.name, result.predictedClass)
        assertEquals(0.0, result.confidenceScore, 0.0)
        assertEquals("file missing", result.errorMessage)
        assertEquals("freshness-v1.0", result.modelVersion)
    }

    @Test
    fun `GIVEN default FreshnessResult WHEN created THEN assumed model available`() {
        val result = FreshnessResult(
            predictedClass = FreshnessClass.FRESH.name,
            confidenceScore = 0.95,
            modelVersion = "freshness-v1.0",
            inferenceTimeMs = 42L
        )

        assertTrue(
            "Default isModelAvailable harus true untuk hasil inferensi nyata",
            result.isModelAvailable
        )
        assertNull("Hasil sukses tidak boleh membawa errorMessage", result.errorMessage)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 3: Label ordering contract (index 0-3 = FRESH, ACCEPTABLE, SPOILED, REJECT)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN canonical order WHEN converting value THEN matches enum order`() {
        // Kontrak urutan output model .tflite yang harus dipatuhi tim ML.
        assertEquals(FreshnessClass.FRESH, FreshnessClass.fromValue("FRESH"))
        assertEquals(FreshnessClass.ACCEPTABLE, FreshnessClass.fromValue("ACCEPTABLE"))
        assertEquals(FreshnessClass.SPOILED, FreshnessClass.fromValue("SPOILED"))
        assertEquals(FreshnessClass.REJECT, FreshnessClass.fromValue("REJECT"))
    }
}
