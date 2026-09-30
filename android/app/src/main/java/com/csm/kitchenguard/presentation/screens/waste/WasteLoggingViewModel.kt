package com.csm.kitchenguard.presentation.screens.waste

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.sync.SyncScheduler
import com.csm.kitchenguard.data.local.entity.BatchEntity
import com.csm.kitchenguard.data.local.entity.IngredientEntity
import com.csm.kitchenguard.domain.usecase.CreateWasteRecordUseCase
import com.csm.kitchenguard.domain.usecase.ValidateOcrWeightUseCase
import com.csm.kitchenguard.utils.ai.FreshnessClassifier
import com.csm.kitchenguard.utils.ocr.OcrScaleReading
import com.csm.kitchenguard.utils.units.MeasurementUnit
import com.csm.kitchenguard.utils.units.UnitConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Rangkuman penuh state form pencatatan waste. */
data class WasteFormUiState(
    val selectedIngredient: IngredientEntity? = null,
    val selectedBatch: BatchEntity? = null,
    val quantityInput: String = "",
    val selectedUnit: String = "kg",
    val selectedReason: String = "", // misal SPOILED

    /**
     * Daftar satuan yang valid dipilih untuk bahan terpilih (PRD Section 13).
     * Mis. canonical "kg" → [kg, g]. Kosong bila belum ada bahan dipilih.
     */
    val availableUnits: List<MeasurementUnit> = emptyList(),

    // OCR & AI Context
    val ocrWeight: Double? = null,
    /** Confidence OCR asli dari ML Kit (Float), null bila tidak tersedia. */
    val ocrConfidence: Float? = null,
    /** True bila confidence OCR tersedia dari ML Kit (bukan heuristik). */
    val ocrConfidenceAvailable: Boolean = false,
    val needsOcrConfirmation: Boolean = false,
    
    val aiClass: String? = null,
    val aiConfidence: Double? = null,
    val isAiUncertain: Boolean = false,
    
    val photoUri: String? = null,
    
    // Submit Status
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Nilai input yang diekspresikan dalam canonical unit bahan, untuk kebutuhan
     * display/preview (PRD Section 13: konversi hanya untuk visual).
     *
     * Backend tetap menghitung nilai baku sebenarnya. Mengembalikan null bila
     * input tidak valid atau bahan belum dipilih.
     */
    val canonicalQuantityPreview: Double?
        get() {
            val canonical = selectedIngredient?.canonicalUnit ?: return null
            val value = quantityInput.toDoubleOrNull() ?: return null
            return UnitConverter.toDisplayValue(value, selectedUnit, canonical)
        }
}

/**
 * ViewModel pengelola kompleksitas Waste Logging Form (AI, OCR, Manual Input).
 */
