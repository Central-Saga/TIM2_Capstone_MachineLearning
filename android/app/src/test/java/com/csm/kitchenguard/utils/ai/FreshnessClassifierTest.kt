package com.csm.kitchenguard.utils.ai

import android.content.Context
import android.content.res.AssetManager
import com.csm.kitchenguard.data.local.entity.enums.FreshnessClass
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import java.io.FileNotFoundException

/**
 * Unit test untuk [FreshnessClassifier] — fokus pada INTEGRITAS (Issue #7).
 *
 * Memastikan bahwa ketika model `.tflite` tidak tersedia, classifier
 * mengembalikan status jujur (UNCERTAIN, isModelAvailable = false)
 * dan TIDAK mengembalikan nilai acak / mock.
 *
 * Catatan: test ini tidak menjalankan inferensi TFLite nyata (butuh model +
 * runtime Android). Ia menguji jalur "model tidak tersedia" dan kontrak result.
 */
class FreshnessClassifierTest {

    /** Context tiruan yang tidak memiliki file model di assets. */
    private fun contextWithoutModel(): Context {
        val assets = mockk<AssetManager>()
        every { assets.openFd(FreshnessClassifier.MODEL_ASSET_NAME) } throws
            FileNotFoundException("no model in test")
        val context = mockk<Context>()
        every { context.assets } returns assets
        return context
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Model gagal dimuat → status jujur
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN model not present WHEN constructed THEN isReady is false`() {
        val classifier = FreshnessClassifier(contextWithoutModel())

        assertFalse(
            "isReady harus false bila file model tidak dapat dimuat",
            classifier.isReady
        )
        classifier.close()
    }

    @Test
    fun `GIVEN model not present WHEN classifyImage THEN returns modelUnavailable without guessing`() {
        val classifier = FreshnessClassifier(contextWithoutModel())

        val result = classifier.classifyImage(mockk(relaxed = true))

        assertFalse(
            "isModelAvailable harus false agar UI tahu model tidak ada",
            result.isModelAvailable
        )
        assertEquals(
            "Kelas harus UNCERTAIN, bukan tebakan acak",
            FreshnessClass.UNCERTAIN.name,
            result.predictedClass
        )
        assertEquals(
            "Confidence harus 0.0 (tidak ada inferensi)",
            0.0,
            result.confidenceScore,
            0.0
        )
        assertNotNull("errorMessage harus diisi sebagai alasan jujur", result.errorMessage)

        classifier.close()
    }

    @Test
    fun `GIVEN model not present WHEN classifyImage twice THEN never returns a random class`() {
        val classifier = FreshnessClassifier(contextWithoutModel())
        val bitmap = mockk<android.graphics.Bitmap>(relaxed = true)

        // Jalankan berkali-kali: bila ada fallback acak, kelas bisa berubah.
        val results = (1..20).map { classifier.classifyImage(bitmap) }

        assertTrue(
            "Semua hasil harus UNCERTAIN (tidak ada randomness)",
            results.all { it.predictedClass == FreshnessClass.UNCERTAIN.name }
        )
        assertTrue(
            "modelVersion tidak boleh mengandung penanda '-mock'",
            results.none { it.modelVersion.contains("mock", ignoreCase = true) }
        )

        classifier.close()
    }

    @Test
    fun `GIVEN classifier closed WHEN closed again THEN does not throw`() {
        val classifier = FreshnessClassifier(contextWithoutModel())

        classifier.close()
        classifier.close() // harus aman (idempotent)
    }
}
