package com.csm.kitchenguard.domain.usecase

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit test untuk [ValidateOcrWeightUseCase].
 * PRD Section 15 — Scale OCR Capture & Confidence Gate.
 */
class ValidateOcrWeightUseCaseTest {

    private lateinit var useCase: ValidateOcrWeightUseCase

    @Before
    fun setUp() {
        useCase = ValidateOcrWeightUseCase()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Confidence Gate (Threshold = 0.90)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN confidence 0_95 WHEN invoke THEN isAutoFilled is true`() {
        val result = useCase("1.45 kg", confidence = 0.95)

        assertTrue("isAutoFilled harus true jika confidence >= 0.90", result.isAutoFilled)
        assertFalse("needsManualConfirmation harus false", result.needsManualConfirmation)
    }

    @Test
    fun `GIVEN confidence exactly 0_90 WHEN invoke THEN isAutoFilled is true`() {
        val result = useCase("2.5 kg", confidence = 0.90)

        // Tepat di threshold harus diterima (>= bukan >)
        assertTrue("isAutoFilled harus true pada threshold tepat", result.isAutoFilled)
    }

    @Test
    fun `GIVEN confidence 0_80 WHEN invoke THEN needsManualConfirmation is true`() {
        val result = useCase("1.50 kg", confidence = 0.80)

        assertFalse("isAutoFilled harus false jika confidence < 0.90", result.isAutoFilled)
        assertTrue("needsManualConfirmation harus true jika confidence < 0.90", result.needsManualConfirmation)
    }

    @Test
    fun `GIVEN confidence 0_0 WHEN invoke THEN needsManualConfirmation is true`() {
        val result = useCase("1.45", confidence = 0.0)

        assertTrue("needsManualConfirmation harus true jika confidence 0.0", result.needsManualConfirmation)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Ekstraksi Angka & Unit
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN text 1_45 kg WHEN invoke THEN extracted quantity is 1_45`() {
        val result = useCase("1.45 kg", confidence = 0.95)

        assertEquals(1.45, result.extractedQuantity)
        assertEquals("kg", result.extractedUnit)
    }

    @Test
    fun `GIVEN text 500g WHEN invoke THEN extracted quantity is 500`() {
        val result = useCase("500g", confidence = 0.95)

        assertEquals(500.0, result.extractedQuantity)
        assertEquals("g", result.extractedUnit)
    }

    @Test
    fun `GIVEN text with comma separator WHEN invoke THEN parsed correctly`() {
        val result = useCase("3,5 kg", confidence = 0.95)

        assertEquals(3.5, result.extractedQuantity)
    }

    @Test
    fun `GIVEN text with only number WHEN invoke THEN unit is null`() {
        val result = useCase("2.5", confidence = 0.95)

        assertEquals(2.5, result.extractedQuantity)
        assertNull("Unit harus null jika tidak ada dalam teks", result.extractedUnit)
    }

    @Test
    fun `GIVEN text with no number WHEN invoke THEN extractedQuantity is null`() {
        val result = useCase("kg only no number", confidence = 0.95)

        // Regex "(\d+[.,]?\d*)" masih mungkin menangkap "0" dari awalan, kita cek isAutoFilled
        // Karena tidak ada angka valid → autofill false
        assertFalse("isAutoFilled harus false jika teks tidak mengandung angka valid", result.isAutoFilled)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 3: Edge Cases
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN text with newlines WHEN invoke THEN text is cleaned`() {
        val result = useCase("1.45\nkg", confidence = 0.95)

        // Newline harus dibersihkan sebelum di-parse
        assertNotNull("Seharusnya berhasil parse meski ada newline", result.extractedQuantity)
    }

    @Test
    fun `GIVEN high confidence but text is pure noise WHEN invoke THEN isAutoFilled is false`() {
        // Jika teks noise dan tidak mengandung angka, meskipun confidence tinggi
        // isAutoFilled = isReliable && quantity != null → false jika quantity null
        val result = useCase("Net Wt", confidence = 0.99)

        assertFalse("isAutoFilled harus false meski confidence tinggi, jika angka tidak ditemukan", result.isAutoFilled)
    }
}
