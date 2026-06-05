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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.atMost
import com.wire.android.R
import com.wire.android.feature.aiassistant.AiMessageToneType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.button.WireTertiaryIconButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
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
import com.wire.android.ui.theme.wireDimensions
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
    canSendMessage: Boolean,
    selfDeletionTimer: SelfDeletionTimer,
    activeAiAction: AiMessageComposerAction? = null,
    canUndo: Boolean = false,
    onSendButtonClicked: () -> Unit,
    onUndoButtonClicked: () -> Unit = {},
    onProofreadButtonClicked: () -> Unit = {},
    onAdjustToneButtonClicked: (AiMessageToneType) -> Unit = {},
    onCustomPromptButtonClicked: () -> Unit = {},
    onEditButtonClicked: () -> Unit,
    onChangeSelfDeletionClicked: (currentlySelected: SelfDeletionTimer) -> Unit,
    onToggleInputSize: () -> Unit,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit,
    onFocused: () -> Unit,
    onSelectedLineIndexChanged: (Int) -> Unit,
    onLineBottomYCoordinateChanged: (Float) -> Unit,
    showOptions: Boolean,
    optionsSelected: Boolean,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(inputType.backgroundColor())
    ) {
        HorizontalDivider(color = MaterialTheme.wireColorScheme.outline)
        if (showOptions) {
            CollapseButton(
                isCollapsed = !isTextExpanded,
                onCollapseClick = onToggleInputSize
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
            canUndo = canUndo,
            onUndoButtonClicked = onUndoButtonClicked,
            onProofreadButtonClicked = onProofreadButtonClicked,
            onAdjustToneButtonClicked = onAdjustToneButtonClicked,
            onCustomPromptButtonClicked = onCustomPromptButtonClicked,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            canSendMessage = canSendMessage,
            selfDeletionTimer = selfDeletionTimer,
            activeAiAction = activeAiAction,
            onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
            onFocused = onFocused,
            onSelectedLineIndexChanged = onSelectedLineIndexChanged,
            onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
            showOptions = showOptions,
            optionsSelected = optionsSelected,
            onPlusClick = onPlusClick,
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
    canSendMessage: Boolean,
    selfDeletionTimer: SelfDeletionTimer,
    activeAiAction: AiMessageComposerAction?,
    canUndo: Boolean,
    onSendButtonClicked: () -> Unit,
    onUndoButtonClicked: () -> Unit,
    onProofreadButtonClicked: () -> Unit,
    onAdjustToneButtonClicked: (AiMessageToneType) -> Unit,
    onCustomPromptButtonClicked: () -> Unit,
    onChangeSelfDeletionClicked: (currentlySelected: SelfDeletionTimer) -> Unit,
    onFocused: () -> Unit,
    onSelectedLineIndexChanged: (Int) -> Unit,
    onLineBottomYCoordinateChanged: (Float) -> Unit,
    showOptions: Boolean,
    optionsSelected: Boolean,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelfDeletingMessageActionViewModel =
        selfDeletingMessageActionViewModel(SelfDeletingMessageActionArgs(conversationId = conversationId)),
) {
    ConstraintLayout(modifier = modifier) {
        val (additionalOptionButton, input, actions) = createRefs()
        val aiActions = createRef()
        val isAiProcessing = activeAiAction != null
        val buttonsTopBarrier = createTopBarrier(additionalOptionButton, aiActions, actions)
        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier.constrainAs(additionalOptionButton) {
                start.linkTo(parent.start)
                bottom.linkTo(parent.bottom)
            }
        ) {
            if (!showOptions && inputType is InputType.Composing) {
                AdditionalOptionButton(
                    isSelected = optionsSelected,
                    onClick = onPlusClick,
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = dimensions().spacing8x)
                .constrainAs(aiActions) {
                    start.linkTo(parent.start)
                    end.linkTo(actions.start)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.fillToConstraints
                }
        ) {
            if (isTextExpanded && canUndo && !isAiProcessing) {
                UndoMessageAction(
                    onUndoButtonClicked = onUndoButtonClicked,
                    modifier = Modifier.semantics { testTag = AI_UNDO_BUTTON_TAG }
                )
            }
            if (isTextExpanded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isAiProcessing) {
                        ProcessingOnDeviceIndicator(
                            modifier = Modifier.semantics { testTag = AI_PROCESSING_INDICATOR_TAG }
                        )
                    } else {
                        ProofreadMessageAction(
                            onProofreadButtonClicked = onProofreadButtonClicked,
                            modifier = Modifier.semantics { testTag = AI_PROOFREAD_BUTTON_TAG }
                        )
                        AdjustToneMessageAction(
                            text = stringResource(R.string.label_adjust_tone_formal),
                            contentDescription = stringResource(R.string.content_description_adjust_tone_formal),
                            onButtonClicked = { onAdjustToneButtonClicked(AiMessageToneType.Formal) },
                            modifier = Modifier.semantics { testTag = AI_FORMAL_TONE_BUTTON_TAG }
                        )
                        AdjustToneMessageAction(
                            text = stringResource(R.string.label_adjust_tone_informal),
                            contentDescription = stringResource(R.string.content_description_adjust_tone_informal),
                            onButtonClicked = { onAdjustToneButtonClicked(AiMessageToneType.Informal) },
                            modifier = Modifier.semantics { testTag = AI_INFORMAL_TONE_BUTTON_TAG }
                        )
                        CustomPromptMessageAction(
                            onButtonClicked = onCustomPromptButtonClicked,
                            modifier = Modifier.semantics { testTag = AI_CUSTOM_PROMPT_BUTTON_TAG }
                        )
                    }
                }
            }
        }

        val collapsedMaxHeight = dimensions().messageComposerActiveInputMaxHeight
        MessageComposerTextInput(
            focusRequester = focusRequester,
            colors = inputType.inputTextColor(isSelfDeleting = selfDeletionTimer.duration != null),
            messageTextState = messageTextState,
            placeHolderText = selfDeletionTimer.duration?.let { stringResource(id = R.string.self_deleting_message_label) }
                ?: inputType.labelText(),
            onFocused = onFocused,
            onSelectedLineIndexChanged = onSelectedLineIndexChanged,
            onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
            keyboardOptions = keyboardOptions,
            onKeyBoardAction = onKeyboardAction,
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
                    UsersTypingIndicatorForConversation(conversationId = conversationId)
                }
            }
            // Only show send button when not in editing mode
            if (inputType !is InputType.Editing && !isAiProcessing) {
                MessageSendActions(
                    onSendButtonClicked = onSendButtonClicked,
                    sendButtonEnabled = canSendMessage,
                    selfDeletionTimer = selfDeletionTimer,
                    onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                    modifier = Modifier
                        .padding(end = dimensions().spacing8x)
                        .semantics { testTag = AI_SEND_ACTIONS_TAG }
                )
            }
        }
    }
}

