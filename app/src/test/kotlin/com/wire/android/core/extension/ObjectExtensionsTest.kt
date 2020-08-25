package com.wire.android.core.extension

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ObjectExtensionsTest {

    @Test
    fun `given a null object, when toStringOrEmpty is called, returns empty string`() {
        val x: Int? = null
        assertThat(x.toStringOrEmpty()).isEqualTo("")
    }

    @Test
    fun `given a non-null object, when toStringOrEmpty is called, returns its toString() value`() {
        val x : Int? = 3
        assertThat(x.toStringOrEmpty()).isEqualTo(3.toString())
    }
}
