package com.csm.kitchenguard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Model request dan response untuk API Batch Sync (Push dari lokal ke server).
 * PRD Section 21 — Push Sync Batch.
 * Endpoint: POST /api/v1/sync/batch
 */

data class SyncBatchRequest(
    @SerializedName("items") val items: List<SyncItemDto>
)

data class SyncItemDto(
    @SerializedName("client_uuid")     val clientUuid: String,
    @SerializedName("idempotency_key") val idempotencyKey: String,
    @SerializedName("entity_type")     val entityType: String,
    @SerializedName("operation")       val operation: String,
    @SerializedName("payload_json")    val payloadJson: String,
    @SerializedName("client_event_at") val clientEventAt: String
)

data class SyncBatchResponse(
    @SerializedName("results") val results: List<SyncResultDto>
)

data class SyncResultDto(
    @SerializedName("client_uuid") val clientUuid: String,
    @SerializedName("status")      val status: String,    // "SYNCED", "CONFLICT", "FAILED_VALIDATION", "ALREADY_PROCESSED"
    @SerializedName("server_id")   val serverId: Long?,   // Nullable karena jika conflict mungkin tidak disave
    @SerializedName("message")     val message: String?
)
