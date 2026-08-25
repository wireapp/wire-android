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

package com.wire.android.ui.home.messagecomposer

import android.view.KeyCharacterMap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.atMost
import com.wire.android.R
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.MessageComposerDefault
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.home.conversations.UsersTypingIndicatorForConversation
import com.wire.android.ui.home.conversations.messages.QuotedMessagePreview
import com.wire.android.ui.home.conversations.selfDeletingMessageActionViewModel
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionArgs
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModel
import com.wire.android.ui.home.messagecomposer.attachments.AdditionalOptionButton
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.InputType
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer

@Composable
fun ActiveMessageComposerInput(
    conversationId: ConversationId,
    messageComposition: MessageComposition,
    messageTextState: TextFieldState,
    isTextExpanded: Boolean,
    inputType: InputType,
    focusRequester: FocusRequester,
    keyboardOptions: KeyboardOptions,
    onKeyboardAction: KeyboardActionHandler?,
    onHardwareEnter: (isShiftPressed: Boolean) -> Boolean,
    canSendMessage: Boolean,
    onSendButtonClicked: () -> Unit,
    onEditButtonClicked: () -> Unit,
    onChangeSelfDeletionClicked: (currentlySelected: SelfDeletionTimer) -> Unit,
    onToggleInputSize: () -> Unit,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit,
    onFocused: () -> Unit,
    onSelectedLineIndexChanged: (Int) -> Unit,
    onLineBottomYCoordinateChanged: (Float) -> Unit,
    showOptions: Boolean,
    showInlinePlusButton: Boolean,
    optionsSelected: Boolean,
    onPlusClick: () -> Unit,
    useKeyboardActivationGate: Boolean,
    modifier: Modifier = Modifier,
    onHardwareTab: (isShiftPressed: Boolean) -> Boolean = { false },
    onHardwareEscape: () -> Boolean = { false },
) {
    var isCollapseButtonFocused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .background(inputType.backgroundColor())
    ) {
        HorizontalDivider(color = MaterialTheme.wireColorScheme.outline)
        if (showOptions || isCollapseButtonFocused) {
            CollapseButton(
                isCollapsed = !isTextExpanded,
                onCollapseClick = onToggleInputSize,
                onFocusChanged = { isCollapseButtonFocused = it },
            )
        }

        messageComposition.quotedMessage?.let { quotedMessage ->
            VerticalSpace.x4()
            Box(modifier = Modifier.padding(horizontal = dimensions().spacing8x)) {
                QuotedMessagePreview(
                    conversationId = conversationId,
                    quotedMessageData = quotedMessage,
                    onCancelReply = onCancelReply
                )
            }
        }

        InputContent(
            conversationId = conversationId,
            messageTextState = messageTextState,
            isTextExpanded = isTextExpanded,
            inputType = inputType,
            focusRequester = focusRequester,
            onSendButtonClicked = onSendButtonClicked,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            onHardwareEnter = onHardwareEnter,
            onHardwareTab = onHardwareTab,
            onHardwareEscape = onHardwareEscape,
            canSendMessage = canSendMessage,
            onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
            onFocused = onFocused,
            onSelectedLineIndexChanged = onSelectedLineIndexChanged,
            onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
            showInlinePlusButton = showInlinePlusButton,
            optionsSelected = optionsSelected,
            onPlusClick = onPlusClick,
            useKeyboardActivationGate = useKeyboardActivationGate,
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (isTextExpanded) it.weight(1F) else it.wrapContentHeight()
                },
        )
        when (inputType) {
            is InputType.Editing -> {
                MessageEditActions(
                    onEditSaveButtonClicked = onEditButtonClicked,
                    onEditCancelButtonClicked = onCancelEdit,
                    editButtonEnabled = inputType.isEditButtonEnabled
                )
            }

            else -> {}
        }
    }
}

