package com.csm.kitchenguard.domain.usecase

import com.csm.kitchenguard.utils.ocr.OcrScaleReading
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit test untuk [ValidateOcrWeightUseCase] dengan kontrak confidence yang
 * benar (Issue #8).
 *
 * Fokus: gate 0.90 hanya boleh auto-fill bila confidence ASLI tersedia;
 * confidence yang tidak tersedia wajib konfirmasi manual.
 */
class ValidateOcrWeightConfidenceGateTest {

    private lateinit var useCase: ValidateOcrWeightUseCase

    @Before
    fun setUp() {
        useCase = ValidateOcrWeightUseCase()
    }

    private fun reading(text: String, confidence: Float) =
        OcrScaleReading.of(text, confidence)

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Confidence tersedia — gate 0.90 berlaku
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN available confidence above threshold WHEN invoke THEN auto-fill`() {
        val result = useCase(reading("1.45 kg", 0.95f))

        assertTrue(result.confidenceAvailable)
        assertTrue(result.isAutoFilled)
        assertFalse(result.needsManualConfirmation)
        assertEquals(1.45, result.extractedQuantity!!, 0.0001)
    }

    @Test
    fun `GIVEN available confidence exactly 0_90 WHEN invoke THEN auto-fill`() {
        val result = useCase(reading("2.5 kg", 0.90f))

        assertTrue("Batas >= 0.90 harus diterima", result.isAutoFilled)
    }

    @Test
    fun `GIVEN available confidence just below threshold WHEN invoke THEN manual`() {
        val result = useCase(reading("1.50 kg", 0.89f))

        assertTrue(result.confidenceAvailable)
        assertFalse(result.isAutoFilled)
        assertTrue(result.needsManualConfirmation)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Confidence TIDAK tersedia — tidak boleh auto-fill
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN unavailable confidence WHEN invoke THEN never auto-fill`() {
        val result = useCase(reading("1.45 kg", Float.NaN))

        assertFalse(result.confidenceAvailable)
        assertFalse("Tidak boleh auto-fill dari confidence tak diketahui", result.isAutoFilled)
        assertTrue(result.needsManualConfirmation)
        // Angka tetap diekstraksi agar staff bisa memverifikasi manual.
        assertEquals(1.45, result.extractedQuantity!!, 0.0001)
    }

    @Test
    fun `GIVEN unavailable confidence via out of range WHEN invoke THEN never auto-fill`() {
        val result = useCase(reading("500 g", 2.0f))

        assertFalse(result.confidenceAvailable)
        assertFalse(result.isAutoFilled)
        assertTrue(result.needsManualConfirmation)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 3: Regresi Issue #8 — confidence TIDAK bergantung pada teks bersih
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN clean number text WHEN confidence high THEN same gate as noisy text`() {
        // Sebelumnya "1.45" selalu ~1.0 (heuristik rasio). Sekarang confidence
        // murni dari sumber; kedua teks dengan confidence sama menempuh gate sama.
        val clean = useCase(reading("1.45", 0.60f))
        val noisy = useCase(reading("net 1.45 kg total", 0.60f))

        assertEquals(clean.isAutoFilled, noisy.isAutoFilled)
        assertFalse("Confidence 0.60 < 0.90 → tidak auto-fill, apapun kerapian teksnya", clean.isAutoFilled)
    }

    @Test
    fun `GIVEN dirty text but genuine high confidence WHEN invoke THEN auto-fill allowed`() {
        val result = useCase(reading("berat bersih 1.45 kg", 0.97f))

        assertTrue(result.isAutoFilled)
        assertEquals(1.45, result.extractedQuantity!!, 0.0001)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 4: Tidak ada angka — tetap manual
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN no number WHEN invoke THEN needs manual regardless of confidence`() {
        val result = useCase(reading("net wt only", 0.99f))

        assertFalse(result.isAutoFilled)
        assertTrue(result.needsManualConfirmation)
        assertNull(result.extractedQuantity)
    }
}
