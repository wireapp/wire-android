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
package com.wire.android.ui.home.messagecomposer.state

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.util.ui.KeyboardHeight
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.util.isPositiveNotNull

@Stable
class MessageCompositionInputStateHolder(
    val messageTextState: TextFieldState,
    selfDeletionTimer: State<SelfDeletionTimer>
) {
    var inputFocused: Boolean by mutableStateOf(false)
        private set

    var keyboardHeight by mutableStateOf(KeyboardHeight.default)
        private set

    var optionsHeight by mutableStateOf(0.dp)
        private set

    var optionsVisible by mutableStateOf(false)
        private set

    var subOptionsVisible by mutableStateOf(false)
        private set

    var isTextExpanded by mutableStateOf(false)
        private set

    var initialKeyboardHeight by mutableStateOf(0.dp)
        private set

    var previousOffset by mutableStateOf(0.dp)
        private set

    private var compositionState: CompositionState by mutableStateOf(CompositionState.Composing)

    val inputType: InputType by derivedStateOf {
        when (val state = compositionState) {
            is CompositionState.Composing -> InputType.Composing(
                isSendButtonEnabled = messageTextState.text.isNotEmpty(),
                messageType = when {
                    selfDeletionTimer.value.duration.isPositiveNotNull() -> MessageType.SelfDeleting(selfDeletionTimer.value)
                    else -> MessageType.Normal
                }
            )
            is CompositionState.Editing -> InputType.Editing(
                isEditButtonEnabled = messageTextState.text.toString() != state.originalMessageText
            )
        }
    }

    fun handleImeOffsetChange(offset: Dp, navBarHeight: Dp, source: Dp, target: Dp) {
        val actualOffset = max(offset - navBarHeight, 0.dp)

        // this check secures that if some additional space will be added to keyboard
        // like gifs search it will save initial keyboard height
        if (source == target && source > 0.dp && initialKeyboardHeight == 0.dp) {
            initialKeyboardHeight = source - navBarHeight
        }

        if (previousOffset < actualOffset) {

            // only if the real goal of this ime offset increase is to really open the keyboard
            // otherwise it can mean the keyboard is still in a process of hiding from the previous screen and ultimately won't be shown
            // in this case we don't want to show and hide the options for a short time as it will only make unwanted blink effect
            if (target > 0.dp) {
                optionsVisible = true
                if (!subOptionsVisible || optionsHeight <= actualOffset) {
                    optionsHeight = actualOffset
                    subOptionsVisible = false
                }
            }
        } else if (previousOffset > actualOffset) {
            if (!subOptionsVisible) {
                optionsHeight = actualOffset
                if (actualOffset == 0.dp) {
                    optionsVisible = false
                    isTextExpanded = false
                }
            }
        }

        previousOffset = actualOffset

        if (keyboardHeight == actualOffset) {
            subOptionsVisible = false
        }

        if (keyboardHeight < actualOffset) {
            keyboardHeight = actualOffset
        }
    }

    fun toEdit(editMessageText: String) {
        compositionState = CompositionState.Editing(editMessageText)
        requestFocus()
    }

    fun toComposing() {
        compositionState = CompositionState.Composing
        requestFocus()
    }

    fun toggleInputSize() {
        isTextExpanded = !isTextExpanded
    }

    fun collapseText() {
        isTextExpanded = false
    }

    fun clearFocus() {
        inputFocused = false
    }

    fun requestFocus() {
        inputFocused = true
    }

    fun showOptions() {
        optionsVisible = true
        subOptionsVisible = true
        if (initialKeyboardHeight > 0.dp) {
            optionsHeight = initialKeyboardHeight
        } else {
            optionsHeight = keyboardHeight
        }
        clearFocus()
    }

    fun collapseComposer(additionalOptionsSubMenuState: AdditionalOptionSubMenuState? = null) {
        if (additionalOptionsSubMenuState != AdditionalOptionSubMenuState.RecordAudio) {
            optionsVisible = false
            subOptionsVisible = false
            isTextExpanded = false
            optionsHeight = 0.dp
            inputFocused = false
        }
    }

    fun calculateOptionsMenuHeight(additionalOptionsSubMenuState: AdditionalOptionSubMenuState): Dp {
        return optionsHeight + if (additionalOptionsSubMenuState != AdditionalOptionSubMenuState.RecordAudio) 0.dp else composeTextHeight
    }

    @Suppress("LongParameterList")
    @VisibleForTesting
    fun updateValuesForTesting(
        keyboardHeight: Dp = KeyboardHeight.default,
        previousOffset: Dp = 0.dp,
        showSubOptions: Boolean = false,
        optionsHeight: Dp = 0.dp,
        showOptions: Boolean = false,
        initialKeyboardHeight: Dp = 0.dp
    ) {
        this.keyboardHeight = keyboardHeight
        this.previousOffset = previousOffset
        this.subOptionsVisible = showSubOptions
        this.optionsHeight = optionsHeight
        this.optionsVisible = showOptions
        this.initialKeyboardHeight = initialKeyboardHeight
    }

    companion object {

        /**
         * This height was based on the size of Input Text + Additional Options (Text Format, Ping, etc)
         */
        val composeTextHeight = 128.dp

        fun saver(
            messageTextState: TextFieldState,
            selfDeletionTimer: State<SelfDeletionTimer>,
            density: Density
        ): Saver<MessageCompositionInputStateHolder, *> = Saver(
            save = {
                with(density) {
                    listOf(
                        it.inputFocused,
                        it.keyboardHeight.toPx(),
                        it.optionsHeight.toPx(),
                        it.optionsVisible,
                        it.subOptionsVisible,
                        it.isTextExpanded,
                        it.previousOffset.toPx()
                    )
                }
            },
            restore = { savedState ->
                with(density) {
                    MessageCompositionInputStateHolder(
                        messageTextState = messageTextState,
                        selfDeletionTimer = selfDeletionTimer
                    ).apply {
                        inputFocused = savedState[0] as Boolean
                        keyboardHeight = (savedState[1] as Float).toDp()
                        optionsHeight = (savedState[2] as Float).toDp()
                        optionsVisible = savedState[3] as Boolean
                        subOptionsVisible = savedState[4] as Boolean
                        isTextExpanded = savedState[5] as Boolean
                        previousOffset = (savedState[6] as Float).toDp()
                    }
                }
            }
        )
    }
}

private sealed class CompositionState {
    data object Composing : CompositionState()
    data class Editing(val originalMessageText: String) : CompositionState()
}

sealed class InputType {
    @Composable
    open fun inputTextColor(): WireTextFieldColors = wireTextFieldColors(
        backgroundColor = Color.Transparent,
        borderColor = Color.Transparent,
        focusColor = Color.Transparent,
        placeholderColor = colorsScheme().secondaryText
    )

    @Composable
    open fun backgroundColor(): Color = colorsScheme().messageComposerBackgroundColor

    @Composable
    open fun labelText(): String = stringResource(R.string.label_type_a_message)

    data class Composing(val isSendButtonEnabled: Boolean, val messageType: MessageType) : InputType() {

        @Composable
        override fun labelText(): String = if (messageType is MessageType.SelfDeleting) {
            stringResource(id = R.string.self_deleting_message_label)
        } else {
            super.labelText()
        }
    }

    class Editing(val isEditButtonEnabled: Boolean) : InputType() {

        @Composable
        override fun backgroundColor(): Color = colorsScheme().messageComposerEditBackgroundColor
    }
}

sealed class MessageType {
    data object Normal : MessageType()
    data class SelfDeleting(val selfDeletionTimer: SelfDeletionTimer) : MessageType()
}
