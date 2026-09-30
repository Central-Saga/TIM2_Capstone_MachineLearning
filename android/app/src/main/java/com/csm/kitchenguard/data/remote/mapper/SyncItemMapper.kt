package com.csm.kitchenguard.data.remote.mapper

import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import com.csm.kitchenguard.data.remote.dto.SyncItemDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Mapper murni dari [SyncQueueEntity] ke [SyncItemDto] (payload jaringan).
 *
 * Dipisahkan dari [com.csm.kitchenguard.data.repository.SyncRepositoryImpl] agar
 * logika resolusi `clientEventAt` dapat diuji tanpa Android/network runtime.
 *
 * Issue #9 — Integritas `clientEventAt`:
 * Nilai yang dikirim ke server HARUS berasal dari waktu event asli yang tercatat
 * di antrean ([SyncQueueEntity.clientEventAt]), BUKAN nilai placeholder konstan.
 */
object SyncItemMapper {

    /** Format ISO-8601 UTC yang dipakai pada field timestamp string. */
    private fun isoUtc(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(millis))

    /**
     * Menentukan timestamp event yang akan dikirim ke server.
     *
     * Prioritas:
     * 1. [SyncQueueEntity.clientEventAt] — waktu event asli dari entitas sumber.
     * 2. Bila kosong/null (mis. data lama sebelum migrasi v2), turunkan dari
     *    [SyncQueueEntity.createdAt] sebagai fallback yang jujur — tetap waktu
     *    nyata dari antrean, bukan nilai konstan karangan.
     *
     * @return string ISO-8601 UTC yang siap dipakai pada `client_event_at`.
     */
    fun resolveClientEventAt(entity: SyncQueueEntity): String {
        val original = entity.clientEventAt?.trim()
        return if (!original.isNullOrEmpty()) {
            original
        } else {
            isoUtc(entity.createdAt)
        }
    }

    /**
     * Melakukan mapping lengkap satu item antrean menjadi DTO jaringan.
     * `idempotencyKey` diambil dari entitas (tidak boleh berubah saat retry).
     */
    fun toDto(entity: SyncQueueEntity): SyncItemDto = SyncItemDto(
        clientUuid = entity.clientUuid,
        idempotencyKey = entity.idempotencyKey,
        entityType = entity.entityType,
        operation = entity.operation,
        payloadJson = entity.payloadJson,
        clientEventAt = resolveClientEventAt(entity)
    )
}
