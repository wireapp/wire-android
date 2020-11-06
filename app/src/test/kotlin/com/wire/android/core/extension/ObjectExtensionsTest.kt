package com.wire.android.core.extension

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class ObjectExtensionsTest {

    @Test
    fun `given a null object, when toStringOrEmpty is called, returns empty string`() {
        val x: Int? = null
        x.toStringOrEmpty() shouldBe String.EMPTY
    }

    @Test
    fun `given a non-null object, when toStringOrEmpty is called, returns its toString() value`() {
        val x: Int? = 3
        x.toStringOrEmpty() shouldBeEqualTo 3.toString()
    }
}