@Composable
private fun ProofreadMessageAction(
    onProofreadButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireSecondaryIconButton(
        onButtonClicked = onProofreadButtonClicked,
        iconResource = R.drawable.ic_proofread,
        contentDescription = R.string.content_description_proofread_message,
        state = WireButtonState.Default,
        shape = CircleShape,
        minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
        minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        modifier = modifier
    )
}

@Composable
private fun UndoMessageAction(
    onUndoButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireTertiaryIconButton(
        onButtonClicked = onUndoButtonClicked,
        iconResource = R.drawable.ic_undo,
        contentDescription = R.string.content_description_undo_ai_action,
        state = WireButtonState.Default,
        shape = CircleShape,
        minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
        minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        modifier = modifier
    )
}

@Composable
private fun AdjustToneMessageAction(
    text: String,
    contentDescription: String,
    onButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireSecondaryButton(
        onClick = onButtonClicked,
        text = text,
        state = WireButtonState.Default,
        description = contentDescription,
        fillMaxWidth = false,
        minSize = MaterialTheme.wireDimensions.buttonMinSize.copy(height = MaterialTheme.wireDimensions.buttonCircleMinSize.height),
        minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        modifier = modifier
    )
}

@Composable
private fun CustomPromptMessageAction(
    onButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireSecondaryButton(
        onClick = onButtonClicked,
        text = stringResource(R.string.label_custom_prompt),
        state = WireButtonState.Default,
        description = stringResource(R.string.content_description_custom_prompt),
        fillMaxWidth = false,
        minSize = MaterialTheme.wireDimensions.buttonMinSize.copy(height = MaterialTheme.wireDimensions.buttonCircleMinSize.height),
        minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        modifier = modifier
    )
}

@Composable
private fun RowScope.ProcessingOnDeviceIndicator(
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
        modifier = modifier
            .weight(1f, fill = false)
            .padding(vertical = dimensions().spacing8x)
    ) {
        WireCircularProgressIndicator(progressColor = colorsScheme().primary)
        Text(
            text = stringResource(R.string.label_ai_processing_on_device),
            style = MaterialTheme.wireTypography.body02,
            color = colorsScheme().secondaryText,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MessageComposerTextInput(
    messageTextState: TextFieldState,
    focusRequester: FocusRequester,
    colors: WireTextFieldColors,
    placeHolderText: String,
    onFocused: () -> Unit,
    keyboardOptions: KeyboardOptions,
    onKeyBoardAction: KeyboardActionHandler?,
    modifier: Modifier = Modifier,
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onFocused()
        }
    }

    WireTextField(
        textState = messageTextState,
        colors = colors,
        textStyle = MaterialTheme.wireTypography.body01,
        // Add an extra space so that the cursor is placed one space before "Type a message"
        placeholderText = " $placeHolderText",
        state = WireTextFieldState.Default,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyBoardAction,
        modifier = modifier
            .focusable(true)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocused()
                }
            },
        interactionSource = interactionSource,
        onSelectedLineIndexChanged = onSelectedLineIndexChanged,
        onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
        lineLimits = TextFieldLineLimits.Default,
    )
}

@Composable
fun CollapseButton(
    isCollapsed: Boolean,
    onCollapseClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            modifier = Modifier.size(20.dp)
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
        canSendMessage = true,
        selfDeletionTimer = SelfDeletionTimer.Disabled,
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
        optionsSelected = true,
        onPlusClick = {}
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

internal const val AI_PROCESSING_INDICATOR_TAG = "ai_processing_indicator"
internal const val AI_PROOFREAD_BUTTON_TAG = "ai_proofread_button"
internal const val AI_FORMAL_TONE_BUTTON_TAG = "ai_formal_tone_button"
internal const val AI_INFORMAL_TONE_BUTTON_TAG = "ai_informal_tone_button"
internal const val AI_CUSTOM_PROMPT_BUTTON_TAG = "ai_custom_prompt_button"
internal const val AI_UNDO_BUTTON_TAG = "ai_undo_button"
internal const val AI_SEND_ACTIONS_TAG = "ai_send_actions"
