package com.wire.android.ui.authentication

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BackendConfigParserTest {
    @Test
    fun `returns a direct backend configuration url`() {
        assertEquals("https://example.test/config.json", " https://example.test/config.json ".toBackendConfigUrl())
    }

    @Test
    fun `extracts an encoded config from a Wire access link`() {
        val input = "wire://access/?config=https%3A%2F%2Fexample.test%2Fconfig.json"

        assertEquals("https://example.test/config.json", input.toBackendConfigUrl())
    }

    @Test
    fun `rejects empty and malformed access links`() {
        assertNull(" ".toBackendConfigUrl())
        assertNull("wire://access/?config=%".toBackendConfigUrl())
    }
}
