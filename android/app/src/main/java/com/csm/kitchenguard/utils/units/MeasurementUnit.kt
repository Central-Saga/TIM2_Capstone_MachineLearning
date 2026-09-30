package com.csm.kitchenguard.utils.units

/**
 * Kategori satuan (PRD Section 13).
 *
 * Konversi HANYA boleh dilakukan di dalam kategori yang sama
 * (mis. g↔kg, ml↔liter). Cross-category (g↔ml) tidak valid.
 */
enum class UnitCategory {
    /** Massa: g, kg. */
    MASS,

    /** Volume: ml, liter. */
    VOLUME,

    /** Hitungan: pcs. */
    COUNT
}

/**
 * Satuan yang didukung untuk input/display (PRD Section 13).
 *
 * @param symbol     simbol yang dipakai di UI & payload.
 * @param category   kategori satuan.
 * @param toBaseFactor faktor konversi ke satuan dasar kategori.
 *        Satuan dasar: MASS = gram, VOLUME = mililiter, COUNT = pcs.
 *        Contoh: kg → 1000 (1 kg = 1000 g), liter → 1000 (1 liter = 1000 ml).
 */
enum class MeasurementUnit(
    val symbol: String,
    val category: UnitCategory,
    val toBaseFactor: Double
) {
    GRAM("g", UnitCategory.MASS, 1.0),
    KILOGRAM("kg", UnitCategory.MASS, 1000.0),
    MILLILITER("ml", UnitCategory.VOLUME, 1.0),
    LITER("liter", UnitCategory.VOLUME, 1000.0),
    PIECES("pcs", UnitCategory.COUNT, 1.0);

    companion object {
        /**
         * Mencari satuan berdasarkan simbol (case-insensitive).
         * Menerima alias umum: "gram"→g, "kilogram"→kg, "l"/"ltr"→liter, "pcs"/"pc"→pcs.
         *
         * @return [MeasurementUnit] atau null bila tidak dikenal.
         */
        fun fromSymbol(symbol: String): MeasurementUnit? {
            val normalized = symbol.trim().lowercase()
            return when (normalized) {
                "g", "gram", "gr" -> GRAM
                "kg", "kilogram", "kilo" -> KILOGRAM
                "ml", "mililiter", "milliliter" -> MILLILITER
                "l", "ltr", "liter", "litre" -> LITER
                "pcs", "pc", "piece", "pieces", "buah", "unit" -> PIECES
                else -> entries.firstOrNull { it.symbol == normalized }
            }
        }

        /** Seluruh satuan dalam kategori tertentu. */
        fun inCategory(category: UnitCategory): List<MeasurementUnit> =
            entries.filter { it.category == category }
    }
}
