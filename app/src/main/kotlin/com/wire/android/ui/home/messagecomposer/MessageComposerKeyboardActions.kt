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

import androidx.compose.foundation.text.input.KeyboardActionHandler

internal fun messageComposerKeyboardActionHandler(
    enterToSend: Boolean,
    canSendMessage: Boolean,
    onSend: () -> Unit,
): KeyboardActionHandler? = if (enterToSend) {
    KeyboardActionHandler { if (canSendMessage) onSend() }
} else {
    // Let the multiline text field handle software keyboard Enter normally.
    null
}

internal fun handleMessageComposerEnter(
    enterToSend: Boolean,
    isShiftPressed: Boolean,
    canSendMessage: Boolean,
    onNewLine: () -> Unit,
    onSend: () -> Unit,
): Boolean = when {
    isShiftPressed -> {
        onNewLine()
        true
    }
    !enterToSend -> false
    canSendMessage -> {
        onSend()
        true
    }
    else -> false
}
