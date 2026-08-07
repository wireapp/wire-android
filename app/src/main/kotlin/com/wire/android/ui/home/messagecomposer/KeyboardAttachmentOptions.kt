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

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.wire.android.ui.common.AttachmentButton
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

@Composable
internal fun KeyboardAttachmentOptions(
    options: List<AttachmentOptionItem>,
    focusRequesters: List<FocusRequester>,
    columnCount: Int,
    contentPadding: PaddingValues,
    labelStyle: TextStyle,
) {
    val rows = remember(options, columnCount) { options.chunked(columnCount) }
    val enabledOptions = remember(options) { options.map { it.isEnabled } }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .focusGroup(),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        rows.forEachIndexed { rowIndex, rowOptions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                rowOptions.forEachIndexed { columnIndex, option ->
                    val index = rowIndex * columnCount + columnIndex
                    KeyboardAttachmentOption(
                        option = option,
                        index = index,
                        enabledOptions = enabledOptions,
                        columnCount = columnCount,
                        focusRequesters = focusRequesters,
                        labelStyle = labelStyle,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                }
                repeat(columnCount - rowOptions.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun KeyboardAttachmentOption(
    option: AttachmentOptionItem,
    index: Int,
    enabledOptions: List<Boolean>,
    columnCount: Int,
    focusRequesters: List<FocusRequester>,
    labelStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(option.text)
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .background(
                color = if (isFocused) {
                    MaterialTheme.wireColorScheme.primaryVariant
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(MaterialTheme.wireDimensions.buttonSmallCornerSize),
            )
            .focusRequester(focusRequesters[index])
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                handleAttachmentKeyEvent(
                    event = event,
                    option = option,
                    focusContext = AttachmentFocusContext(index, enabledOptions, columnCount, focusRequesters),
                )
            }
            .semantics {
                contentDescription = label
                role = Role.Button
                if (!option.isEnabled) disabled()
            }
            .focusable(enabled = option.isEnabled),
        contentAlignment = Alignment.Center,
    ) {
        AttachmentButton(
            icon = option.icon,
            labelStyle = labelStyle,
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false },
            text = label,
            enabled = option.isEnabled,
        ) { option.onClick() }
    }
}

private fun handleAttachmentKeyEvent(
    event: KeyEvent,
    option: AttachmentOptionItem,
    focusContext: AttachmentFocusContext,
): Boolean = when {
    event.type != KeyEventType.KeyDown -> false
    event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Spacebar -> {
        if (option.isEnabled) option.onClick()
        true
    }

    else -> event.toAttachmentFocusMove()?.let { move ->
        attachmentFocusableTargetIndex(
            focusContext.index,
            focusContext.enabledOptions,
            focusContext.columnCount,
            move,
        )?.let {
            focusContext.focusRequesters[it].requestFocus()
            true
        }
    } ?: false
}

private data class AttachmentFocusContext(
    val index: Int,
    val enabledOptions: List<Boolean>,
    val columnCount: Int,
    val focusRequesters: List<FocusRequester>,
)

private fun KeyEvent.toAttachmentFocusMove(): AttachmentFocusMove? = when (key) {
    Key.Tab -> if (isShiftPressed) AttachmentFocusMove.Previous else AttachmentFocusMove.Next
    Key.DirectionLeft -> AttachmentFocusMove.Left
    Key.DirectionRight -> AttachmentFocusMove.Right
    Key.DirectionUp -> AttachmentFocusMove.Up
    Key.DirectionDown -> AttachmentFocusMove.Down
    else -> null
}

internal enum class AttachmentFocusMove { Previous, Next, Left, Right, Up, Down }

internal fun attachmentFocusTargetIndex(
    currentIndex: Int,
    itemCount: Int,
    columnCount: Int,
    move: AttachmentFocusMove,
): Int? = if (currentIndex !in 0 until itemCount || columnCount <= 0) {
    null
} else {
    when (move) {
        AttachmentFocusMove.Previous -> (currentIndex - 1 + itemCount) % itemCount
        AttachmentFocusMove.Next -> (currentIndex + 1) % itemCount
        AttachmentFocusMove.Left -> (currentIndex - 1).takeIf { currentIndex % columnCount != 0 }
        AttachmentFocusMove.Right -> (currentIndex + 1).takeIf { (currentIndex + 1) % columnCount != 0 && it < itemCount }
        AttachmentFocusMove.Up -> (currentIndex - columnCount).takeIf { it >= 0 }
        AttachmentFocusMove.Down -> (currentIndex + columnCount).takeIf { it < itemCount }
    }
}

internal fun attachmentFocusableTargetIndex(
    currentIndex: Int,
    enabledOptions: List<Boolean>,
    columnCount: Int,
    move: AttachmentFocusMove,
): Int? {
    var candidate = attachmentFocusTargetIndex(currentIndex, enabledOptions.size, columnCount, move)
    repeat(enabledOptions.size) {
        if (candidate == null || enabledOptions[candidate]) return candidate
        candidate = attachmentFocusTargetIndex(candidate, enabledOptions.size, columnCount, move)
    }
    return null
}
