package com.kitchenguard.csm.model

import com.google.gson.annotations.SerializedName

// Barcode Models
data class BarcodeRequest(
    @SerializedName("barcode_value") val barcodeValue: String,
    @SerializedName("detected_format") val detectedFormat: String = "EAN_13"
)

data class BarcodeResponse(
    @SerializedName("status") val status: String,
    @SerializedName("barcode") val barcode: BarcodeMeta?,
    @SerializedName("ingredient") val ingredient: IngredientData?,
    @SerializedName("fallback_required") val fallbackRequired: Boolean
)

data class BarcodeMeta(
    @SerializedName("value") val value: String,
    @SerializedName("format") val format: String,
    @SerializedName("latency_ms") val latencyMs: Double
)

data class IngredientData(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("batch_id") val batchId: String,
    @SerializedName("supplier") val supplier: String,
    @SerializedName("base_unit") val baseUnit: String,
    @SerializedName("expiry_at") val expiryAt: String,
    @SerializedName("storage_location") val storageLocation: String
)

// Vision Ingredient & Freshness Models
data class VisionRequest(
    @SerializedName("image_base64") val imageBase64: String? = null,
    @SerializedName("ingredient_hint") val ingredientHint: String? = null,
    @SerializedName("threshold") val threshold: Double = 0.85
)

data class VisionResponse(
    @SerializedName("ai") val ai: VisionAIMeta,
    @SerializedName("ingredient") val ingredient: VisionIngredientMeta,
    @SerializedName("visual_analysis") val visualAnalysis: VisualAnalysisMeta,
    @SerializedName("sop_action") val sopAction: String
)

data class VisionAIMeta(
    @SerializedName("predicted_class") val predictedClass: String,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("gate_status") val gateStatus: String,
    @SerializedName("model_version") val modelVersion: String,
    @SerializedName("inference_time_ms") val inferenceTimeMs: Double
)

data class VisionIngredientMeta(
    @SerializedName("key") val key: String,
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("storage_temp") val storageTemp: String
)

data class VisualAnalysisMeta(
    @SerializedName("blemish_percentage") val blemishPercentage: Double,
    @SerializedName("estimated_shelf_life_days") val estimatedShelfLifeDays: Int,
    @SerializedName("visual_evidence") val visualEvidence: String
)

// OCR Scale Models
data class OCRScaleRequest(
    @SerializedName("scale_value_hint") val scaleValueHint: String = "1.45 kg"
)

data class OCRScaleResponse(
    @SerializedName("status") val status: String,
    @SerializedName("ocr") val ocr: OCRMeta
)

data class OCRMeta(
    @SerializedName("raw_text") val rawText: String,
    @SerializedName("weight") val weight: Double,
    @SerializedName("unit") val unit: String,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("auto_filled") val autoFilled: Boolean
)

// Waste Logging & Financial Impact Models
data class WasteLogSubmissionRequest(
    @SerializedName("barcode_value") val barcodeValue: String? = null,
    @SerializedName("ingredient_name") val ingredientName: String? = null,
    @SerializedName("batch_id") val batchId: String? = null,
    @SerializedName("ocr_weight") val ocrWeight: Double? = null,
    @SerializedName("ocr_unit") val ocrUnit: String = "kg",
    @SerializedName("ocr_confidence") val ocrConfidence: Double? = null,
    @SerializedName("note") val note: String,
    @SerializedName("reported_by") val reportedBy: String = "Staff Dapur"
)

data class WasteLogSubmissionResponse(
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("reported_by") val reportedBy: String?,
    @SerializedName("ai") val ai: WasteAIMeta,
    @SerializedName("action_recommendation") val actionRecommendation: String,
    @SerializedName("financial_impact") val financialImpact: FinancialImpactData?
)

data class WasteAIMeta(
    @SerializedName("task") val task: String,
    @SerializedName("class") val predictedClass: String,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("gate_status") val gateStatus: String,
    @SerializedName("model_version") val modelVersion: String
)

data class FinancialImpactData(
    @SerializedName("category") val category: String,
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("cost_per_kg_rupiah") val costPerKgRupiah: Double,
    @SerializedName("total_loss_rupiah") val totalLossRupiah: Double,
    @SerializedName("priority_level") val priorityLevel: String,
    @SerializedName("action_recommendation") val actionRecommendation: String
)
