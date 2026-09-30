package com.csm.kitchenguard.presentation.screens.waste

import com.csm.kitchenguard.data.local.entity.IngredientEntity
import com.csm.kitchenguard.utils.units.MeasurementUnit
import com.csm.kitchenguard.utils.units.UnitConverter
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test untuk konversi unit di [WasteFormUiState] (Issue #20 / PRD Section 13).
 *
 * Menguji logika murni pada state (tanpa Android runtime), yaitu:
 * - availableUnits terisi sesuai canonical bahan.
 * - canonicalQuantityPreview mengonversi nilai input ke canonical.
 */
class WasteFormUiStateUnitConversionTest {

    private val ingredientKg = IngredientEntity(
        id = 1L, name = "Daging Sapi", category = "Meat", canonicalUnit = "kg", minimumStock = 1.0
    )

    private fun stateWith(
        unit: String,
        quantity: String,
        ingredient: IngredientEntity = ingredientKg
    ) = WasteFormUiState(
        selectedIngredient = ingredient,
        quantityInput = quantity,
        selectedUnit = unit,
        availableUnits = UnitConverter.availableUnitsFor(ingredient.canonicalUnit)
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Group 1: availableUnits untuk bahan
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN canonical kg WHEN availableUnits THEN offers kg and g`() {
        val state = stateWith("kg", "")

        assertEquals(2, state.availableUnits.size)
        assertTrue(state.availableUnits.contains(MeasurementUnit.KILOGRAM))
        assertTrue(state.availableUnits.contains(MeasurementUnit.GRAM))
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Group 2: canonicalQuantityPreview
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `GIVEN input in grams WHEN preview THEN converted to canonical kg`() {
        val state = stateWith(unit = "g", quantity = "1500")

        assertEquals(1.5, state.canonicalQuantityPreview!!, 1e-9)
    }

    @Test
    fun `GIVEN input already canonical WHEN preview THEN unchanged`() {
        val state = stateWith(unit = "kg", quantity = "2.5")

        assertEquals(2.5, state.canonicalQuantityPreview!!, 1e-9)
    }

    @Test
    fun `GIVEN empty input WHEN preview THEN null`() {
        val state = stateWith(unit = "g", quantity = "")

        assertNull(state.canonicalQuantityPreview)
    }

    @Test
    fun `GIVEN invalid input WHEN preview THEN null`() {
        val state = stateWith(unit = "g", quantity = "abc")

        assertNull(state.canonicalQuantityPreview)
    }

    @Test
    fun `GIVEN no ingredient WHEN preview THEN null`() {
        val state = WasteFormUiState(quantityInput = "5", selectedUnit = "g")

        assertNull(state.canonicalQuantityPreview)
    }

    @Test
    fun `GIVEN volume ingredient WHEN preview with ml THEN converted to liter`() {
        val oil = IngredientEntity(
            id = 3L, name = "Minyak", category = "Pantry", canonicalUnit = "liter", minimumStock = 1.0
        )
        val state = stateWith(unit = "ml", quantity = "500", ingredient = oil)

        assertEquals(0.5, state.canonicalQuantityPreview!!, 1e-9)
    }
}
