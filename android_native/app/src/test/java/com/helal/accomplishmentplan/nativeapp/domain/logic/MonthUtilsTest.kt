package com.helal.accomplishmentplan.nativeapp.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthUtilsTest {

    @Test
    fun `converts bidirectionally between YYYY-MM storage key and Mmm-YYYY display label`() {
        val parsed = MonthUtils.parseStorageKey("2026-11")!!
        assertEquals(2026, parsed.year)
        assertEquals(11, parsed.month)
        assertEquals("Nov-2026", parsed.toDisplayLabel())

        val fromDisplay = MonthUtils.parseDisplayLabel("nov-2026")!!
        assertEquals("2026-11", fromDisplay.toStorageKey())
        assertEquals("Nov-2026", fromDisplay.toDisplayLabel())
    }

    @Test
    fun `rejects invalid month formats and builds deterministic counter document ID`() {
        assertNull(MonthUtils.parseStorageKey("2026-13"))
        assertNull(MonthUtils.parseDisplayLabel("Foo-2026"))
        assertEquals("uid_abc_Nov-2026", MonthUtils.counterDocumentId("uid_abc", "2026-11"))
        assertEquals("uid_abc_Nov-2026", MonthUtils.counterDocumentId("uid_abc", "Nov-2026"))
    }

    @Test
    fun `sorts month labels chronologically descending and builds 12-month year grid`() {
        val sorted = MonthUtils.sortMonthLabelsDescending(
            listOf("Oct-2026", "Jan-2027", "Nov-2026", "dec-2026", "Nov-2026")
        )
        assertEquals(listOf("Jan-2027", "Dec-2026", "Nov-2026", "Oct-2026"), sorted)
        assertEquals(12, MonthUtils.monthsForYear(2026).size)
        assertEquals("Jan-2026", MonthUtils.monthsForYear(2026).first().toDisplayLabel())
        assertEquals("Dec-2026", MonthUtils.monthsForYear(2026).last().toDisplayLabel())
    }
}
