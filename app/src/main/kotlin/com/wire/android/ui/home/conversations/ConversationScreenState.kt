/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversations

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberConversationScreenState(
    bottomSheetState: WireModalSheetState = rememberWireModalSheetState(),
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
            modalBottomSheetState = bottomSheetState,
            coroutineScope = coroutineScope
        )
    }
}

// todo: pass directly the strings, to avoid passing the context
class ConversationScreenState(
    val context: Context,
    val clipboardManager: ClipboardManager,
    val snackBarHostState: SnackbarHostState,
    val modalBottomSheetState: WireModalSheetState,
    val coroutineScope: CoroutineScope
) {

    var bottomSheetMenuType: BottomSheetMenuType by mutableStateOf(BottomSheetMenuType.None)

    fun showEditContextMenu(message: UIMessage.Regular) {
        bottomSheetMenuType = BottomSheetMenuType.Edit(message)
        coroutineScope.launch { modalBottomSheetState.show() }
    }

    fun hideContextMenu(onComplete: () -> Unit = {}) {
        coroutineScope.launch {
            modalBottomSheetState.hide()
            onComplete()
        }
    }

    fun copyMessage(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        coroutineScope.launch {
            modalBottomSheetState.hide()
            snackBarHostState.showSnackbar(context.getString(R.string.info_message_copied))
        }
    }

    fun showSelfDeletionContextMenu() {
        bottomSheetMenuType = BottomSheetMenuType.SelfDeletion
        coroutineScope.launch { modalBottomSheetState.show() }
    }

    sealed class BottomSheetMenuType {
        class Edit(val selectedMessage: UIMessage.Regular) : BottomSheetMenuType()

        object SelfDeletion : BottomSheetMenuType()

        object None : BottomSheetMenuType()
    }
}
