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

package com.wire.android.ui.home.messagecomposer

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.home.conversations.messages.QuotedMessagePreview
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputType
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

//@PackagePrivate
//@Composable
//internal fun MessageComposerInput(
//    transition: Transition<MessageComposeInputState>,
//    interactionAvailability: InteractionAvailability,
//    securityClassificationType: SecurityClassificationType,
//    messageComposeInputState: MessageComposeInputState,
//    quotedMessageData: UIQuotedMessage.UIQuotedData?,
//    membersToMention: List<Contact>,
//    actions: MessageComposerInputActions,
//    inputFocusRequester: FocusRequester,
//    isFileSharingEnabled: Boolean,
//    showSelfDeletingOption: Boolean
//) {
//    when (interactionAvailability) {
//        InteractionAvailability.BLOCKED_USER -> BlockedUserComposerInput(securityClassificationType)
//        InteractionAvailability.DELETED_USER -> DeletedUserComposerInput(securityClassificationType)
//        InteractionAvailability.NOT_MEMBER, InteractionAvailability.DISABLED ->
//            MessageComposerClassifiedBanner(securityClassificationType, PaddingValues(vertical = dimensions().spacing16x))
//
//        InteractionAvailability.ENABLED -> {
//            EnabledMessageComposerInput(
//                transition = transition,
//                securityClassificationType = securityClassificationType,
//                messageComposeInputState = messageComposeInputState,
//                quotedMessageData = quotedMessageData,
//                membersToMention = membersToMention,
//                actions = actions,
//                inputFocusRequester = inputFocusRequester,
//                isFileSharingEnabled = isFileSharingEnabled,
//                showSelfDeletingOption = showSelfDeletingOption
//            )
//        }
//    }
//}
//
//@Composable
//private fun EnabledMessageComposerInput(
//    transition: Transition<MessageComposeInputState>,
//    securityClassificationType: SecurityClassificationType,
//    messageComposeInputState: MessageComposeInputState,
//    quotedMessageData: UIQuotedMessage.UIQuotedData?,
//    membersToMention: List<Contact>,
//    actions: MessageComposerInputActions,
//    inputFocusRequester: FocusRequester,
//    isFileSharingEnabled: Boolean,
//    showSelfDeletingOption: Boolean
//) {
//    Box {
//        var currentSelectedLineIndex by remember { mutableStateOf(0) }
//        var cursorCoordinateY by remember { mutableStateOf(0F) }
//        Column {
//            MessageComposeInput(
//                transition = transition,
//                messageComposeInputState = messageComposeInputState,
//                quotedMessageData = quotedMessageData,
//                securityClassificationType = securityClassificationType,
//                onSelectedLineIndexChange = { currentSelectedLineIndex = it },
//                onLineBottomCoordinateChange = { cursorCoordinateY = it },
//                actions = actions,
//                inputFocusRequester = inputFocusRequester,
//                isFileSharingEnabled = isFileSharingEnabled,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .let {
//                        if (messageComposeInputState.isExpanded) it.weight(1f)
//                        else it.wrapContentHeight()
//                    }
//            )
//            MessageComposeActionsBox(
//                transition = transition,
//                isMentionActive = membersToMention.isNotEmpty(),
//                isFileSharingEnabled = isFileSharingEnabled,
//                startMention = actions.startMention,
//                onAdditionalOptionButtonClicked = actions.onAdditionalOptionButtonClicked,
//                modifier = Modifier.background(colorsScheme().messageComposerBackgroundColor),
//                onPingClicked = actions.onPingClicked,
//                onSelfDeletionOptionButtonClicked = actions.onSelfDeletionOptionButtonClicked,
//                showSelfDeletingOption = true,
//                onGifButtonClicked = {}
//            )
//        }
//        if (membersToMention.isNotEmpty() && messageComposeInputState.isExpanded) {
//            DropDownMentionsSuggestions(currentSelectedLineIndex, cursorCoordinateY, membersToMention, actions.onMentionPicked)
//        }
//    }
//}
//
//@Composable
//private fun MessageComposeInput(
//    transition: Transition<MessageComposeInputState>,
//    messageComposeInputState: MessageComposeInputState,
//    quotedMessageData: UIQuotedMessage.UIQuotedData?,
//    securityClassificationType: SecurityClassificationType,
//    onSelectedLineIndexChange: (Int) -> Unit,
//    onLineBottomCoordinateChange: (Float) -> Unit,
//    actions: MessageComposerInputActions,
//    inputFocusRequester: FocusRequester,
//    isFileSharingEnabled: Boolean,
//    modifier: Modifier
//) {
//    Column(
//        modifier = modifier
//            .background(
//                if (messageComposeInputState.isEditMessage) colorsScheme().messageComposerEditBackgroundColor
//                else colorsScheme().messageComposerBackgroundColor
//            )
//    ) {
//        val isClassifiedConversation = securityClassificationType != SecurityClassificationType.NONE
//        if (isClassifiedConversation) {
//            Box(Modifier.wrapContentSize()) {
//                VerticalSpace.x8()
//                SecurityClassificationBanner(securityClassificationType = securityClassificationType)
//            }
//        }
//        Divider(color = MaterialTheme.wireColorScheme.outline)
//        CollapseIconButtonBox(
//            transition = transition,
//            toggleFullScreen = actions.onToggleFullScreen
//        )
//
//        if (quotedMessageData != null) {
//            Row(modifier = Modifier.padding(horizontal = dimensions().spacing8x)) {
//                QuotedMessagePreview(
//                    quotedMessageData = quotedMessageData,
//                    onCancelReply = actions.onCancelReply
//                )
//            }
//        }
//        // Row wrapping the AdditionalOptionButton() when we are in Enabled state and MessageComposerInput()
//        // when we are in the Fullscreen state, we want to align the TextField to Top of the Row,
//        // when other we center it vertically. Once we go to Fullscreen, we set the weight to 1f
//        // so that it fills the whole Row which is = height of the whole screen - height of TopBar -
//        // - height of container with additional options
//        MessageComposerInputRow(
//            transition = transition,
//            messageComposeInputState = messageComposeInputState,
//            onMessageTextChanged = actions.onMessageTextChanged,
//            onInputFocusChanged = actions.onInputFocusChanged,
//            focusRequester = inputFocusRequester,
//            onSendButtonClicked = actions.onSendButtonClicked,
//            onSelectedLineIndexChanged = onSelectedLineIndexChange,
//            onLineBottomYCoordinateChanged = onLineBottomCoordinateChange,
//            onAdditionalOptionButtonClicked = actions.onAdditionalOptionButtonClicked,
//            onEditCancelButtonClicked = actions.onEditCancelButtonClicked,
//            onEditSaveButtonClicked = actions.onEditSaveButtonClicked,
//            onChangeSelfDeletionTimeClicked = actions.onSelfDeletionOptionButtonClicked,
//            isFileSharingEnabled = isFileSharingEnabled,
//        )
//    }
//}

