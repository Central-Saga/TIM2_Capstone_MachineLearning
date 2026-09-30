package com.csm.kitchenguard.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.csm.kitchenguard.BuildConfig
import com.csm.kitchenguard.data.local.dao.MasterDataDao
import com.csm.kitchenguard.data.local.dao.NotificationDao
import com.csm.kitchenguard.data.local.dao.ShiftDao
import com.csm.kitchenguard.data.local.dao.StockAuditDao
import com.csm.kitchenguard.data.local.dao.SyncQueueDao
import com.csm.kitchenguard.data.local.dao.WasteDao
import com.csm.kitchenguard.data.local.entity.ActiveShiftEntity
import com.csm.kitchenguard.data.local.entity.BatchEntity
import com.csm.kitchenguard.data.local.entity.IngredientEntity
import com.csm.kitchenguard.data.local.entity.NotificationEntity
import com.csm.kitchenguard.data.local.entity.StockAuditDraftEntity
import com.csm.kitchenguard.data.local.entity.SyncQueueEntity
import com.csm.kitchenguard.data.local.entity.UserEntity
import com.csm.kitchenguard.data.local.entity.WasteRecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Konfigurasi database Room utama KitchenGuard CSM.
 *
 * Mendaftarkan seluruh 8 entity lokal untuk penyimpanan offline-first.
 * PRD Section 18 — Offline Local Database.
 *
 * Singleton: gunakan [AppDatabase.getInstance] untuk mendapatkan instance.
 * Nama file database: `kitchenguard.db`.
 *
 * Database version history:
 *   v1 — Initial schema (FASE 2 Tahap 07)
 *   v2 — Tambah kolom `sync_queue.client_event_at` untuk integritas timestamp
 *        event pada batch sync (Issue #9).
 */
@Database(
    entities = [
        UserEntity::class,
        ActiveShiftEntity::class,
        IngredientEntity::class,
        BatchEntity::class,
        WasteRecordEntity::class,
        SyncQueueEntity::class,
        StockAuditDraftEntity::class,
        NotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // ── DAO Accessors ─────────────────────────────────────────────────────────

    /** DAO untuk pencatatan dan sinkronisasi waste record. */
    abstract fun wasteDao(): WasteDao

    /** DAO untuk manajemen antrean sinkronisasi WorkManager. */
    abstract fun syncQueueDao(): SyncQueueDao

    /** DAO untuk konteks shift aktif. */
    abstract fun shiftDao(): ShiftDao

    /** DAO untuk cache master data bahan dan batch. */
    abstract fun masterDataDao(): MasterDataDao

    /** DAO untuk draft audit fisik stok. */
    abstract fun stockAuditDao(): StockAuditDao

    /** DAO untuk cache riwayat notifikasi (Notification Center). */
    abstract fun notificationDao(): NotificationDao

    // ── Singleton ─────────────────────────────────────────────────────────────

    companion object {

        /**
         * `@Volatile` memastikan perubahan [INSTANCE] langsung terlihat
         * oleh semua thread (tidak ter-cache di CPU register).
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Mengembalikan instance singleton [AppDatabase].
         * Menggunakan double-checked locking untuk thread safety:
         * - Pengecekan pertama (tanpa lock) untuk performa saat instance sudah ada.
         * - Pengecekan kedua (di dalam `synchronized`) untuk atomicity saat
         *   dua thread pertama kali membuat instance secara bersamaan.
         *
         * @param context Application context (bukan Activity context)
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "kitchenguard.db"
            ).addMigrations(*DatabaseMigrations.ALL)

            // Issue #25 — Seeder data dummy HANYA untuk DEBUG build.
            // Build release TIDAK boleh menyisipkan data palsu ke database produksi.
            if (BuildConfig.DEBUG) {
                builder.addCallback(SeedCallback)
            } else {
                Log.i("AppDatabase", "Release build: seed data dinonaktifkan.")
            }

            return builder.build()
        }
    }

    // ── Seeder Callback ───────────────────────────────────────────────────────

    /**
     * Callback yang dieksekusi HANYA saat database pertama kali dibuat
     * (fresh install atau data aplikasi dihapus).
     *
     * Menyisipkan data dummy master bahan dan batch agar UI memiliki
     * opsi bahan yang bisa dipilih saat pengujian offline tanpa koneksi ke server.
     *
     * CATATAN TEKNIS: Dalam `onCreate`, instance DAO belum tersedia karena
     * database masih dalam proses inisialisasi. Raw SQL via [SupportSQLiteDatabase]
     * adalah satu-satunya cara yang didukung Room di titik ini.
     */
    private object SeedCallback : RoomDatabase.Callback() {

        /** Format ISO-8601 UTC untuk field timestamp string di entity. */
        private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        /** Hitung ISO-8601 string untuk N hari dari sekarang. */
        private fun daysFromNow(days: Int): String {
            val millis = System.currentTimeMillis() + days.toLong() * 24 * 60 * 60 * 1000
            return isoFormat.format(Date(millis))
        }

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            seedIngredients(db)
            seedBatches(db)
        }

        // ── Seed Ingredients ──────────────────────────────────────────────────

        private fun seedIngredients(db: SupportSQLiteDatabase) {
            val ingredients = listOf(
                // id | name | category | canonical_unit | minimum_stock
                "(1, 'Daging Ayam Broiler', 'Meat & Butcher', 'kg', 5.0)",
                "(2, 'Beras Putih',         'Dry Goods',      'kg', 10.0)",
                "(3, 'Minyak Goreng',        'Pantry',         'liter', 5.0)",
                "(4, 'Tomat Segar',          'Produce',        'kg', 3.0)"
            )
            ingredients.forEach { values ->
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO ingredients
                        (id, name, category, canonical_unit, minimum_stock)
                    VALUES $values
                    """.trimIndent()
                )
            }
        }

        // ── Seed Batches ──────────────────────────────────────────────────────

        private fun seedBatches(db: SupportSQLiteDatabase) {
            // Tanggal expiry dihitung relatif dari waktu seeder berjalan
            val batches = listOf(
                // Ayam Broiler — 2 batch: satu hampir expired, satu aktif normal
                SeedBatch(1L, 1L, "BATCH-AY-001", daysFromNow(3),  12.5, "ACTIVE"),
                SeedBatch(2L, 1L, "BATCH-AY-002", daysFromNow(1),   4.0, "NEAR_EXPIRY"),
                // Beras Putih — 1 batch dengan stok besar
                SeedBatch(3L, 2L, "BATCH-BR-001", daysFromNow(30), 50.0, "ACTIVE"),
                // Minyak Goreng — 1 batch
                SeedBatch(4L, 3L, "BATCH-MY-001", daysFromNow(60), 20.0, "ACTIVE"),
                // Tomat Segar — 2 batch: satu hampir expired, satu masih aman
                SeedBatch(5L, 4L, "BATCH-TM-001", daysFromNow(2),   6.0, "NEAR_EXPIRY"),
                SeedBatch(6L, 4L, "BATCH-TM-002", daysFromNow(5),   8.0, "ACTIVE")
            )
            batches.forEach { b ->
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO batches
                        (id, ingredient_id, batch_code, expiry_at, remaining_quantity, status)
                    VALUES (${b.id}, ${b.ingredientId}, '${b.batchCode}',
                            '${b.expiryAt}', ${b.remainingQty}, '${b.status}')
                    """.trimIndent()
                )
            }
        }

        /** Helper data class untuk konstruksi data batch seeder. */
        private data class SeedBatch(
            val id: Long,
            val ingredientId: Long,
            val batchCode: String,
            val expiryAt: String,
            val remainingQty: Double,
            val status: String
        )
    }
}