// flexible composable to adapt when [MessageComposerTextInput] is expanded or collapsed
@Composable
private fun InputContent(
    conversationId: ConversationId,
    messageTextState: TextFieldState,
    isTextExpanded: Boolean,
    inputType: InputType,
    focusRequester: FocusRequester,
    keyboardOptions: KeyboardOptions,
    onKeyboardAction: KeyboardActionHandler?,
    onHardwareEnter: (isShiftPressed: Boolean) -> Boolean,
    onHardwareTab: (isShiftPressed: Boolean) -> Boolean,
    onHardwareEscape: () -> Boolean,
    canSendMessage: Boolean,
    onSendButtonClicked: () -> Unit,
    onChangeSelfDeletionClicked: (currentlySelected: SelfDeletionTimer) -> Unit,
    onFocused: () -> Unit,
    onSelectedLineIndexChanged: (Int) -> Unit,
    onLineBottomYCoordinateChanged: (Float) -> Unit,
    showInlinePlusButton: Boolean,
    optionsSelected: Boolean,
    onPlusClick: () -> Unit,
    useKeyboardActivationGate: Boolean,
    modifier: Modifier = Modifier,
    viewModel: SelfDeletingMessageActionViewModel =
        selfDeletingMessageActionViewModel(
            SelfDeletingMessageActionArgs(conversationId = conversationId)
        ),
) {
    ConstraintLayout(modifier = modifier) {
        val (additionalOptionButton, input, actions) = createRefs()
        val buttonsTopBarrier = createTopBarrier(additionalOptionButton, actions)
        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier.constrainAs(additionalOptionButton) {
                start.linkTo(parent.start)
                bottom.linkTo(parent.bottom)
            }
        ) {
            if (showInlinePlusButton && inputType is InputType.Composing) {
                AdditionalOptionButton(
                    isSelected = optionsSelected,
                    onClick = onPlusClick,
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            }
        }

        val collapsedMaxHeight = dimensions().messageComposerActiveInputMaxHeight
        MessageComposerTextInput(
            focusRequester = focusRequester,
            colors = inputType.inputTextColor(isSelfDeleting = viewModel.state().duration != null),
            messageTextState = messageTextState,
            placeHolderText = viewModel.state().duration?.let { stringResource(id = R.string.self_deleting_message_label) }
                ?: inputType.labelText(),
            onFocused = onFocused,
            onSelectedLineIndexChanged = onSelectedLineIndexChanged,
            onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
            keyboardOptions = keyboardOptions,
            onKeyBoardAction = onKeyboardAction,
            onHardwareEnter = onHardwareEnter,
            onHardwareTab = onHardwareTab,
            onHardwareEscape = onHardwareEscape,
            useKeyboardActivationGate = useKeyboardActivationGate,
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(input) {
                    width = Dimension.fillToConstraints
                    height = if (isTextExpanded) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(buttonsTopBarrier)
                        Dimension.fillToConstraints
                    } else {
                        start.linkTo(additionalOptionButton.end)
                        end.linkTo(actions.start)
                        bottom.linkTo(parent.bottom)
                        Dimension.preferredWrapContent.atMost(collapsedMaxHeight)
                    }
                }
        )

        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .constrainAs(actions) {
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
        ) {
            if (isTextExpanded) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    UsersTypingIndicatorForConversation(
                        conversationId = conversationId,
                    )
                }
            }
            // Only show send button when not in editing mode
            if (inputType !is InputType.Editing) {
                MessageSendActions(
                    onSendButtonClicked = onSendButtonClicked,
                    sendButtonEnabled = canSendMessage,
                    selfDeletionTimer = viewModel.state(),
                    onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                    modifier = Modifier.padding(end = dimensions().spacing8x)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("CyclomaticComplexMethod") // Focus gating keeps hardware and touch keyboard states in one text-field owner.
@Composable
private fun MessageComposerTextInput(
    messageTextState: TextFieldState,
    focusRequester: FocusRequester,
    colors: WireTextFieldColors,
    placeHolderText: String,
    onFocused: () -> Unit,
    keyboardOptions: KeyboardOptions,
    onKeyBoardAction: KeyboardActionHandler?,
    onHardwareEnter: (isShiftPressed: Boolean) -> Boolean,
    onHardwareTab: (isShiftPressed: Boolean) -> Boolean,
    onHardwareEscape: () -> Boolean,
    useKeyboardActivationGate: Boolean,
    modifier: Modifier = Modifier,
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
) {
    val interactionSource = remember { MutableInteractionSource() }
    val activationGateInteractionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val inputFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    var isActivationGateFocused by remember { mutableStateOf(false) }
    var hasComposerFocus by remember { mutableStateOf(false) }
    var isInputActive by remember(useKeyboardActivationGate) {
        mutableStateOf(!useKeyboardActivationGate)
    }

    fun activateInput(initialText: String? = null) {
        initialText?.let { text ->
            messageTextState.edit { append(text) }
        }
        isInputActive = true
        onFocused()
    }

    LaunchedEffect(isInputActive, useKeyboardActivationGate) {
        if (useKeyboardActivationGate && isInputActive) {
            inputFocusRequester.requestFocus()
            withFrameNanos { }
            softwareKeyboardController?.hide()
        }
    }

    LaunchedEffect(hasComposerFocus, useKeyboardActivationGate) {
        if (useKeyboardActivationGate && !hasComposerFocus) {
            // Focus briefly leaves the activation gate before reaching the text field. Wait one frame so this
            // internal hand-off is not mistaken for leaving the composer and deactivating the input again.
            withFrameNanos { }
            if (!hasComposerFocus) {
                isInputActive = false
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onFocused()
        }
    }

    Box(
        modifier = modifier
            .applyIf(useKeyboardActivationGate) {
                focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        event.handleInputActivation(isInputActive) { initialText ->
                            activateInput(initialText)
                        }
                    }
                    .onFocusChanged { focusState ->
                        isActivationGateFocused = focusState.isFocused
                        hasComposerFocus = focusState.hasFocus
                    }
                    .applyIf(isActivationGateFocused) {
                        background(
                            color = MaterialTheme.wireColorScheme.primaryVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                    .focusable(
                        enabled = !isInputActive,
                        interactionSource = activationGateInteractionSource,
                    )
                    .applyIf(!isInputActive) {
                        pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                                activateInput()
                            }
                        }
                    }
            }
    ) {
        WireTextField(
            textState = messageTextState,
            colors = colors,
            textStyle = MaterialTheme.wireTypography.body01,
            // Add an extra space so that the cursor is placed one space before "Type a message"
            placeholderText = " $placeHolderText",
            state = WireTextFieldState.Default,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyBoardAction,
            modifier = Modifier
                .fillMaxWidth()
                .applyIf(!useKeyboardActivationGate) {
                    focusable(true)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onFocused()
                            }
                        }
                },
            inputModifier = Modifier
                .applyIf(useKeyboardActivationGate) {
                    focusRequester(inputFocusRequester)
                        .focusProperties { canFocus = isInputActive }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.Escape -> onHardwareEscape()

                        event.key == Key.Tab -> onHardwareTab(event.isShiftPressed) ||
                            focusManager.moveFocus(
                                if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
                            )

                        event.key == Key.Enter || event.key == Key.NumPadEnter ->
                            onHardwareEnter(event.isShiftPressed)

                        else -> false
                    }
                },
            interactionSource = interactionSource,
            onSelectedLineIndexChanged = onSelectedLineIndexChanged,
            onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
            onTap = null,
            lineLimits = TextFieldLineLimits.Default,
        )
    }
}

