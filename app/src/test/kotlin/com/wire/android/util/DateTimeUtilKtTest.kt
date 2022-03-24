package com.wire.android.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DateTimeUtilKtTest {

    @Test
    fun `given an invalid date, when performing a transformation, then return null`() {
        val result = "NOT_VALID".formatMediumDateTime()
        assertEquals(null, result)
    }
}
