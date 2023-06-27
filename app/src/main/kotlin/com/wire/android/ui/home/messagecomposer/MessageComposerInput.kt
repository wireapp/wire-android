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

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.wire.android.ui.common.SecurityClassificationBanner
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.home.conversations.messages.QuotedMessagePreview
import com.wire.android.ui.home.messagecomposer.state.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputType
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@Composable
fun InActiveMessageComposerInput(
    messageText: TextFieldValue,
    onMessageComposerFocused: () -> Unit
) {
    MessageComposerTextInput(
        inputFocused = false,
        colors = wireTextFieldColors(
            backgroundColor = Color.Transparent,
            borderColor = Color.Transparent,
            focusColor = Color.Transparent,
            placeholderColor = colorsScheme().secondaryText
        ),
        messageText = messageText,
        onMessageTextChanged = {
            // non functional
        },
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
    messageComposition: MessageComposition,
    inputSize: MessageCompositionInputSize,
    inputType: MessageCompositionInputType,
    inputFocused: Boolean,
    securityClassificationType: SecurityClassificationType,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    onSendButtonClicked: () -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onToggleInputSize: () -> Unit,
    onCancelReply: () -> Unit,
    onInputFocusedChanged: (Boolean) -> Unit,
    onMentionPicked: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    Box {
        var currentSelectedLineIndex by remember { mutableStateOf(0) }
        var cursorCoordinateY by remember { mutableStateOf(0F) }

        Column(
            modifier = modifier
        ) {
            val isClassifiedConversation = securityClassificationType != SecurityClassificationType.NONE
            if (isClassifiedConversation) {
                Box(Modifier.wrapContentSize()) {
                    VerticalSpace.x8()
                    SecurityClassificationBanner(securityClassificationType = securityClassificationType)
                }
            }
            Divider(color = MaterialTheme.wireColorScheme.outline)
            CollapseButton(
                onCollapseClick = onToggleInputSize
            )

            val quotedMessage = messageComposition.quotedMessage
            if (quotedMessage != null) {
                Row(modifier = Modifier.padding(horizontal = dimensions().spacing8x)) {
                    QuotedMessagePreview(
                        quotedMessageData = quotedMessage,
                        onCancelReply = onCancelReply
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalAlignment = Alignment.Bottom
            ) {
                val stretchToMaxParentConstraintHeightOrWithInBoundary = when (inputSize) {
                    MessageCompositionInputSize.COLLAPSED -> Modifier.heightIn(
                        max = dimensions().messageComposerActiveInputMaxHeight
                    )

                    MessageCompositionInputSize.EXPANDED -> Modifier.fillMaxHeight()
                }.weight(1f)

                MessageComposerTextInput(
                    inputFocused = inputFocused,
                    colors = inputType.inputTextColor(),
                    messageText = messageComposition.messageTextFieldValue,
                    onMessageTextChanged = onMessageTextChanged,
                    singleLine = false,
                    onFocusChanged = onInputFocusedChanged,
                    onSelectedLineIndexChanged = { currentSelectedLineIndex = it },
                    onLineBottomYCoordinateChanged = { cursorCoordinateY = it },
                    modifier = stretchToMaxParentConstraintHeightOrWithInBoundary
                )

                Row(Modifier.wrapContentSize()) {
                    when (inputType) {
                        is MessageCompositionInputType.Composing -> MessageSendActions(
                            onSendButtonClicked = onSendButtonClicked,
                            sendButtonEnabled = inputType.isSendButtonEnabled
                        )

                        is MessageCompositionInputType.SelfDeleting -> SelfDeletingActions(
                            selfDeletionTimer = messageComposition.selfDeletionTimer,
                            sendButtonEnabled = inputType.isSendButtonEnabled,
                            onSendButtonClicked = onSendButtonClicked,
                            onChangeSelfDeletionClicked = onChangeSelfDeletionClicked
                        )

                        else -> {}
                    }
                }
            }
            when (inputType) {
                is MessageCompositionInputType.Editing -> {
                    MessageEditActions(
                        onEditSaveButtonClicked = { },
                        onEditCancelButtonClicked = { },
                        editButtonEnabled = inputType.isEditButtonEnabled
                    )
                }

                else -> {}
            }
        }

        val mentionSearchResult = messageComposition.mentionSearchResult
        if (mentionSearchResult.isNotEmpty() && inputSize == MessageCompositionInputSize.EXPANDED) {
            DropDownMentionsSuggestions(
                currentSelectedLineIndex = currentSelectedLineIndex,
                cursorCoordinateY = cursorCoordinateY,
                membersToMention = mentionSearchResult,
                onMentionPicked = onMentionPicked
            )
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

    var focused by remember(inputFocused) { mutableStateOf(inputFocused) }

    LaunchedEffect(focused) {
        Log.d("TEST", "inputFocused: $focused")
        if (focused) focusRequester.requestFocus()
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

@Composable
private fun CollapseButton(
    onCollapseClick: () -> Unit
) {
    var isCollapsed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val collapseButtonRotationDegree by animateFloatAsState(targetValue = if (isCollapsed) 180f else 0f)

        IconButton(
            onClick = {
                isCollapsed = !isCollapsed
                onCollapseClick()
            },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_collapse),
                contentDescription = stringResource(R.string.content_description_drop_down_icon),
                tint = colorsScheme().onSecondaryButtonDisabled,
                modifier = Modifier.rotate(collapseButtonRotationDegree)
            )
        }
    }
}
