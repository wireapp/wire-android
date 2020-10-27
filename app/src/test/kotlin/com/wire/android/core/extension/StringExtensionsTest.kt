package com.wire.android.core.extension

import com.wire.android.UnitTest
import org.amshove.kluent.shouldBe
import org.junit.Test

class StringExtensionsTest : UnitTest() {

    @Test
    fun `when EMPTY is called then return empty string`() {
        String.EMPTY shouldBe ""
    }
}
