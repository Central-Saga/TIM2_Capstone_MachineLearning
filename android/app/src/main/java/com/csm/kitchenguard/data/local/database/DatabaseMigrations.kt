package com.csm.kitchenguard.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Kumpulan migrasi skema [AppDatabase].
 *
 * PRD Section 18 — Offline Local Database.
 */
object DatabaseMigrations {

    /**
     * v1 → v2 (Issue #9).
     *
     * Menambahkan kolom `client_event_at` pada tabel `sync_queue` agar timestamp
     * event asli ikut tersimpan di antrean dan tidak hilang sebelum dikirim ke
     * server. Kolom bersifat NULLABLE — baris lama akan berisi NULL dan ditangani
     * oleh fallback di lapisan mapper.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE sync_queue ADD COLUMN client_event_at TEXT"
            )
        }
    }

    /** Seluruh migrasi yang harus didaftarkan ke Room. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
