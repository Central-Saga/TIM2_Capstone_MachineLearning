package com.csm.kitchenguard.utils.ocr

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk [OcrConfidenceResolver] dan [OcrScaleReading].
 *
 * Issue #8 — memastikan confidence OCR berasal dari ML Kit yang sesungguhnya
 * (atau ditandai "tidak tersedia"), bukan rasio regex palsu.
 */
class OcrConfidenceResolverTest {

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: OcrScaleReading.of — validasi nilai confidence mentah
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN valid confidence WHEN of THEN confidenceAvailable is true`() {
        val reading = OcrScaleReading.of("1.45 kg", 0.93f)

        assertTrue(reading.confidenceAvailable)
        assertEquals(0.93f, reading.confidence, 0.001f)
    }

    @Test
    fun `GIVEN NaN confidence WHEN of THEN marked unavailable`() {
        val reading = OcrScaleReading.of("1.45 kg", Float.NaN)

        assertFalse("NaN harus dianggap confidence tidak tersedia", reading.confidenceAvailable)
        assertEquals(OcrScaleReading.CONFIDENCE_UNAVAILABLE, reading.confidence, 0.0f)
    }

    @Test
    fun `GIVEN out of range confidence WHEN of THEN marked unavailable`() {
        val reading = OcrScaleReading.of("1.45 kg", 1.5f)

        assertFalse(reading.confidenceAvailable)
        assertEquals(OcrScaleReading.CONFIDENCE_UNAVAILABLE, reading.confidence, 0.0f)
    }

    @Test
    fun `GIVEN boundary confidence 0 and 1 WHEN of THEN both accepted`() {
        assertTrue(OcrScaleReading.of("x", 0.0f).confidenceAvailable)
        assertTrue(OcrScaleReading.of("x", 1.0f).confidenceAvailable)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: OcrConfidenceResolver.resolve — prioritas & fallback
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN valid line confidence WHEN resolve THEN uses line value`() {
        val reading = OcrConfidenceResolver.resolve(
            rawText = "1.45 kg",
            lineConfidence = 0.91f,
            elementConfidences = listOf(0.20f, 0.30f) // harus diabaikan
        )

        assertTrue(reading.confidenceAvailable)
        assertEquals(0.91f, reading.confidence, 0.001f)
    }

    @Test
    fun `GIVEN invalid line but valid elements WHEN resolve THEN averages elements`() {
        val reading = OcrConfidenceResolver.resolve(
            rawText = "1.45 kg",
            lineConfidence = Float.NaN,
            elementConfidences = listOf(0.80f, 0.90f, 0.70f)
        )

        assertTrue("Rata-rata elemen valid harus dipakai", reading.confidenceAvailable)
        assertEquals(0.80f, reading.confidence, 0.001f) // (0.8+0.9+0.7)/3
    }

    @Test
    fun `GIVEN all confidences invalid WHEN resolve THEN marked unavailable`() {
        val reading = OcrConfidenceResolver.resolve(
            rawText = "1.45 kg",
            lineConfidence = Float.NaN,
            elementConfidences = listOf(Float.NaN, 2.0f)
        )

        assertFalse(reading.confidenceAvailable)
        assertEquals(OcrScaleReading.CONFIDENCE_UNAVAILABLE, reading.confidence, 0.0f)
    }

    @Test
    fun `GIVEN no elements and invalid line WHEN resolve THEN marked unavailable`() {
        val reading = OcrConfidenceResolver.resolve(
            rawText = "kg",
            lineConfidence = -Float.MIN_VALUE,
            elementConfidences = emptyList()
        )

        assertFalse(reading.confidenceAvailable)
    }

    @Test
    fun `GIVEN dirty text but genuine high confidence WHEN resolve THEN keeps confidence`() {
        // Poin inti issue #8: confidence TIDAK bergantung pada panjang/kerapian teks.
        val clean = OcrConfidenceResolver.resolve("1.45", 0.95f, emptyList())
        val dirty = OcrConfidenceResolver.resolve("net wt 1.45 kg total", 0.95f, emptyList())

        assertTrue(clean.confidenceAvailable)
        assertTrue(dirty.confidenceAvailable)
        assertEquals(
            "Confidence harus sama meski teks 'kotor', karena berasal dari ML Kit",
            clean.confidence,
            dirty.confidence,
            0.0001f
        )
    }
}