@Composable
fun InActiveMessageComposerInput(messageText: TextFieldValue, onMessageComposerFocused: () -> Unit) {
    MessageComposerTextInput(
        inputFocused = false,
        colors = wireTextFieldColors(
            backgroundColor = Color.Transparent,
            borderColor = Color.Transparent,
            focusColor = Color.Transparent,
            placeholderColor = colorsScheme().secondaryText
        ),
        messageText = messageText,
        onMessageTextChanged = { },
        singleLine = false,
        onFocusChanged = { isFocused ->
            if (isFocused) {
                onMessageComposerFocused()
            }
        }
    )
}

@Composable
fun ActiveMessageComposerInput(
    inputFocused: Boolean,
    securityClassificationType: SecurityClassificationType,
    interactionAvailability: InteractionAvailability,
    messageCompositionInputState: MessageCompositionInputType,
    messageCompositionInputSize: MessageCompositionInputSize,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    onSendButtonClicked: () -> Unit,
    onCollapseButtonClicked: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    with(messageCompositionInputState) {
        Column(
            modifier = modifier
        ) {
            CollapseButton(
                onCollapseClick = {
                    onCollapseButtonClicked()
                }
            )

            val quotedMessage = messageCompositionState.value.quotedMessage
            if (quotedMessage != null) {
                Row(modifier = Modifier.padding(horizontal = dimensions().spacing8x)) {
                    QuotedMessagePreview(
                        quotedMessageData = quotedMessage,
                        onCancelReply = {}
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalAlignment = Alignment.Bottom
            ) {
                val stretchToMaxParentConstraintHeightOrWithInBoundary = when (messageCompositionInputSize) {
                    MessageCompositionInputSize.COLLAPSED -> Modifier.heightIn(max = dimensions().messageComposerActiveInputMaxHeight)
                    MessageCompositionInputSize.EXPANDED -> Modifier.fillMaxHeight()
                }.weight(1f)

                MessageComposerTextInput(
                    inputFocused = inputFocused,
                    colors = messageCompositionInputState.inputTextColor(),
                    messageText = messageCompositionInputState.messageCompositionState.value.messageTextFieldValue,
                    onMessageTextChanged = onMessageTextChanged,
                    singleLine = false,
                    onFocusChanged = { isFocused ->
                        if (isFocused) onFocused()
                    },
                    modifier = stretchToMaxParentConstraintHeightOrWithInBoundary
                )
                Row(Modifier.wrapContentSize()) {
                    when (messageCompositionInputState) {
                        is MessageCompositionInputType.Composing -> MessageSendActions(
                            onSendButtonClicked = onSendButtonClicked,
                            sendButtonEnabled = messageCompositionInputState.isSendButtonEnabled
                        )

                        is MessageCompositionInputType.SelfDeleting -> SelfDeletingActions(
                            selfDeletionTimer = messageCompositionInputState.messageCompositionState.value.selfDeletionTimer,
                            sendButtonEnabled = messageCompositionInputState.isSendButtonEnabled,
                            onSendButtonClicked = onSendButtonClicked,
                            onChangeSelfDeletionClicked = messageCompositionInputState::showSelfDeletingTimeOption
                        )

                        else -> {}
                    }
                }
            }
            when (messageCompositionInputState) {
                is MessageCompositionInputType.Editing -> {
                    MessageEditActions(
                        onEditSaveButtonClicked = { },
                        onEditCancelButtonClicked = {},
                        editButtonEnabled = messageCompositionInputState.isEditButtonEnabled
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun MessageComposerTextInput(
    inputFocused: Boolean,
    colors: WireTextFieldColors,
    singleLine: Boolean,
    messageText: TextFieldValue,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(inputFocused) {
        if (inputFocused) focusRequester.requestFocus()
        else focusManager.clearFocus()
    }


    WireTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        colors = colors,
        singleLine = singleLine,
        maxLines = Int.MAX_VALUE,
        textStyle = MaterialTheme.wireTypography.body01,
        // Add an extra space so that the cursor is placed one space before "Type a message"
        placeholderText = " " + stringResource(R.string.label_type_a_message),
        modifier = modifier.then(
            Modifier
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                }
                .focusRequester(focusRequester)
        ),
        onSelectedLineIndexChanged = onSelectedLineIndexChanged,
        onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged
    )
}


@Composable
fun BlockedUserComposerInput(securityClassificationType: SecurityClassificationType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorsScheme().backgroundVariant)
            .padding(dimensions().spacing16x)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_conversation),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = "",
            modifier = Modifier
                .padding(start = dimensions().spacing8x)
                .size(dimensions().spacing12x)
        )
        Text(
            text = LocalContext.current.resources.stringWithStyledArgs(
                R.string.label_system_message_blocked_user,
                MaterialTheme.wireTypography.body01,
                MaterialTheme.wireTypography.body02,
                colorsScheme().secondaryText,
                colorsScheme().onBackground,
                stringResource(id = R.string.member_name_you_label_titlecase)
            ),
            style = MaterialTheme.wireTypography.body01,
            maxLines = 1,
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .padding(start = dimensions().spacing16x)
        )
    }
    MessageComposerClassifiedBanner(securityClassificationType = securityClassificationType)
}

@Composable
fun DeletedUserComposerInput(securityClassificationType: SecurityClassificationType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorsScheme().backgroundVariant)
            .padding(dimensions().spacing16x)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_conversation),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = "",
            modifier = Modifier
                .padding(start = dimensions().spacing8x)
                .size(dimensions().spacing12x)
        )
        Text(
            text = LocalContext.current.resources.stringWithStyledArgs(
                R.string.label_system_message_user_not_available,
                MaterialTheme.wireTypography.body01,
                MaterialTheme.wireTypography.body02,
                colorsScheme().secondaryText,
                colorsScheme().onBackground,
            ),
            style = MaterialTheme.wireTypography.body01,
            maxLines = 1,
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .padding(start = dimensions().spacing16x)
        )
    }
    MessageComposerClassifiedBanner(securityClassificationType = securityClassificationType)
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CollapseButton(
    onCollapseClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
//        val collapseButtonRotationDegree by animateFloatAsState(
//            label = stringResource(R.string.animation_label_button_rotation_degree_transition)
//        ) { state ->
//            if (state.isExpanded) 180f
//            else 0f
//        }
        IconButton(
            onClick = onCollapseClick,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_collapse),
                contentDescription = stringResource(R.string.content_description_drop_down_icon),
                tint = colorsScheme().onSecondaryButtonDisabled,
                modifier = Modifier.rotate(180f)
            )
        }
    }
}
