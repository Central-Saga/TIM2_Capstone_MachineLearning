package com.csm.kitchenguard.utils.units

/**
 * Utilitas konversi satuan untuk **keperluan display saja** (PRD Section 13).
 *
 * PENTING:
 * - Nilai baku (canonical unit) dan kalkulasi loss finansial dihitung & divalidasi
 *   oleh backend (PostgreSQL). Util ini TIDAK menggantikan logika server.
 * - Konversi HANYA berlaku di dalam kategori yang sama (g↔kg, ml↔liter).
 *   Konversi lintas kategori (mis. g↔ml) ditolak.
 */
object UnitConverter {

    /**
     * Mengonversi [value] dari [from] ke [to].
     *
     * @return [Result.success] berisi nilai hasil konversi, atau
     *         [Result.failure] bila satuan tidak dikenal / beda kategori.
     */
    fun convert(value: Double, from: MeasurementUnit, to: MeasurementUnit): Result<Double> {
        if (from.category != to.category) {
            return Result.failure(
                IllegalArgumentException(
                    "Tidak dapat mengonversi '${from.symbol}' ke '${to.symbol}': " +
                        "kategori berbeda (${from.category} vs ${to.category})."
                )
            )
        }
        // Konversi via satuan dasar: value → base → target.
        val baseValue = value * from.toBaseFactor
        return Result.success(baseValue / to.toBaseFactor)
    }

    /**
     * Mengonversi [value] dari [from] ke [to] berdasarkan SIMBOL satuan.
     * Berguna saat input berasal dari UI/DB berupa String.
     */
    fun convert(value: Double, fromSymbol: String, toSymbol: String): Result<Double> {
        val from = MeasurementUnit.fromSymbol(fromSymbol)
            ?: return Result.failure(IllegalArgumentException("Satuan asal tidak dikenal: '$fromSymbol'"))
        val to = MeasurementUnit.fromSymbol(toSymbol)
            ?: return Result.failure(IllegalArgumentException("Satuan tujuan tidak dikenal: '$toSymbol'"))
        return convert(value, from, to)
    }

    /**
     * Mengembalikan daftar satuan yang VALID dipilih untuk sebuah canonical unit.
     *
     * Contoh: canonical "kg" → [kg, g] (agar user bisa input gram).
     * Bila canonical tidak dikenal, kembalikan daftar berisi canonical itu sendiri
     * (fallback aman: tidak menawarkan konversi yang tidak bisa dihitung).
     */
    fun availableUnitsFor(canonicalUnit: String): List<MeasurementUnit> {
        val canonical = MeasurementUnit.fromSymbol(canonicalUnit)
            ?: return emptyList()
        return MeasurementUnit.inCategory(canonical.category)
    }

    /**
     * Menghitung nilai tampilan dalam satuan [displayUnit] dari sebuah nilai
     * yang dinyatakan dalam [sourceUnit]. Bila konversi tidak memungkinkan
     * (beda kategori / tidak dikenal), mengembalikan nilai asli apa adanya
     * agar display tidak error — bukan mengubah makna data.
     */
    fun toDisplayValue(value: Double, sourceUnit: String, displayUnit: String): Double {
        return convert(value, sourceUnit, displayUnit).getOrDefault(value)
    }

    /**
     * Menghitung ulang nilai bila user mengubah satuan input dari [oldUnit] ke [newUnit].
     *
     * Karena nilai input adalah angka yang diketik user, mengubah satuan TIDAK
     * otomatis mengubah angka (user yang mengetik ulang). Util ini disediakan
     * bila UX ingin mempertahankan besar fisik, mis. 1 kg → 1000 g.
     *
     * @return [Result.success] nilai ekuivalen dalam [newUnit], atau failure.
     */
    fun rebaseValue(value: Double, oldUnit: String, newUnit: String): Result<Double> =
        convert(value, oldUnit, newUnit)

    /**
     * Memformat nilai untuk display beserta satuannya, membuang desimal tak perlu.
     *
     * Contoh: 1.5 → "1.5 kg", 1000.0 → "1000 g", 0.25 → "0.25 liter".
     */
    fun format(value: Double, unit: MeasurementUnit): String {
        val text = if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            // Locale.US eksplisit agar pemisah desimal selalu '.' — tidak
            // bergantung locale perangkat (mis. '1,5' di locale Indonesia).
            java.lang.String.format(java.util.Locale.US, "%.4f", value)
                .trimEnd('0')
                .trimEnd('.')
        }
        return "$text ${unit.symbol}"
    }
}
