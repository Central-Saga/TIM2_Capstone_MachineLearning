package com.csm.kitchenguard.domain.model

/**
 * Definisi peran pengguna dalam sistem KitchenGuard CSM.
 * PRD Section 9 — Authentication & Authorization.
 *
 * Issue #26 — Role disimpan di sesi tetapi sebelumnya tidak pernah dipakai
 * untuk membatasi akses. Class ini mendefinisikan hierarki peran dan
 * helper untuk pemeriksaan hak akses.
 */
enum class UserRole(val value: String) {
    /** Staf dapur — akses penuh ke pencatatan waste dan audit shift sendiri. */
    STAFF("STAFF"),

    /** Supervisor dapur — akses penuh termasuk melihat laporan semua stasiun. */
    SUPERVISOR("SUPERVISOR"),

    /** Head Chef — akses penuh + persetujuan konflik sync dan laporan manajemen. */
    HEAD_CHEF("HEAD_CHEF"),

    /** Role tidak dikenal / belum login. Tidak boleh mengakses fitur apa pun. */
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromValue(value: String): UserRole =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN

        /**
         * Mengembalikan true bila role ini memiliki hak minimum STAFF.
         * Semua pengguna yang sudah login seharusnya memenuhi syarat ini.
         */
        fun UserRole.canAccessKitchenFeatures(): Boolean =
            this in listOf(STAFF, SUPERVISOR, HEAD_CHEF)

        /**
         * Mengembalikan true bila role ini berhak melihat laporan lintas stasiun
         * dan menyetujui konflik sync.
         */
        fun UserRole.canManageConflicts(): Boolean =
            this in listOf(SUPERVISOR, HEAD_CHEF)

        /**
         * Mengembalikan true bila role ini berhak mengakses menu manajemen /
         * konfigurasi sistem (PRD: admin-level features).
         */
        fun UserRole.isAdminLevel(): Boolean =
            this == HEAD_CHEF
    }
}
