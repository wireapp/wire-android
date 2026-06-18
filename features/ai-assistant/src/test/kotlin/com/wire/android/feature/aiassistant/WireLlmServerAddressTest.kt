/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.feature.aiassistant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.api.Test

class WireLlmServerAddressTest {
    @Test
    fun givenValidIpv4WithWhitespace_whenNormalizing_thenTrimmedAddressIsReturned() {
        assertEquals("192.168.1.20", WireLlmServerAddress.normalize(" 192.168.1.20 "))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "http://192.168.1.20", "192.168.1.20:8080", "256.1.1.1", "01.2.3.4", "localhost"])
    fun givenInvalidAddress_whenNormalizing_thenNullIsReturned(value: String) {
        assertNull(WireLlmServerAddress.normalize(value))
    }
}
