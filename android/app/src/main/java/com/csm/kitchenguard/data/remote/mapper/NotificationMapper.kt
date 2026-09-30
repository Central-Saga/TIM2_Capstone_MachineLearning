package com.csm.kitchenguard.data.remote.mapper

import com.csm.kitchenguard.data.local.entity.NotificationEntity
import com.csm.kitchenguard.data.remote.dto.NotificationDto

/**
 * Mapper murni dari [NotificationDto] (server) ke [NotificationEntity] (cache lokal).
 *
 * Issue #14 — cache notifikasi harus diisi dari hasil pull sync, bukan dibiarkan kosong.
 */
object NotificationMapper {

    /**
     * Memetakan satu notifikasi server menjadi entity cache.
     *
     * [NotificationEntity.isRead] default `false` (belum dibaca) karena status baca
     * dikelola lokal dan tidak dikirim oleh server pada pull sync.
     */
    fun toEntity(dto: NotificationDto): NotificationEntity = NotificationEntity(
        id = dto.id,
        type = dto.type,
        title = dto.title,
        message = dto.message,
        referenceId = dto.referenceId,
        isRead = false,
        createdAt = dto.createdAt
    )

    /** Memetakan daftar notifikasi server menjadi daftar entity cache. */
    fun toEntityList(dtos: List<NotificationDto>): List<NotificationEntity> =
        dtos.map { toEntity(it) }
}