class WasteLoggingViewModel(
    application: Application,
    private val masterDataDao: MasterDataDao,
    private val shiftDao: ShiftDao,
    private val validateOcrWeightUseCase: ValidateOcrWeightUseCase,
    private val createWasteRecordUseCase: CreateWasteRecordUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WasteFormUiState())
    val uiState: StateFlow<WasteFormUiState> = _uiState.asStateFlow()

    // Opsi Bahan Makanan dari DB (Dropdown Data)
    val ingredientsList: StateFlow<List<IngredientEntity>> = masterDataDao.getAllIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Update State (Intent) ──────────────────────────────────────────────────

    fun onIngredientSelected(ingredient: IngredientEntity) {
        _uiState.update { 
            it.copy(
                selectedIngredient = ingredient,
                selectedBatch = null, // Reset batch jika bahan diganti
                selectedUnit = ingredient.canonicalUnit,
                // Sediakan pilihan satuan se-kategori untuk konversi input (PRD Section 13).
                availableUnits = UnitConverter.availableUnitsFor(ingredient.canonicalUnit)
            ) 
        }
    }

    /**
     * Mengubah satuan input yang dipilih user.
     *
     * Nilai numerik yang sudah diketik akan dikonversi agar besar FISIK tetap sama
     * (mis. 1 kg → 1000 g). Bila input kosong/tidak valid, hanya satuan yang berubah.
     * Konversi ini hanya untuk display; backend tetap memakai nilai baku.
     */
    fun onUnitSelected(unit: MeasurementUnit) {
        _uiState.update { state ->
            val currentValue = state.quantityInput.toDoubleOrNull()
            val rebased = currentValue?.let { value ->
                UnitConverter.rebaseValue(value, state.selectedUnit, unit.symbol).getOrNull()
            }
            state.copy(
                selectedUnit = unit.symbol,
                quantityInput = rebased?.let { formatQuantity(it) } ?: state.quantityInput
            )
        }
    }

    /** Format nilai hasil konversi tanpa notasi ilmiah / desimal berlebih. */
    private fun formatQuantity(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else value.toString()

    fun onBatchSelected(batch: BatchEntity) {
        _uiState.update { it.copy(selectedBatch = batch) }
    }

    fun onQuantityInputChanged(text: String) {
        _uiState.update { it.copy(quantityInput = text) }
    }

    fun onReasonChanged(reason: String) {
        _uiState.update { it.copy(selectedReason = reason) }
    }

    fun onPhotoCaptured(uri: String) {
        _uiState.update { it.copy(photoUri = uri) }
    }

    // ── AI & OCR Integrasi ────────────────────────────────────────────────────

    /**
     * Memproses hasil pembacaan OCR timbangan.
     *
     * Menerima [OcrScaleReading] yang membawa confidence ASLI dari ML Kit
     * beserta status ketersediaannya (Issue #8). Tidak ada heuristik rasio regex.
     */
    fun onOcrCaptured(reading: OcrScaleReading) {
        val result = validateOcrWeightUseCase(reading)

        _uiState.update {
            it.copy(
                ocrWeight = result.extractedQuantity,
                // Simpan confidence hanya bila tersedia; null bila tidak.
                ocrConfidence = if (result.confidenceAvailable) reading.confidence else null,
                ocrConfidenceAvailable = result.confidenceAvailable,
                needsOcrConfirmation = result.needsManualConfirmation,
                // Auto-fill HANYA bila confidence tersedia dan valid (>= 0.90).
                quantityInput = if (result.isAutoFilled) {
                    result.extractedQuantity.toString()
                } else {
                    it.quantityInput
                },
                selectedUnit = result.extractedUnit ?: it.selectedUnit
            )
        }
    }

    /**
     * Overload kompatibilitas untuk pemanggil lama (teks + confidence ganda).
     * Confidence yang tidak valid diperlakukan sebagai "tidak tersedia".
     */
    fun onOcrCaptured(rawText: String, confidence: Double) {
        onOcrCaptured(OcrScaleReading.of(rawText, confidence.toFloat()))
    }

    fun onAiResultReceived(predictedClass: String, confidence: Double) {
        // Tampilkan indikasi "Uncertain" di UI jika di bawah threshold
        // Issue #30 — pakai konstanta CONFIDENCE_THRESHOLD, bukan hardcode 0.85
        val isUncertain = confidence < com.csm.kitchenguard.data.local.entity.enums.FreshnessClass.CONFIDENCE_THRESHOLD
        _uiState.update { 
            it.copy(
                aiClass = predictedClass, 
                aiConfidence = confidence,
                isAiUncertain = isUncertain
            ) 
        }
    }

    // ── Eksekusi Submit ───────────────────────────────────────────────────────

    fun submitWaste() {
        val state = _uiState.value
        
        if (state.selectedIngredient == null) {
            _uiState.update { it.copy(errorMessage = "Bahan harus dipilih") }
            return
        }

        val qtyDouble = state.quantityInput.toDoubleOrNull()
        if (qtyDouble == null || qtyDouble <= 0) {
            _uiState.update { it.copy(errorMessage = "Kuantitas tidak valid") }
            return
        }

        if (state.selectedReason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Alasan waste wajib diisi") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            
            // Ambil Shift ID aktif saat ini (karena form dijalankan tanpa parameter shift di UI)
            val shift = shiftDao.getActiveShiftOnce()
            if (shift == null) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = "Tidak ada shift aktif.") }
                return@launch
            }

            // Issue #22 — Validasi batch wajib untuk alasan SPOILED/EXPIRED.
            // Batch diperlukan agar server bisa menghitung selisih stok dengan benar.
            val requiresBatch = state.selectedReason in listOf("SPOILED", "EXPIRED")
            if (requiresBatch && state.selectedBatch == null) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Untuk alasan \"${state.selectedReason}\", batch bahan wajib dipilih " +
                            "agar selisih stok dapat dihitung dengan benar."
                    )
                }
                return@launch
            }

            val result = createWasteRecordUseCase(
                ingredientId = state.selectedIngredient.id,
                batchId = state.selectedBatch?.id,
                quantity = qtyDouble,
                unit = state.selectedUnit,
                reason = state.selectedReason,
                shiftId = shift.shiftId,
                stationId = shift.stationId,
                aiClass = state.aiClass,
                aiConfidence = state.aiConfidence,
                // Issue #27 — gunakan konstanta MODEL_VERSION agar tidak hardcode string duplikat.
                aiModelVersion = if (state.aiClass != null) FreshnessClassifier.MODEL_VERSION else null,
                ocrRawText = if (state.ocrWeight != null) "${state.ocrWeight} ${state.selectedUnit}" else null,
                ocrConfidence = state.ocrConfidence?.toDouble(),
                photoPath = state.photoUri
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isSubmitting = false, submitSuccess = true) }
                // Trigger Immediate Sync agar data langsung diunggah tanpa menunggu 15 menit
                SyncScheduler.triggerImmediateSync(getApplication())
            } else {
                _uiState.update { 
                    it.copy(isSubmitting = false, errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }
}
