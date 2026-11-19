package com.helal.accomplishmentplan.nativeapp.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StatusColorPaletteTest {

    @Test
    fun `zero count returns neutral white background and dark text for standard and custom statuses`() {
        val doneZero = StatusColorPalette.summaryPillStyle("Done", 0)
        val customZero = StatusColorPalette.summaryPillStyle("Board Review", 0)

        assertEquals(Color(0xFFFFFFFF), doneZero.containerColor)
        assertEquals(Color(0xFF111827), doneZero.contentColor)
        assertEquals(Color(0xFFD1D5DB), doneZero.borderColor)

        assertEquals(doneZero, customZero)
    }

    @Test
    fun `positive count returns status-specific colors for standard statuses`() {
        val doneOne = StatusColorPalette.summaryPillStyle("Done", 1)
        val pendingThree = StatusColorPalette.summaryPillStyle("Pending", 3)
        val cancelledTwo = StatusColorPalette.summaryPillStyle("Cancelled", 2)

        assertNotEquals(StatusColorPalette.zeroCountNeutralStyle, doneOne)
        assertNotEquals(doneOne.containerColor, pendingThree.containerColor)
        assertNotEquals(pendingThree.containerColor, cancelledTwo.containerColor)
    }

    @Test
    fun `custom status color mapping is deterministic and case-insensitive`() {
        val a = StatusColorPalette.styleForStatus("Audit Checking")
        val b = StatusColorPalette.styleForStatus("  audit checking  ")

        assertEquals(a, b)
    }
}
