package com.wire.android.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DateTimeUtilKtTest {

    @Test
    fun `given an invalid date, when performing a transformation, then return null`() {
        val result = "NOT_VALID".formatMediumDateTime()
        assertEquals(null, result)
    }

    @Test
    fun `given an valid date, when performing a transformation, then return with medium format`() {
        val result = "2022-03-24T18:02:30.360Z".formatMediumDateTime()
        assertEquals("Mar 24, 2022, 6:02:30 PM", result)
    }
}
