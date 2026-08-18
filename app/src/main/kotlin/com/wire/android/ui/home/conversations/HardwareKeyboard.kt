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
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Unlike the software keyboard, a connected hardware keyboard does not make the IME insets visible.
 * Conversation controls must therefore not use IME visibility as a proxy for keyboard interaction.
 */
@Composable
internal fun isHardwareKeyboardConnected(): Boolean =
    hasHardwareKeyboard(LocalConfiguration.current)

internal fun hasHardwareKeyboard(configuration: Configuration): Boolean =
    configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO &&
        (
            configuration.keyboard == Configuration.KEYBOARD_QWERTY ||
                configuration.keyboard == Configuration.KEYBOARD_12KEY
            )
