/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.conversations

import android.content.res.Configuration
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HardwareKeyboardTest {

    @Test
    fun givenNoHardwareKeyboard_whenCheckingConfiguration_thenReturnsFalse() {
        val configuration = Configuration().apply {
            keyboard = Configuration.KEYBOARD_NOKEYS
        }

        assertFalse(hasHardwareKeyboard(configuration))
    }

    @Test
    fun givenUndefinedKeyboard_whenCheckingConfiguration_thenReturnsFalse() {
        val configuration = Configuration().apply {
            keyboard = Configuration.KEYBOARD_UNDEFINED
        }

        assertFalse(hasHardwareKeyboard(configuration))
    }

    @Test
    fun givenQwertyHardwareKeyboard_whenCheckingConfiguration_thenReturnsTrue() {
        val configuration = Configuration().apply {
            keyboard = Configuration.KEYBOARD_QWERTY
            hardKeyboardHidden = Configuration.HARDKEYBOARDHIDDEN_NO
        }

        assertTrue(hasHardwareKeyboard(configuration))
    }

    @Test
    fun givenTwelveKeyHardwareKeyboard_whenCheckingConfiguration_thenReturnsTrue() {
        val configuration = Configuration().apply {
            keyboard = Configuration.KEYBOARD_12KEY
            hardKeyboardHidden = Configuration.HARDKEYBOARDHIDDEN_NO
        }

        assertTrue(hasHardwareKeyboard(configuration))
    }

    @Test
    fun givenReportedKeyboardIsHidden_whenCheckingConfiguration_thenReturnsFalse() {
        val configuration = Configuration().apply {
            keyboard = Configuration.KEYBOARD_QWERTY
            hardKeyboardHidden = Configuration.HARDKEYBOARDHIDDEN_YES
        }

        assertFalse(hasHardwareKeyboard(configuration))
    }
}
