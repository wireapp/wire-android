package com.wire.android.core.extension

import com.wire.android.UnitTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class StringExtensionsTest : UnitTest() {

    @Test
    fun `when EMPTY is called then return empty string`() {
        String.EMPTY shouldBe ""
    }

    @Test
    fun `given accent characters as input, when removeAccents, then output should be latin`() {
        val input = "àáâãäçèéêëìíîïñòóôõöùúûüýÿÀÁÂÃÄÇÈÉÊËÌÍÎÏÑÒÓÔÕÖÙÚÛÜÝ"
        val output = "aaaaaceeeeiiiinooooouuuuyyAAAAACEEEEIIIINOOOOOUUUUY"

        val result = input.removeAccents()
        result shouldBeEqualTo output
    }
}
