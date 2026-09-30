package com.csm.kitchenguard.domain.usecase

import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import com.csm.kitchenguard.data.local.entity.enums.FreshnessClass
import com.csm.kitchenguard.data.local.entity.enums.WasteReason
import com.csm.kitchenguard.domain.repository.WasteRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Use case utama untuk merekam aktivitas Waste Logging.
 * PRD Section 12 & 16.
 * Mengenkapsulasi aturan validasi fisik dan AI sebelum data disimpan secara offline-first.
 */
class CreateWasteRecordUseCase(
    private val wasteRepository: WasteRepository,
    private val masterDataDao: MasterDataDao
) {

    /** Format ISO-8601 UTC untuk standarisasi timestamp di server. */
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend operator fun invoke(
        ingredientId: Long,
        batchId: Long?,
        quantity: Double,
        unit: String,
        reason: String,
        shiftId: Long,
        stationId: Long,
        aiClass: String?,
        aiConfidence: Double?,
        aiModelVersion: String?,
        ocrRawText: String?,
        ocrConfidence: Double?,
        photoPath: String?
    ): Result<Unit> {
        try {
            // 1. Validasi Dasar
            if (quantity <= 0) {
                return Result.failure(Exception("Kuantitas waste harus lebih besar dari 0."))
            }

            // 2. Validasi Konteks Batch (PRD Section 14).
            //    Alasan SPOILED/EXPIRED mewajibkan pemilihan batch JIKA bahan
            //    memiliki batch aktif. Perbandingan memakai enum [WasteReason],
            //    bukan string literal (Issue #22).
            val wasteReason = WasteReason.fromValue(reason)
            val requiresBatch = wasteReason == WasteReason.SPOILED ||
                wasteReason == WasteReason.EXPIRED

            if (requiresBatch) {
                val activeBatches = masterDataDao.getActiveBatchesOnce(ingredientId)
                if (activeBatches.isNotEmpty()) {
                    if (batchId == null) {
                        return Result.failure(
                            Exception(
                                "Batch wajib dipilih untuk alasan ${wasteReason.value} " +
                                    "karena bahan ini memiliki batch aktif."
                            )
                        )
                    }
                    // Batch yang dipilih harus benar-benar milik bahan ini & aktif.
                    val isKnownBatch = activeBatches.any { it.id == batchId }
                    if (!isKnownBatch) {
                        return Result.failure(
                            Exception(
                                "Batch #$batchId tidak valid untuk bahan ini " +
                                    "atau sudah tidak aktif."
                            )
                        )
                    }
                }
            }

            // 3. AI Freshness Confidence Gate (PRD Section 16)
            var finalAiClass = aiClass
            if (aiClass != null && aiConfidence != null) {
                if (aiConfidence < FreshnessClass.CONFIDENCE_THRESHOLD) {
                    finalAiClass = FreshnessClass.UNCERTAIN.name
                }
            }

            // 4. Denormalisasi Nama Bahan (Untuk preview Offline di UI)
            val ingredient = masterDataDao.getIngredientById(ingredientId)
            val ingredientName = ingredient?.name ?: "Unknown Ingredient"

            // 5. Generate Idempotency & UUID
            val clientUuid = UUID.randomUUID().toString()
            val idempotencyKey = "waste:$clientUuid"
            val clientEventAt = isoFormat.format(Date())

            // 6. Bentuk Entitas
            val wasteRecord = WasteRecordEntity(
                clientUuid = clientUuid,
                idempotencyKey = idempotencyKey,
                shiftId = shiftId,
                stationId = stationId,
                ingredientId = ingredientId,
                ingredientName = ingredientName,
                batchId = batchId,
                quantity = quantity,
                unit = unit,
                reason = reason,
                photoPath = photoPath,
                aiClass = finalAiClass,
                aiConfidence = aiConfidence,
                aiModelVersion = aiModelVersion,
                ocrRawText = ocrRawText,
                ocrConfidence = ocrConfidence,
                clientEventAt = clientEventAt,
                syncStatus = "PENDING"
            )

            // 7. Simpan via Repository (Offline-First: Simpan + Enqueue)
            return wasteRepository.saveWasteRecord(wasteRecord)

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
