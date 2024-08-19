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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.util.isNotMarkdownBlank

enum class ComposerState {
    Collapsed,
    Expanded,
}

@Stable
class MessageCompositionInputStateHolder(
    val messageTextState: TextFieldState,
    private val keyboardController: SoftwareKeyboardController?
) {
    var inputFocused: Boolean by mutableStateOf(false)

    private var keyboardHeight by mutableStateOf(0.dp)

    var optionsHeight by mutableStateOf(0.dp)
        private set

    var composerState by mutableStateOf(ComposerState.Collapsed)
        private set

    var isTextExpanded by mutableStateOf(false)
        private set

    var optionsVisible by mutableStateOf(false)
        private set

    // FocusRequester
    val focusRequester = FocusRequester()

    private var compositionState: CompositionState by mutableStateOf(CompositionState.Composing)

    val inputType: InputType by derivedStateOf {
        when (val state = compositionState) {
            is CompositionState.Composing -> InputType.Composing(
                isSendButtonEnabled = messageTextState.text.isNotMarkdownBlank()
            )

            is CompositionState.Editing -> InputType.Editing(
                isEditButtonEnabled = messageTextState.text != state.originalMessageText && messageTextState.text.isNotMarkdownBlank()
            )
        }
    }

    fun handleImeOffsetChange(offset: Dp, navBarHeight: Dp, source: Dp, target: Dp) {
        val actualOffset = max(offset - navBarHeight, 0.dp)
        val actualSource = max(source - navBarHeight, 0.dp)
        val actualTarget = max(target - navBarHeight, 0.dp)
        println("KBX actual offset $actualOffset")
        println("KBX actual source $actualSource target $actualTarget")

        // this check secures that if some additional space will be added to keyboard
        // like gifs search it will save initial keyboard height
        if (source == target && source > 0.dp) {
            optionsHeight = actualOffset
        }

        if (source == target) {
            if (source > 0.dp) {
                if (keyboardHeight == 0.dp) {
                    keyboardHeight = actualOffset
                }
                optionsHeight = actualOffset
            }
        }

        if(actualTarget == 0.dp) {
            if (keyboardHeight > 0.dp) {
                optionsHeight = keyboardHeight
            }
            println("KBX inputFocused $inputFocused composerState ${composerState}")
            inputFocused = false
            composerState = ComposerState.Collapsed
        }
    }

    fun showAttachments(showOptions: Boolean) {
        println("KBX set optionsVisible $showOptions")
        optionsVisible = showOptions
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

    fun setFocused() {
        println("KBX setFocused")
        composerState = ComposerState.Expanded
        inputFocused = true
        keyboardController?.show()
    }

    fun requestFocus() {
        println("KBX request focus $inputFocused")
        if (!inputFocused) {
            focusRequester.requestFocus()
        }
        keyboardController?.show()
        inputFocused = true
        println("KBX request focus Keyboard")
        composerState = ComposerState.Expanded
    }

    fun hideKeyboard() {
        focusRequester.freeFocus()
        keyboardController?.hide()
        composerState = ComposerState.Collapsed
    }

    fun showOptions() {
        requestFocus()
        composerState = ComposerState.Expanded
        keyboardController?.show()
    }

    fun collapseComposer(additionalOptionsSubMenuState: AdditionalOptionSubMenuState? = null) {
        if (additionalOptionsSubMenuState != AdditionalOptionSubMenuState.RecordAudio) {
            composerState = ComposerState.Collapsed
            isTextExpanded = false
        }
    }

    fun calculateOptionsMenuHeight(additionalOptionsSubMenuState: AdditionalOptionSubMenuState): Dp {
        return optionsHeight + if (additionalOptionsSubMenuState != AdditionalOptionSubMenuState.RecordAudio) 0.dp else composeTextHeight
    }

    @Suppress("LongParameterList")
    @VisibleForTesting
    fun updateValuesForTesting(
        keyboardHeight: Dp = 250.dp,
        optionsHeight: Dp = 0.dp,
        composerState: ComposerState = ComposerState.Collapsed,
    ) {
        this.keyboardHeight = keyboardHeight
        this.optionsHeight = optionsHeight
        this.composerState = composerState
    }

    companion object {
        private const val TAG = "[MessageCompositionInputStateHolder]"
        private const val enableLogs = true
        private fun logActions(actionName: String) {
            if (enableLogs) {
                println("$TAG $actionName")
            }
        }

        /**
         * This height was based on the size of Input Text + Additional Options (Text Format, Ping, etc)
         */
        val composeTextHeight = 128.dp

        fun saver(
            messageTextState: TextFieldState,
            keyboardController: SoftwareKeyboardController?,
            density: Density
        ): Saver<MessageCompositionInputStateHolder, *> = Saver(
            save = {
                with(density) {
                    listOf(
                        it.inputFocused,
                        it.keyboardHeight.toPx(),
                        it.optionsHeight.toPx(),
                        it.composerState.toString(),
                        it.isTextExpanded
                    )
                }
            },
            restore = { savedState ->
                with(density) {
                    MessageCompositionInputStateHolder(
                        messageTextState = messageTextState,
                        keyboardController = keyboardController,
                    ).apply {
                        inputFocused = savedState[0] as Boolean
                        keyboardHeight = (savedState[1] as Float).toDp()
                        optionsHeight = (savedState[2] as Float).toDp()
                        composerState = ComposerState.valueOf(savedState[3] as String)
                        isTextExpanded = savedState[4] as Boolean
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
    open fun inputTextColor(isSelfDeleting: Boolean): WireTextFieldColors = wireTextFieldColors(
        backgroundColor = Color.Transparent,
        borderColor = Color.Transparent,
        focusColor = Color.Transparent,
        placeholderColor = if (isSelfDeleting) {
            colorsScheme().primary
        } else {
            colorsScheme().secondaryText
        }
    )

    @Composable
    open fun backgroundColor(): Color = colorsScheme().messageComposerBackgroundColor

    @Composable
    open fun labelText(): String = stringResource(R.string.label_type_a_message)

    data class Composing(val isSendButtonEnabled: Boolean) : InputType()

    class Editing(val isEditButtonEnabled: Boolean) : InputType() {

        @Composable
        override fun backgroundColor(): Color = colorsScheme().messageComposerEditBackgroundColor
    }
}