private inline fun KeyEvent.handleInputActivation(
    isInputActive: Boolean,
    activateInput: (initialText: String?) -> Unit,
): Boolean = if (isInputActive || type != KeyEventType.KeyDown) {
    false
} else {
    when (key) {
        Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
            activateInput(null)
            true
        }

        else -> printableCharacter?.let { initialText ->
            activateInput(initialText)
            true
        } ?: false
    }
}

private val KeyEvent.printableCharacter: String?
    get() {
        if (type != KeyEventType.KeyDown || key in NON_PRINTABLE_COMPOSER_KEYS) return null
        return unicodeCharToPrintableString(utf16CodePoint)
    }

internal fun unicodeCharToPrintableString(unicodeChar: Int): String? {
    val codePoint = unicodeChar and KeyCharacterMap.COMBINING_ACCENT_MASK
    return codePoint
        .takeIf { it != 0 && Character.isValidCodePoint(it) }
        ?.let { String(Character.toChars(it)) }
}

private val NON_PRINTABLE_COMPOSER_KEYS = setOf(
    Key.Tab,
    Key.DirectionUp,
    Key.DirectionDown,
    Key.DirectionLeft,
    Key.DirectionRight,
    Key.Back,
    Key.Escape,
    Key.Backspace,
    Key.Delete,
    Key.Enter,
    Key.NumPadEnter,
    Key.MoveHome,
    Key.MoveEnd,
    Key.PageUp,
    Key.PageDown,
)

