package com.csm.kitchenguard.data.remote.mapper

import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Unit test untuk [SyncItemMapper].
 *
 * Issue #9 — memastikan `client_event_at` yang dikirim ke server berasal dari
 * waktu event asli, bukan nilai placeholder konstan.
 */
class SyncItemMapperTest {

    private fun queueEntity(
        clientEventAt: String? = null,
        createdAt: Long = 1_700_000_000_000L,
        idempotencyKey: String = "waste:c3a1b8e2-9f44-4e2b-b93d-8e42b10a2f91",
        entityType: String = "WASTE_RECORD"
    ) = SyncQueueEntity(
        id = 1L,
        clientUuid = "c3a1b8e2-9f44-4e2b-b93d-8e42b10a2f91",
        idempotencyKey = idempotencyKey,
        entityType = entityType,
        operation = "CREATE",
        payloadJson = "{}",
        clientEventAt = clientEventAt,
        createdAt = createdAt,
        errorMessage = null
    )

    private fun isoUtc(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(millis)

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Nilai asli dipakai apa adanya
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN clientEventAt present WHEN toDto THEN uses original value`() {
        val entity = queueEntity(clientEventAt = "2026-09-16T13:30:00Z")

        val dto = SyncItemMapper.toDto(entity)

        assertEquals("2026-09-16T13:30:00Z", dto.clientEventAt)
    }

    @Test
    fun `GIVEN clientEventAt present WHEN toDto THEN never uses placeholder`() {
        val entity = queueEntity(clientEventAt = "2026-09-16T13:30:00Z")

        val dto = SyncItemMapper.toDto(entity)

        assertNotEquals(
            "Placeholder lama tidak boleh muncul lagi",
            "2026-09-16T12:00:00Z",
            dto.clientEventAt
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Fallback jujur ke createdAt (data lama pre-migrasi)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN clientEventAt null WHEN toDto THEN falls back to createdAt`() {
        val createdAt = 1_700_000_000_000L
        val entity = queueEntity(clientEventAt = null, createdAt = createdAt)

        val dto = SyncItemMapper.toDto(entity)

        assertEquals(isoUtc(createdAt), dto.clientEventAt)
    }

    @Test
    fun `GIVEN clientEventAt blank WHEN toDto THEN falls back to createdAt`() {
        val createdAt = 1_700_000_000_000L
        val entity = queueEntity(clientEventAt = "   ", createdAt = createdAt)

        val dto = SyncItemMapper.toDto(entity)

        assertEquals(isoUtc(createdAt), dto.clientEventAt)
    }

    @Test
    fun `GIVEN fallback used WHEN toDto THEN result is valid ISO-8601`() {
        val entity = queueEntity(clientEventAt = null)

        val dto = SyncItemMapper.toDto(entity)

        // Harus bisa di-parse balik sebagai ISO-8601 UTC.
        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(dto.clientEventAt)
        assertNotNull("Fallback harus berupa ISO-8601 yang valid", parsed)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 3: Field lain tetap utuh
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN queue entity WHEN toDto THEN other fields mapped unchanged`() {
        val entity = queueEntity(clientEventAt = "2026-09-16T13:30:00Z")

        val dto = SyncItemMapper.toDto(entity)

        assertEquals(entity.clientUuid, dto.clientUuid)
        assertEquals(entity.idempotencyKey, dto.idempotencyKey)
        assertEquals(entity.entityType, dto.entityType)
        assertEquals(entity.operation, dto.operation)
        assertEquals(entity.payloadJson, dto.payloadJson)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 4: Integritas idempotencyKey (Issue #10)
    // Key yang dikirim HARUS identik dengan key yang di-enqueue — tidak
    // direkonstruksi dari clientUuid.
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN enqueued key WHEN toDto THEN sent key equals enqueued key`() {
        val storedKey = "waste:c3a1b8e2-9f44-4e2b-b93d-8e42b10a2f91"
        val entity = queueEntity(idempotencyKey = storedKey)

        val dto = SyncItemMapper.toDto(entity)

        assertEquals("Key harus dipakai apa adanya", storedKey, dto.idempotencyKey)
    }

    @Test
    fun `GIVEN non-waste key WHEN toDto THEN key is NOT reconstructed as waste prefix`() {
        // Kasus inti issue #10: entitas audit punya format key berbeda.
        // Kode lama akan menimpanya menjadi "waste:<clientUuid>".
        val auditKey = "audit-shift-42:c3a1b8e2-9f44-4e2b-b93d-8e42b10a2f91"
        val entity = queueEntity(
            idempotencyKey = auditKey,
            entityType = "AUDIT"
        )

        val dto = SyncItemMapper.toDto(entity)

        assertEquals(auditKey, dto.idempotencyKey)
        assertFalse(
            "Key audit tidak boleh direkonstruksi dengan prefix waste:",
            dto.idempotencyKey.startsWith("waste:")
        )
    }

    @Test
    fun `GIVEN key with custom format WHEN toDto THEN not derived from clientUuid`() {
        // Key sengaja BUKAN turunan dari clientUuid, untuk membuktikan
        // mapper tidak membangun ulang dari clientUuid.
        val customKey = "custom-idem-key-xyz"
        val entity = queueEntity(idempotencyKey = customKey)

        val dto = SyncItemMapper.toDto(entity)

        assertEquals(customKey, dto.idempotencyKey)
        assertNotEquals(
            "Key tidak boleh hasil rekonstruksi dari clientUuid",
            "waste:${entity.clientUuid}",
            dto.idempotencyKey
        )
    }
}
