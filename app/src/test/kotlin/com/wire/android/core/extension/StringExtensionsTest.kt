package com.wire.android.core.extension

import com.wire.android.UnitTest
import org.assertj.core.api.Assertions.assertThat

import org.junit.Test

class StringExtensionsTest : UnitTest() {

    @Test
    fun `when EMPTY is called then return empty string`() {
        assertThat(String.EMPTY).isEqualTo("")
    }
}
