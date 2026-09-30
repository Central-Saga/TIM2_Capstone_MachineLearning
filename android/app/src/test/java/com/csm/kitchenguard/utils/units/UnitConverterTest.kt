package com.csm.kitchenguard.utils.units

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk [UnitConverter] (PRD Section 13 — Unit Conversion).
 *
 * Konversi hanya untuk display, dibatasi pada kategori yang sama.
 */
class UnitConverterTest {

    private val delta = 1e-9

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: Massa (g ↔ kg)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN 1 kg WHEN convert to g THEN 1000`() {
        val result = UnitConverter.convert(1.0, MeasurementUnit.KILOGRAM, MeasurementUnit.GRAM)

        assertEquals(1000.0, result.getOrThrow(), delta)
    }

    @Test
    fun `GIVEN 1500 g WHEN convert to kg THEN 1_5`() {
        val result = UnitConverter.convert(1500.0, MeasurementUnit.GRAM, MeasurementUnit.KILOGRAM)

        assertEquals(1.5, result.getOrThrow(), delta)
    }

    @Test
    fun `GIVEN 0 kg WHEN convert to g THEN 0`() {
        val result = UnitConverter.convert(0.0, MeasurementUnit.KILOGRAM, MeasurementUnit.GRAM)

        assertEquals(0.0, result.getOrThrow(), delta)
    }

    @Test
    fun `GIVEN same unit WHEN convert THEN value unchanged`() {
        val result = UnitConverter.convert(2.5, MeasurementUnit.KILOGRAM, MeasurementUnit.KILOGRAM)

        assertEquals(2.5, result.getOrThrow(), delta)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: Volume (ml ↔ liter)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN 1 liter WHEN convert to ml THEN 1000`() {
        val result = UnitConverter.convert(1.0, MeasurementUnit.LITER, MeasurementUnit.MILLILITER)

        assertEquals(1000.0, result.getOrThrow(), delta)
    }

    @Test
    fun `GIVEN 250 ml WHEN convert to liter THEN 0_25`() {
        val result = UnitConverter.convert(250.0, MeasurementUnit.MILLILITER, MeasurementUnit.LITER)

        assertEquals(0.25, result.getOrThrow(), delta)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 3: Cross-category ditolak
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN g WHEN convert to ml THEN failure`() {
        val result = UnitConverter.convert(100.0, MeasurementUnit.GRAM, MeasurementUnit.MILLILITER)

        assertTrue("g↔ml tidak valid (kategori berbeda)", result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `GIVEN kg WHEN convert to liter THEN failure`() {
        val result = UnitConverter.convert(1.0, MeasurementUnit.KILOGRAM, MeasurementUnit.LITER)

        assertTrue(result.isFailure)
    }

    @Test
    fun `GIVEN pcs WHEN convert to kg THEN failure`() {
        val result = UnitConverter.convert(3.0, MeasurementUnit.PIECES, MeasurementUnit.KILOGRAM)

        assertTrue(result.isFailure)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 4: pcs (COUNT) identity
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN pcs WHEN convert to pcs THEN unchanged`() {
        val result = UnitConverter.convert(7.0, MeasurementUnit.PIECES, MeasurementUnit.PIECES)

        assertEquals(7.0, result.getOrThrow(), delta)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 5: Symbol-aware API
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN symbol kg to g WHEN convert THEN 10`() {
        val result = UnitConverter.convert(0.01, "kg", "g")

        assertEquals(10.0, result.getOrThrow(), delta)
    }

    @Test
    fun `GIVEN unknown symbol WHEN convert THEN failure`() {
        val result = UnitConverter.convert(1.0, "xyz", "g")

        assertTrue(result.isFailure)
    }

    @Test
    fun `GIVEN alias gram WHEN fromSymbol THEN GRAM`() {
        assertEquals(MeasurementUnit.GRAM, MeasurementUnit.fromSymbol("gram"))
        assertEquals(MeasurementUnit.KILOGRAM, MeasurementUnit.fromSymbol("kilogram"))
        assertEquals(MeasurementUnit.LITER, MeasurementUnit.fromSymbol("l"))
        assertEquals(MeasurementUnit.PIECES, MeasurementUnit.fromSymbol("buah"))
    }

    @Test
    fun `GIVEN uppercase symbol WHEN fromSymbol THEN resolved`() {
        assertEquals(MeasurementUnit.KILOGRAM, MeasurementUnit.fromSymbol("KG"))
        assertEquals(MeasurementUnit.MILLILITER, MeasurementUnit.fromSymbol(" ML "))
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 6: availableUnitsFor
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN canonical kg WHEN availableUnitsFor THEN returns kg and g`() {
        val units = UnitConverter.availableUnitsFor("kg")

        assertEquals(2, units.size)
        assertTrue(units.contains(MeasurementUnit.KILOGRAM))
        assertTrue(units.contains(MeasurementUnit.GRAM))
    }

    @Test
    fun `GIVEN canonical liter WHEN availableUnitsFor THEN returns liter and ml not kg`() {
        val units = UnitConverter.availableUnitsFor("liter")

        assertTrue(units.contains(MeasurementUnit.LITER))
        assertTrue(units.contains(MeasurementUnit.MILLILITER))
        assertFalse("Volume tidak boleh menawarkan massa", units.contains(MeasurementUnit.KILOGRAM))
    }

    @Test
    fun `GIVEN canonical pcs WHEN availableUnitsFor THEN only pcs`() {
        val units = UnitConverter.availableUnitsFor("pcs")

        assertEquals(listOf(MeasurementUnit.PIECES), units)
    }

    @Test
    fun `GIVEN unknown canonical WHEN availableUnitsFor THEN empty`() {
        assertTrue(UnitConverter.availableUnitsFor("furlong").isEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 7: toDisplayValue (graceful)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN convertible WHEN toDisplayValue THEN converted`() {
        val display = UnitConverter.toDisplayValue(1000.0, "g", "kg")

        assertEquals(1.0, display, delta)
    }

    @Test
    fun `GIVEN non-convertible WHEN toDisplayValue THEN original value kept`() {
        // Cross-category → kembalikan nilai asli, jangan error.
        val display = UnitConverter.toDisplayValue(500.0, "g", "ml")

        assertEquals(500.0, display, delta)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 8: format
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN integer value WHEN format THEN no decimal`() {
        assertEquals("1000 g", UnitConverter.format(1000.0, MeasurementUnit.GRAM))
        assertEquals("2 kg", UnitConverter.format(2.0, MeasurementUnit.KILOGRAM))
    }

    @Test
    fun `GIVEN fractional value WHEN format THEN human readable`() {
        assertEquals("1.5 kg", UnitConverter.format(1.5, MeasurementUnit.KILOGRAM))
        assertEquals("0.25 liter", UnitConverter.format(0.25, MeasurementUnit.LITER))
    }

    @Test
    fun `GIVEN non-US locale WHEN format THEN decimal separator stays dot`() {
        // Regresi: format tidak boleh bergantung locale perangkat
        // (mis. locale Indonesia akan menghasilkan "1,5" bila tidak dipaksa US).
        val original = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale("in", "ID"))
            assertEquals("1.5 kg", UnitConverter.format(1.5, MeasurementUnit.KILOGRAM))
        } finally {
            java.util.Locale.setDefault(original)
        }
    }
}
