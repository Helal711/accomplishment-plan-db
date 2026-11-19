package com.helal.accomplishmentplan.nativeapp.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountParserTest {

    @Test
    fun `safely parses String, Long, Int, and Double amounts`() {
        assertEquals(25000.0, AmountParser.parseToDouble("25,000 TK")!!, 0.001)
        assertEquals(18500.5, AmountParser.parseToDouble("18,500.50")!!, 0.001)
        assertEquals(42000.0, AmountParser.parseToDouble(42000L)!!, 0.001)
        assertEquals(1500.75, AmountParser.parseToDouble(1500.75)!!, 0.001)
        assertEquals(12500.0, AmountParser.parseToDouble("১২,৫০০ ৳")!!, 0.001)
        assertNull(AmountParser.parseToDouble(Double.NaN))
        assertNull(AmountParser.parseToDouble("   "))
    }

    @Test
    fun `converts numeric and string values to clean storage strings`() {
        assertEquals("45000", AmountParser.rawToStorageString(45000L))
        assertEquals("45000", AmountParser.rawToStorageString(45000.0))
        assertEquals("45000.5", AmountParser.rawToStorageString(45000.50))
        assertEquals("12,500", AmountParser.rawToStorageString("  12,500  "))
        assertEquals("", AmountParser.rawToStorageString(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `formats display currency and generates amount search tokens`() {
        assertEquals("125,000 TK", AmountParser.formatForDisplay(125000L))
        assertEquals("12,500.75 TK", AmountParser.formatForDisplay(12500.75))

        val tokens = AmountParser.searchTokens("125,000")
        assertTrue(tokens.contains("125000"))
        assertTrue(tokens.contains("125,000"))
        assertTrue(tokens.contains("125,000 tk"))
    }
}
