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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.button.wireTertiaryButtonColors
import com.wire.android.ui.theme.wireColorScheme

/** Conversation-scoped focus colors until the app-wide focus tokens are aligned with the active accent. */
@Composable
internal fun messageComposerSecondaryButtonColors() = wireSecondaryButtonColors().copy(
    focused = MaterialTheme.wireColorScheme.primaryVariant,
)

@Composable
internal fun messageComposerTertiaryButtonColors() = wireTertiaryButtonColors().copy(
    focused = MaterialTheme.wireColorScheme.primaryVariant,
)
