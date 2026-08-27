/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.messagecomposer

import android.view.KeyCharacterMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MessageComposerDeadKeyTest {

    @Test
    fun givenDeadKeyUnicodeCharacter_whenConverting_thenReturnsAccentWithoutCombiningFlag() {
        val deadKey = KeyCharacterMap.COMBINING_ACCENT or ACUTE_ACCENT.code

        assertEquals(ACUTE_ACCENT.toString(), unicodeCharToPrintableString(deadKey))
    }

    @Test
    fun givenRegularUnicodeCharacter_whenConverting_thenReturnsCharacter() {
        assertEquals("a", unicodeCharToPrintableString('a'.code))
    }

    @Test
    fun givenInvalidUnicodeCharacter_whenConverting_thenReturnsNull() {
        assertNull(unicodeCharToPrintableString(Int.MAX_VALUE))
    }

    private companion object {
        const val ACUTE_ACCENT = '\u00B4'
    }
}
