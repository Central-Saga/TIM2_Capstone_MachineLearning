package com.csm.kitchenguard.presentation.components.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.csm.kitchenguard.utils.ocr.OcrConfidenceResolver
import com.csm.kitchenguard.utils.ocr.OcrScaleReading
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * ImageAnalyzer khusus untuk mengekstrak angka berat dari layar timbangan digital
 * menggunakan Google ML Kit Text Recognition.
 *
 * INTEGRITAS (Issue #8):
 * Confidence yang dihasilkan berasal dari ML Kit (`Line.getConfidence()` /
 * `Element.getConfidence()`), BUKAN rasio panjang teks terhadap regex.
 * Bila ML Kit tidak menyediakan confidence yang valid, [OcrScaleReading.confidenceAvailable]
 * akan `false` sehingga gate OCR menuntut konfirmasi manual.
 */
class ScaleOcrAnalyzer(
    private val onResult: (reading: OcrScaleReading) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Pola regex spesifik untuk timbangan (misal: "1.250", "3.5 kg")
    private val weightRegex = Regex("""(\d+[.,]?\d*)\s*([a-zA-Z]+)?""")

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val lineText = line.text.lowercase().trim()

                            // Hanya proses baris yang mengandung pola angka berat.
                            if (weightRegex.containsMatchIn(lineText)) {
                                val reading = toReading(line, lineText)
                                if (reading != null) {
                                    onResult(reading)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // Abaikan error saat streaming frame
                }
                .addOnCompleteListener {
                    // Sangat krusial: tutup proxy agar CameraX bisa mengirim frame berikutnya
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    /**
     * Membentuk [OcrScaleReading] dari [Text.Line] dengan mengambil confidence
     * asli dari ML Kit. Mengembalikan `null` bila baris tidak punya confidence
     * berarti (mencegah publish noise).
     */
    private fun toReading(line: Text.Line, lineText: String): OcrScaleReading? {
        val elementConfidences = line.elements.map { it.confidence }
        val reading = OcrConfidenceResolver.resolve(
            rawText = lineText,
            lineConfidence = line.confidence,
            elementConfidences = elementConfidences
        )

        // Bila confidence tersedia, terapkan ambang minimal agar tidak mem-publish noise.
        // Bila tidak tersedia, tetap teruskan (confidenceAvailable=false) agar UI
        // bisa meminta konfirmasi manual — bukan dibuang tanpa jejak.
        return if (!reading.confidenceAvailable || reading.confidence > 0.5) {
            reading
        } else {
            null
        }
    }
}
