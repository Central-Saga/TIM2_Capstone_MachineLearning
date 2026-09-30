package com.csm.kitchenguard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Model request dan response untuk API Submit Physical Stock Audit.
 * PRD Section 24 — Physical Stock Audit.
 * Endpoint: POST /api/v1/stock-audits
 */

data class StockAuditRequest(
    @SerializedName("shift_id")   val shiftId: Long,
    @SerializedName("station_id") val stationId: Long,
    @SerializedName("items")      val items: List<StockAuditItemDto>
)

data class StockAuditItemDto(
    @SerializedName("ingredient_id")   val ingredientId: Long,
    @SerializedName("batch_id")        val batchId: Long?,
    @SerializedName("actual_physical") val actualPhysical: Double,
    @SerializedName("unit")            val unit: String
)

data class StockAuditResponse(
    @SerializedName("audit_id") val auditId: Long,
    @SerializedName("success")  val success: Boolean,
    @SerializedName("message")  val message: String?
)
