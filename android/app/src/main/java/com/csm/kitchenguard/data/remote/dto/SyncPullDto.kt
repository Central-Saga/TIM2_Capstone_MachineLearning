package com.csm.kitchenguard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Model request dan response untuk API Pull Sync (Master Data dari server ke lokal).
 * PRD Section 22 — Pull Sync Batch.
 * Endpoint: POST /api/v1/sync/pull
 */

data class SyncPullRequest(
    @SerializedName("last_sync_cursor") val lastSyncCursor: Long? // epoch timestamp atau revision counter
)

data class SyncPullResponse(
    @SerializedName("next_sync_cursor") val nextSyncCursor: Long,
    @SerializedName("ingredients")      val ingredients: List<IngredientDto>,
    @SerializedName("batches")          val batches: List<BatchDto>,
    @SerializedName("active_shift")     val activeShift: ShiftDto?,
    @SerializedName("notifications")    val notifications: List<NotificationDto>?
)

data class IngredientDto(
    @SerializedName("id")             val id: Long,
    @SerializedName("name")           val name: String,
    @SerializedName("category")       val category: String,
    @SerializedName("canonical_unit") val canonicalUnit: String,
    @SerializedName("minimum_stock")  val minimumStock: Double
)

data class BatchDto(
    @SerializedName("id")                 val id: Long,
    @SerializedName("ingredient_id")      val ingredientId: Long,
    @SerializedName("batch_code")         val batchCode: String,
    @SerializedName("expiry_at")          val expiryAt: String,
    @SerializedName("remaining_quantity") val remainingQuantity: Double,
    @SerializedName("status")             val status: String
)

data class ShiftDto(
    @SerializedName("id")             val id: Long,
    @SerializedName("station_id")     val stationId: Long,
    @SerializedName("station_name")   val stationName: String,
    @SerializedName("started_at")     val startedAt: String,
    @SerializedName("server_version") val serverVersion: Int,
    @SerializedName("is_locked")      val isLocked: Boolean
)

data class NotificationDto(
    @SerializedName("id")           val id: Long,
    @SerializedName("type")         val type: String,
    @SerializedName("title")        val title: String,
    @SerializedName("message")      val message: String,
    @SerializedName("reference_id") val referenceId: String?,
    @SerializedName("created_at")   val createdAt: String
)
