/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.Density
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal sealed interface MessageOptionsDismissal {
    // Identity distinguishes consecutive dismissals of the same message.
    class RestoreMessage(val messageId: String) : MessageOptionsDismissal
    data object FocusHandledByAction : MessageOptionsDismissal
}

/**
 * Owns message-options opening/dismissal and the keyboard focus return request.
 *
 * Create with [rememberMessageOptionsState] in the conversation screen, render [sheetState],
 * and route all opening/dismissal through this owner. Keyboard shortcuts call [open] with
 * `fromKeyboard = true`; actions that take focus call [hide] with `focusHandledByAction = true`.
 * The message list uses [retainMessageNavigation] and [dismissal], claims the result through
 * [acknowledgeDismissal], and restores focus to the invoking message when requested.
 *
 * Recreation discards unfinished openings and preserves visible sheets and their pending focus restoration.
 */
internal class MessageOptionsState(
    val sheetState: WireModalSheetState<String>,
    private val scope: CoroutineScope,
    private val onTouchDismiss: () -> Unit = {},
    initialKeyboardMessageId: String? = null,
    initialRestorationId: String? = null,
) {
    private var keyboardMessageId by mutableStateOf(initialKeyboardMessageId)
    private var persistDismissal = initialRestorationId != null
    var isOpening by mutableStateOf(false)
        private set
    var dismissal by mutableStateOf<MessageOptionsDismissal?>(
        initialRestorationId?.let { MessageOptionsDismissal.RestoreMessage(it) }
    )
        private set

    val retainMessageNavigation: Boolean
        get() = keyboardMessageId != null || dismissal is MessageOptionsDismissal.RestoreMessage

    fun open(messageId: String, fromKeyboard: Boolean = false): Job? {
        if (isOpening || sheetState.isVisible) return null
        return scope.launch(start = CoroutineStart.UNDISPATCHED) {
            isOpening = true
            keyboardMessageId = messageId.takeIf { fromKeyboard }
            dismissal = null
            persistDismissal = false
            var shown = false
            try {
                sheetState.showAwaited(messageId, hideKeyboard = true)
                shown = true
            } finally {
                isOpening = false
                if (!shown) finishDismissal(persist = false)
            }
        }
    }

    fun hide(focusHandledByAction: Boolean = false, onComplete: suspend () -> Unit = {}) = sheetState.hide {
        finishDismissal(focusHandledByAction)
        onComplete()
    }

    fun onDismissRequest() {
        if (!sheetState.isVisible) return
        val fromKeyboard = keyboardMessageId != null
        sheetState.onDismissRequest()
        finishDismissal()
        if (!fromKeyboard) onTouchDismiss()
    }

    private fun finishDismissal(focusHandledByAction: Boolean = false, persist: Boolean = true) {
        dismissal = when {
            focusHandledByAction -> MessageOptionsDismissal.FocusHandledByAction
            keyboardMessageId != null -> MessageOptionsDismissal.RestoreMessage(checkNotNull(keyboardMessageId))
            else -> dismissal
        }
        keyboardMessageId = null
        persistDismissal = persist
    }

    fun acknowledgeDismissal(handled: MessageOptionsDismissal): Boolean {
        if (dismissal === handled) {
            dismissal = null
            persistDismissal = false
            return true
        }
        return false
    }

    companion object {
        fun saver(density: Density, keyboard: SoftwareKeyboardController?, scope: CoroutineScope, onTouchDismiss: () -> Unit) =
            listSaver<MessageOptionsState, String>(
                save = { state ->
                    val visibleId = (state.sheetState.currentValue as? WireSheetValue.Expanded)?.value
                    val returnId = (state.dismissal as? MessageOptionsDismissal.RestoreMessage)?.messageId
                    // An opening and its failed return request belong to the old composition.
                    listOf(
                        visibleId.orEmpty(),
                        if (visibleId != null) state.keyboardMessageId.orEmpty() else "",
                        if (state.persistDismissal) returnId.orEmpty() else "",
                    )
                },
                restore = { saved ->
                    val initialValue = saved[0].takeIf(String::isNotEmpty)?.let { WireSheetValue.Expanded(it) } ?: WireSheetValue.Hidden
                    MessageOptionsState(
                        sheetState = WireModalSheetState(
                            density = density,
                            scope = scope,
                            keyboardController = keyboard,
                            initialValue = initialValue,
                        ),
                        scope = scope,
                        onTouchDismiss = onTouchDismiss,
                        initialKeyboardMessageId = saved[1].takeIf(String::isNotEmpty),
                        initialRestorationId = saved[2].takeIf(String::isNotEmpty),
                    )
                },
            )
    }
}

@Composable
internal fun rememberMessageOptionsState(
    onTouchDismiss: () -> Unit = {},
    scope: CoroutineScope = rememberCoroutineScope(),
    keyboard: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
    initialMessageId: String? = null,
): MessageOptionsState {
    val density = LocalDensity.current
    val touchDismiss by rememberUpdatedState(onTouchDismiss)
    return rememberSaveable(saver = MessageOptionsState.saver(density, keyboard, scope) { touchDismiss() }) {
        MessageOptionsState(
            sheetState = WireModalSheetState(
                density = density,
                scope = scope,
                keyboardController = keyboard,
                initialValue = initialMessageId?.let { id -> WireSheetValue.Expanded(id) } ?: WireSheetValue.Hidden,
            ),
            scope = scope,
            onTouchDismiss = { touchDismiss() },
        )
    }
}
