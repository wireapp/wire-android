/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.settings.devices

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.wire.android.R
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberConversationScreenState(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): DeviceDetailsScreenState {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = LocalSnackbarHostState.current
    val text = stringResource(R.string.label_text_copied)
    return remember {
        DeviceDetailsScreenState(
            clipboardManager = clipboardManager,
            coroutineScope = coroutineScope,
            copiedText = text,
            snackBarHostState = snackbarHostState
        )
    }
}

class DeviceDetailsScreenState(
    private val clipboardManager: ClipboardManager,
    val snackBarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope,
    private val copiedText: String
) {

    fun copyMessage(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        coroutineScope.launch { snackBarHostState.showSnackbar(copiedText) }
    }
}