@Composable
fun CollapseButton(
    isCollapsed: Boolean,
    onCollapseClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val collapseButtonRotationDegree by animateFloatAsState(targetValue = if (isCollapsed) 0F else 180f)

        IconButton(
            onClick = {
                onCollapseClick()
            },
            modifier = Modifier
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .size(20.dp)
        ) {
            Icon(
                painter = painterResource(id = com.wire.android.ui.common.R.drawable.ic_collapse),
                contentDescription = if (isCollapsed) {
                    stringResource(R.string.content_description_expand_text_icon)
                } else {
                    stringResource(R.string.content_description_collapse_text_icon)
                },
                tint = colorsScheme().onSecondaryButtonDisabled,
                modifier = Modifier.rotate(collapseButtonRotationDegree)
            )
        }
    }
}

@Composable
private fun PreviewActiveMessageComposerInput(inputType: InputType, isTextExpanded: Boolean) {
    ActiveMessageComposerInput(
        conversationId = ConversationId("conversationId", "domain"),
        messageComposition = MessageComposition(ConversationId("conversationId", "domain")),
        messageTextState = TextFieldState(""),
        isTextExpanded = isTextExpanded,
        inputType = inputType,
        keyboardOptions = KeyboardOptions.Companion.MessageComposerDefault,
        onKeyboardAction = null,
        onHardwareEnter = { false },
        canSendMessage = true,
        focusRequester = remember { FocusRequester() },
        onSendButtonClicked = {},
        onEditButtonClicked = {},
        onChangeSelfDeletionClicked = {},
        onToggleInputSize = {},
        onCancelReply = {},
        onCancelEdit = {},
        onFocused = {},
        onSelectedLineIndexChanged = {},
        onLineBottomYCoordinateChanged = {},
        showOptions = true,
        showInlinePlusButton = false,
        optionsSelected = true,
        onPlusClick = {},
        useKeyboardActivationGate = false,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewActiveMessageComposerInputCollapsed() = WireTheme {
    PreviewActiveMessageComposerInput(
        inputType = InputType.Composing(isSendButtonEnabled = true),
        isTextExpanded = false
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewActiveMessageComposerInputCollapsedEdit() = WireTheme {
    PreviewActiveMessageComposerInput(
        inputType = InputType.Editing(isEditButtonEnabled = true),
        isTextExpanded = false
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewActiveMessageComposerInputExpanded() = WireTheme {
    PreviewActiveMessageComposerInput(
        inputType = InputType.Composing(isSendButtonEnabled = true),
        isTextExpanded = true
    )
}
