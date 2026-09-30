package com.csm.kitchenguard.domain.usecase

import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.data.local.entity.IngredientEntity
import com.csm.kitchenguard.data.local.entity.enums.FreshnessClass
import com.csm.kitchenguard.domain.repository.WasteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit test untuk [CreateWasteRecordUseCase].
 * PRD Section 12 & 16 — Validasi input, idempotency, dan AI Confidence Gate.
 */
class CreateWasteRecordUseCaseTest {

    // System Under Test
    private lateinit var useCase: CreateWasteRecordUseCase

    // Mocked Dependencies
    private val wasteRepository: WasteRepository = mockk()
    private val masterDataDao: MasterDataDao = mockk()

    private val dummyIngredient = IngredientEntity(
        id = 1L, name = "Daging Sapi", category = "Meat", canonicalUnit = "kg", minimumStock = 0.5
    )

    @Before
    fun setUp() {
        useCase = CreateWasteRecordUseCase(wasteRepository, masterDataDao)

        // Stub default: masterDataDao mengembalikan bahan dummy
        coEvery { masterDataDao.getIngredientById(any()) } returns dummyIngredient
        // Stub default: repository selalu sukses menyimpan
        coEvery { wasteRepository.saveWasteRecord(any()) } returns Result.success(Unit)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Validasi Input
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN quantity is zero WHEN invoke THEN returns failure with correct message`() = runTest {
        val result = callUseCase(quantity = 0.0)

        assertTrue("Harus failure jika quantity = 0", result.isFailure)
        assertEquals(
            "Kuantitas waste harus lebih besar dari 0.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `GIVEN quantity is negative WHEN invoke THEN returns failure`() = runTest {
        val result = callUseCase(quantity = -1.5)

        assertTrue("Harus failure jika quantity negatif", result.isFailure)
    }

    @Test
    fun `GIVEN valid quantity WHEN invoke THEN returns success`() = runTest {
        val result = callUseCase(quantity = 1.5)

        assertTrue("Harus sukses jika quantity valid", result.isSuccess)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Format UUID & Idempotency Key
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN valid input WHEN invoke THEN idempotencyKey saved starts with waste prefix`() = runTest {
        callUseCase(quantity = 1.0)

        // Verifikasi bahwa saveWasteRecord dipanggil dengan idempotencyKey berformat "waste:<UUID>"
        coVerify {
            wasteRepository.saveWasteRecord(
                match { record ->
                    record.idempotencyKey.startsWith("waste:") &&
                    record.idempotencyKey == "waste:${record.clientUuid}"
                }
            )
        }
    }

    @Test
    fun `GIVEN valid input WHEN invoke twice THEN two unique clientUuids are generated`() = runTest {
        var firstUuid = ""
        var secondUuid = ""

        coEvery { wasteRepository.saveWasteRecord(any()) } answers {
            if (firstUuid.isEmpty()) firstUuid = firstArg<com.csm.kitchenguard.data.local.entity.WasteRecordEntity>().clientUuid
            else secondUuid = firstArg<com.csm.kitchenguard.data.local.entity.WasteRecordEntity>().clientUuid
            Result.success(Unit)
        }

        callUseCase(quantity = 1.0)
        callUseCase(quantity = 2.0)

        assertNotEquals("Setiap pemanggilan harus menghasilkan UUID unik", firstUuid, secondUuid)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 3: AI Confidence Gate (PRD Section 16)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN aiClass SPOILED and confidence 0_94 WHEN invoke THEN aiClass stays SPOILED`() = runTest {
        callUseCase(quantity = 1.0, aiClass = "SPOILED", aiConfidence = 0.94)

        coVerify {
            wasteRepository.saveWasteRecord(
                match { record ->
                    // Confidence 0.94 >= threshold 0.85 → kelas tetap SPOILED
                    record.aiClass == "SPOILED"
                }
            )
        }
    }

    @Test
    fun `GIVEN aiClass SPOILED and confidence 0_70 WHEN invoke THEN aiClass becomes UNCERTAIN`() = runTest {
        // Confidence 0.70 < threshold 0.85 → harus dipaksa menjadi UNCERTAIN
        callUseCase(quantity = 1.0, aiClass = "SPOILED", aiConfidence = 0.70)

        coVerify {
            wasteRepository.saveWasteRecord(
                match { record ->
                    record.aiClass == FreshnessClass.UNCERTAIN.name
                }
            )
        }
    }

    @Test
    fun `GIVEN aiClass at exact threshold 0_85 WHEN invoke THEN aiClass is preserved`() = runTest {
        callUseCase(quantity = 1.0, aiClass = "ACCEPTABLE", aiConfidence = 0.85)

        coVerify {
            wasteRepository.saveWasteRecord(
                match { record ->
                    // Tepat di threshold harus diterima (>= bukan >)
                    record.aiClass == "ACCEPTABLE"
                }
            )
        }
    }

    @Test
    fun `GIVEN no aiClass provided WHEN invoke THEN aiClass saved as null`() = runTest {
        callUseCase(quantity = 1.0, aiClass = null, aiConfidence = null)

        coVerify {
            wasteRepository.saveWasteRecord(
                match { record -> record.aiClass == null }
            )
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────────────────

    private suspend fun callUseCase(
        ingredientId: Long = 1L,
        batchId: Long? = null,
        quantity: Double = 1.5,
        unit: String = "kg",
        reason: String = "TRIM",
        shiftId: Long = 101L,
        stationId: Long = 1L,
        aiClass: String? = null,
        aiConfidence: Double? = null,
        aiModelVersion: String? = null,
        ocrRawText: String? = null,
        ocrConfidence: Double? = null,
        photoPath: String? = null
    ) = useCase(
        ingredientId, batchId, quantity, unit, reason, shiftId, stationId,
        aiClass, aiConfidence, aiModelVersion, ocrRawText, ocrConfidence, photoPath
    )
}
