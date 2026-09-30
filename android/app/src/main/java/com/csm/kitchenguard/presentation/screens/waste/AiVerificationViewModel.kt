package com.csm.kitchenguard.presentation.screens.waste

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.csm.kitchenguard.data.local.entity.enums.FreshnessClass
import com.csm.kitchenguard.utils.ai.FreshnessClassifier
import com.csm.kitchenguard.utils.ai.FreshnessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Status pemrosesan AI pada layar AI Verification (Figma M05).
 */
enum class AiScanPhase {
    /** Belum ada gambar / menunggu capture. */
    IDLE,

    /** Inferensi TFLite sedang berjalan. */
    ANALYZING,

    /** Inferensi selesai dan model tersedia. */
    DONE,

    /**
     * Model TFLite tidak tersedia / gagal dimuat.
     * UI wajib meminta staff melakukan observasi manual — tidak ada tebakan.
     */
    MODEL_UNAVAILABLE
}

/**
 * State lengkap layar AI Freshness Verification.
 *
 * Semua nilai berasal dari inferensi model TFLite yang nyata (atau status jujur
 * "model tidak tersedia"). Tidak ada nilai acak / hardcoded.
 */
data class AiVerificationUiState(
    val phase: AiScanPhase = AiScanPhase.IDLE,
    val predictedClass: String = FreshnessClass.UNCERTAIN.name,
    val confidenceScore: Double = 0.0,
    val modelVersion: String = FreshnessClassifier.MODEL_VERSION,
    val inferenceTimeMs: Long = 0L,
    val isModelAvailable: Boolean = false,
    val errorMessage: String? = null
) {
    /** True bila confidence di bawah ambang batas → wajib verifikasi manusia. */
    val requiresHumanReview: Boolean
        get() = isModelAvailable &&
            confidenceScore < FreshnessClass.CONFIDENCE_THRESHOLD

    /** Confidence dalam persen untuk ditampilkan di UI. */
    val confidencePercent: Double get() = confidenceScore * 100.0
}

/**
 * ViewModel untuk AI Verification (PRD Section 16).
 *
 * Menjembatani [FreshnessClassifier] (inferensi on-device) dengan UI.
 * Integritas (Issue #7): bila model tidak tersedia, state menjadi
 * [AiScanPhase.MODEL_UNAVAILABLE] dan UI menampilkan pesan jujur, bukan hasil acak.
 */
class AiVerificationViewModel(
    application: Application,
    private val freshnessClassifier: FreshnessClassifier
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        AiVerificationUiState(
            // Cek ketersediaan model sejak awal agar UI bisa memperingatkan lebih dulu.
            phase = if (freshnessClassifier.isReady) {
                AiScanPhase.IDLE
            } else {
                AiScanPhase.MODEL_UNAVAILABLE
            },
            isModelAvailable = freshnessClassifier.isReady,
            errorMessage = if (freshnessClassifier.isReady) {
                null
            } else {
                "Model AI kesegaran belum tersedia di perangkat ini."
            }
        )
    )
    val uiState: StateFlow<AiVerificationUiState> = _uiState.asStateFlow()

    /**
     * Menjalankan inferensi AI atas [bitmap].
     *
     * Bila model tidak tersedia, hasilnya adalah status jujur
     * [AiScanPhase.MODEL_UNAVAILABLE] (bukan tebakan acak).
     */
    fun classify(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(phase = AiScanPhase.ANALYZING, errorMessage = null) }

            val result: FreshnessResult = withContext(Dispatchers.Default) {
                freshnessClassifier.classifyImage(bitmap)
            }

            _uiState.update { it.applyResult(result) }
        }
    }

    private fun AiVerificationUiState.applyResult(result: FreshnessResult): AiVerificationUiState =
        copy(
            phase = if (result.isModelAvailable) {
                AiScanPhase.DONE
            } else {
                AiScanPhase.MODEL_UNAVAILABLE
            },
            predictedClass = result.predictedClass,
            confidenceScore = result.confidenceScore,
            modelVersion = result.modelVersion,
            inferenceTimeMs = result.inferenceTimeMs,
            isModelAvailable = result.isModelAvailable,
            errorMessage = result.errorMessage
                ?: if (result.isModelAvailable) null
                else "Model AI kesegaran belum tersedia di perangkat ini."
        )

    override fun onCleared() {
        super.onCleared()
        freshnessClassifier.close()
    }
}
