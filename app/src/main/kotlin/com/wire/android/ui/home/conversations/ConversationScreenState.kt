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

package com.wire.android.ui.home.conversations

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberConversationScreenState(
    editSheetState: WireModalSheetState<String> = rememberWireModalSheetState<String>(),
    selfDeletingSheetState: WireModalSheetState<SelfDeletionTimer> = rememberWireModalSheetState<SelfDeletionTimer>(),
    locationSheetState: WireModalSheetState<Unit> = rememberWireModalSheetState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): ConversationScreenState {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackBarHostState = LocalSnackbarHostState.current
    return remember {
        ConversationScreenState(
            context = context,
            clipboardManager = clipboardManager,
            snackBarHostState = snackBarHostState,
            editSheetState = editSheetState,
            selfDeletingSheetState = selfDeletingSheetState,
            locationSheetState = locationSheetState,
            coroutineScope = coroutineScope
        )
    }
}

// todo: pass directly the strings, to avoid passing the context
class ConversationScreenState(
    val context: Context,
    val clipboardManager: ClipboardManager,
    val snackBarHostState: SnackbarHostState,
    val editSheetState: WireModalSheetState<String>,
    val selfDeletingSheetState: WireModalSheetState<SelfDeletionTimer>,
    val locationSheetState: WireModalSheetState<Unit>,
    val coroutineScope: CoroutineScope
) {
    fun showEditContextMenu(message: UIMessage.Regular) {
        editSheetState.show(message.header.messageId, hideKeyboard = true)
    }

    fun copyMessage(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        coroutineScope.launch {
            snackBarHostState.showSnackbar(context.getString(R.string.info_message_copied))
        }
    }

    fun showSelfDeletionContextMenu(currentlySelected: SelfDeletionTimer) {
        selfDeletingSheetState.show(currentlySelected, hideKeyboard = true)
    }

    fun showLocationSheet() {
        locationSheetState.show(hideKeyboard = true)
    }

    val isAnySheetVisible: Boolean
        get() = editSheetState.isVisible || selfDeletingSheetState.isVisible || locationSheetState.isVisible
}
