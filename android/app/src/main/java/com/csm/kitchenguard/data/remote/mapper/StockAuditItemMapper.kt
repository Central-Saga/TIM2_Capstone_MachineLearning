package com.csm.kitchenguard.data.remote.mapper

import com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity
import com.csm.kitchenguard.data.remote.dto.StockAuditItemDto

/**
 * Mapper murni dari [StockAuditDraftEntity] ke [StockAuditItemDto].
 *
 * Issue #13 — Integritas payload audit:
 * `batchId` dan `unit` HARUS diambil dari draft yang tersimpan, bukan di-hardcode.
 * Nilai yang salah membuat kalkulasi variance di server tidak akurat.
 */
object StockAuditItemMapper {

    /**
     * Memetakan satu draft audit menjadi item request.
     *
     * - `batchId` diteruskan apa adanya (termasuk null bila audit level ingredient).
     * - `unit` diteruskan dari denormalisasi satuan pada draft.
     */
    fun toDto(draft: StockAuditDraftEntity): StockAuditItemDto = StockAuditItemDto(
        ingredientId = draft.ingredientId,
        batchId = draft.batchId,
        actualPhysical = draft.actualPhysical,
        unit = draft.unit
    )

    /** Memetakan seluruh draft menjadi daftar item request. */
    fun toDtoList(drafts: List<StockAuditDraftEntity>): List<StockAuditItemDto> =
        drafts.map { toDto(it) }
}
