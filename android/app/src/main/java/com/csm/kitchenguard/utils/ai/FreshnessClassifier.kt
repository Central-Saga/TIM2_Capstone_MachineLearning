package com.csm.kitchenguard.utils.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.csm.kitchenguard.data.local.entity.enums.FreshnessClass
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Utilitas untuk mengeksekusi model TensorFlow Lite secara offline di perangkat.
 * PRD Section 16 — On-Device AI Freshness Classification.
 *
 * INTEGRITAS (Issue #7):
 * Class ini TIDAK memiliki fallback acak. Bila file model `freshness-v1.tflite`
 * tidak ada di `app/src/main/assets/`, maka [isReady] = false dan setiap
 * [classifyImage] mengembalikan hasil jujur berupa [FreshnessClass.UNCERTAIN]
 * dengan penanda [FreshnessResult.isModelAvailable] = false.
 */
class FreshnessClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null

    /** Alasan model gagal dimuat (null bila berhasil). */
    private var loadError: String? = null

    private val modelVersion = MODEL_VERSION

    /** True bila interpreter TFLite berhasil dimuat dan siap inferensi. */
    val isReady: Boolean get() = interpreter != null

    // Label klasifikasi sesuai urutan array output model (0-3)
    private val labels = listOf(
        FreshnessClass.FRESH.name,
        FreshnessClass.ACCEPTABLE.name,
        FreshnessClass.SPOILED.name,
        FreshnessClass.REJECT.name
    )

    init {
        try {
            val assetFileDescriptor = context.assets.openFd(MODEL_ASSET_NAME)
            FileInputStream(assetFileDescriptor.fileDescriptor).use { fileInputStream ->
                val mappedByteBuffer = fileInputStream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.declaredLength
                )
                interpreter = Interpreter(mappedByteBuffer)
            }
            loadError = null
            Log.i(TAG, "Model $MODEL_ASSET_NAME berhasil dimuat ($modelVersion).")
        } catch (e: Exception) {
            // Tidak ada fallback acak: kita catat penyebabnya dan biarkan
            // classifyImage() mengembalikan status jujur "model tidak tersedia".
            interpreter = null
            loadError = "Model $MODEL_ASSET_NAME tidak dapat dimuat: ${e.message}"
            Log.w(
                TAG,
                "Model $MODEL_ASSET_NAME tidak tersedia. AI freshness " +
                    "akan mengembalikan UNCERTAIN (tanpa tebakan).", e
            )
        }
    }

    /**
     * Memproses gambar (Bitmap) untuk diklasifikasi tingkat kesegarannya.
     * Menerapkan threshold 85% untuk menentukan apakah hasilnya cukup meyakinkan.
     *
     * Bila model tidak tersedia, mengembalikan hasil jujur (UNCERTAIN,
     * isModelAvailable = false) — TIDAK mengembalikan nilai acak.
     */
    fun classifyImage(bitmap: Bitmap): FreshnessResult {
        val startTime = System.currentTimeMillis()

        // ── Model tidak tersedia → hasil jujur, bukan tebakan ────────────────
        val activeInterpreter = interpreter
            ?: return FreshnessResult.modelUnavailable(
                modelVersion = modelVersion,
                reason = loadError ?: "Interpreter TFLite tidak terinisialisasi.",
                inferenceTimeMs = System.currentTimeMillis() - startTime
            )

        // 1. Preprocessing — model membutuhkan input 224x224 RGB Float32 [-1.0, 1.0]
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // 2. Inference — output array: [1][4] probabilitas untuk 4 kelas
        val outputBuffer = Array(1) { FloatArray(labels.size) }
        activeInterpreter.run(inputBuffer, outputBuffer)

        val probabilities = outputBuffer[0]

        // 3. Post-processing (ArgMax)
        var maxIndex = 0
        var maxConfidence = probabilities[0]
        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxConfidence) {
                maxConfidence = probabilities[i]
                maxIndex = i
            }
        }

        // 4. Confidence Gate (PRD Section 16: < 85% = UNCERTAIN)
        val finalClass = FreshnessClass.evaluate(labels[maxIndex], maxConfidence.toDouble())

        val inferenceTime = System.currentTimeMillis() - startTime

        return FreshnessResult(
            predictedClass = finalClass.name,
            confidenceScore = maxConfidence.toDouble(),
            modelVersion = modelVersion,
            inferenceTimeMs = inferenceTime,
            isModelAvailable = true,
            errorMessage = null
        )
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // INPUT_SIZE * INPUT_SIZE * 3 (RGB) * 4 (Float32 bytes)
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                // Normalisasi ke [-1.0, 1.0]
                byteBuffer.putFloat((((value shr 16 and 0xFF) - 127.5f) / 127.5f))
                byteBuffer.putFloat((((value shr 8 and 0xFF) - 127.5f) / 127.5f))
                byteBuffer.putFloat((((value and 0xFF) - 127.5f) / 127.5f))
            }
        }
        return byteBuffer
    }

    /** Melepaskan resource interpreter. Aman dipanggil berkali-kali. */
    fun close() {
        interpreter?.close()
        interpreter = null
    }

    companion object {
        private const val TAG = "FreshnessClassifier"

        /** Nama file model di app/src/main/assets/. */
        const val MODEL_ASSET_NAME = "freshness-v1.tflite"

        /** Versi model, dicatat ke WasteRecordEntity.aiModelVersion. */
        const val MODEL_VERSION = "freshness-v1.0"

        /** Ukuran sisi input model (224x224). */
        private const val INPUT_SIZE = 224
    }
}
