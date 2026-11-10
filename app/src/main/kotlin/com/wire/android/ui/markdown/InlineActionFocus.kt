/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.markdown

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import java.util.UUID

internal interface InlineActionFocusRegistry {
    fun register(groupId: String, restore: (Int) -> Boolean)
    fun unregister(groupId: String)
    fun save(groupId: String, targetIndex: Int)
}

internal val LocalInlineActionFocusRegistry = staticCompositionLocalOf<InlineActionFocusRegistry?> { null }

/**
 * Connects a text block's native links and mentions to message focus restoration.
 *
 * Obtain this with [rememberInlineActionFocus], apply [modifier] to the text containing the
 * inline targets, and call [save] with the activated target's zero-based source-order index
 * before opening its dialog or profile. The enclosing message supplies
 * [LocalInlineActionFocusRegistry] to remember that occurrence and restore focus on return.
 * Without a registry, the modifier and save callback have no effect.
 *
 * Native inline targets do not expose individual [FocusRequester]s, so restoration uses a
 * temporary anchor to reach the recorded occurrence without adding a normal Tab stop.
 */
internal class InlineActionFocus(val modifier: Modifier, val save: (Int) -> Unit)

@Composable
internal fun rememberInlineActionFocus(): InlineActionFocus {
    val registry = LocalInlineActionFocusRegistry.current
    val groupId = rememberSaveable { UUID.randomUUID().toString() }
    val requester = remember { FocusRequester() }
    val isRestoring = remember { mutableStateOf(false) }
    val hasFocus = remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val inputMode = LocalInputModeManager.current
    DisposableEffect(registry, groupId) {
        registry?.register(groupId) { targetIndex ->
            // Native inline links do not expose individual FocusRequesters. Start from a
            // temporary anchor, then traverse their source order to the recorded occurrence.
            // The anchor is skipped during ordinary keyboard navigation.
            isRestoring.value = true
            try {
                requester.requestFocus() && (0..targetIndex).all { focusManager.moveFocus(FocusDirection.Next) }
            } finally {
                isRestoring.value = false
            }
        }
        onDispose { registry?.unregister(groupId) }
    }
    return InlineActionFocus(
        modifier = if (registry == null) {
            Modifier
        } else {
            Modifier
                .focusRequester(requester)
                .onFocusChanged { hasFocus.value = it.hasFocus }
                .focusProperties { canFocus = isRestoring.value }
                .focusable()
        },
        save = { targetIndex ->
            if (hasFocus.value && inputMode.inputMode == InputMode.Keyboard) {
                registry?.save(groupId, targetIndex)
            }
        },
    )
}
