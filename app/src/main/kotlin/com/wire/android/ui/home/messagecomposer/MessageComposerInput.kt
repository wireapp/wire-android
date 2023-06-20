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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.SecurityClassificationBanner
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.messages.QuotedMessagePreview
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.messagecomposer.state.MessageComposeInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageComposeInputState
import com.wire.android.ui.home.messagecomposer.state.MessageComposeInputType
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import io.github.esentsov.PackagePrivate

@PackagePrivate
@Composable
internal fun MessageComposerInput(
    transition: Transition<MessageComposeInputState>,
    interactionAvailability: InteractionAvailability,
    securityClassificationType: SecurityClassificationType,
    messageComposeInputState: MessageComposeInputState,
    quotedMessageData: UIQuotedMessage.UIQuotedData?,
    membersToMention: List<Contact>,
    actions: MessageComposerInputActions,
    inputFocusRequester: FocusRequester,
    isFileSharingEnabled: Boolean,
    showSelfDeletingOption: Boolean
) {
    when (interactionAvailability) {
        InteractionAvailability.BLOCKED_USER -> BlockedUserComposerInput(securityClassificationType)
        InteractionAvailability.DELETED_USER -> DeletedUserComposerInput(securityClassificationType)
        InteractionAvailability.NOT_MEMBER, InteractionAvailability.DISABLED ->
            MessageComposerClassifiedBanner(securityClassificationType, PaddingValues(vertical = dimensions().spacing16x))

        InteractionAvailability.ENABLED -> {
            EnabledMessageComposerInput(
                transition = transition,
                securityClassificationType = securityClassificationType,
                messageComposeInputState = messageComposeInputState,
                quotedMessageData = quotedMessageData,
                membersToMention = membersToMention,
                actions = actions,
                inputFocusRequester = inputFocusRequester,
                isFileSharingEnabled = isFileSharingEnabled,
                showSelfDeletingOption = showSelfDeletingOption
            )
        }
    }
}

@Composable
private fun EnabledMessageComposerInput(
    transition: Transition<MessageComposeInputState>,
    securityClassificationType: SecurityClassificationType,
    messageComposeInputState: MessageComposeInputState,
    quotedMessageData: UIQuotedMessage.UIQuotedData?,
    membersToMention: List<Contact>,
    actions: MessageComposerInputActions,
    inputFocusRequester: FocusRequester,
    isFileSharingEnabled: Boolean,
    showSelfDeletingOption: Boolean
) {
    Box {
        var currentSelectedLineIndex by remember { mutableStateOf(0) }
        var cursorCoordinateY by remember { mutableStateOf(0F) }
        Column {
            MessageComposeInput(
                transition = transition,
                messageComposeInputState = messageComposeInputState,
                quotedMessageData = quotedMessageData,
                securityClassificationType = securityClassificationType,
                onSelectedLineIndexChange = { currentSelectedLineIndex = it },
                onLineBottomCoordinateChange = { cursorCoordinateY = it },
                actions = actions,
                inputFocusRequester = inputFocusRequester,
                isFileSharingEnabled = isFileSharingEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .let {
                        if (messageComposeInputState.isExpanded) it.weight(1f)
                        else it.wrapContentHeight()
                    }
            )
            MessageComposeActionsBox(
                transition = transition,
                isMentionActive = membersToMention.isNotEmpty(),
                isFileSharingEnabled = isFileSharingEnabled,
                startMention = actions.startMention,
                onAdditionalOptionButtonClicked = actions.onAdditionalOptionButtonClicked,
                modifier = Modifier.background(colorsScheme().messageComposerBackgroundColor),
                onPingClicked = actions.onPingClicked,
                onSelfDeletionOptionButtonClicked = actions.onSelfDeletionOptionButtonClicked,
                showSelfDeletingOption = showSelfDeletingOption,
                onRichTextEditingButtonClicked = actions.onRichTextEditingButtonClicked,
                onCloseRichTextEditingButtonClicked = actions.onCloseRichTextEditingButtonClicked,
                onRichTextEditingHeaderButtonClicked = actions.toRichTextEditingHeader,
                onRichTextEditingBoldButtonClicked = actions.toRichTextEditingBold,
                onRichTextEditingItalicButtonClicked = actions.toRichTextEditingItalic
            )
        }
        if (membersToMention.isNotEmpty() && messageComposeInputState.isExpanded) {
            DropDownMentionsSuggestions(currentSelectedLineIndex, cursorCoordinateY, membersToMention, actions.onMentionPicked)
        }
    }
}

@Composable
private fun MessageComposeInput(
    transition: Transition<MessageComposeInputState>,
    messageComposeInputState: MessageComposeInputState,
    quotedMessageData: UIQuotedMessage.UIQuotedData?,
    securityClassificationType: SecurityClassificationType,
    onSelectedLineIndexChange: (Int) -> Unit,
    onLineBottomCoordinateChange: (Float) -> Unit,
    actions: MessageComposerInputActions,
    inputFocusRequester: FocusRequester,
    isFileSharingEnabled: Boolean,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .background(
                if (messageComposeInputState.isEditMessage) colorsScheme().messageComposerEditBackgroundColor
                else colorsScheme().messageComposerBackgroundColor
            )
    ) {
        val isClassifiedConversation = securityClassificationType != SecurityClassificationType.NONE
        if (isClassifiedConversation) {
            Box(Modifier.wrapContentSize()) {
                VerticalSpace.x8()
                SecurityClassificationBanner(securityClassificationType = securityClassificationType)
            }
        }
        Divider(color = MaterialTheme.wireColorScheme.outline)
        CollapseIconButtonBox(
            transition = transition,
            toggleFullScreen = actions.onToggleFullScreen
        )

        if (quotedMessageData != null) {
            Row(modifier = Modifier.padding(horizontal = dimensions().spacing8x)) {
                QuotedMessagePreview(
                    quotedMessageData = quotedMessageData,
                    onCancelReply = actions.onCancelReply
                )
            }
        }
        // Row wrapping the AdditionalOptionButton() when we are in Enabled state and MessageComposerInput()
        // when we are in the Fullscreen state, we want to align the TextField to Top of the Row,
        // when other we center it vertically. Once we go to Fullscreen, we set the weight to 1f
        // so that it fills the whole Row which is = height of the whole screen - height of TopBar -
        // - height of container with additional options
        MessageComposerInputRow(
            transition = transition,
            messageComposeInputState = messageComposeInputState,
            onMessageTextChanged = actions.onMessageTextChanged,
            onInputFocusChanged = actions.onInputFocusChanged,
            focusRequester = inputFocusRequester,
            onSendButtonClicked = actions.onSendButtonClicked,
            onSelectedLineIndexChanged = onSelectedLineIndexChange,
            onLineBottomYCoordinateChanged = onLineBottomCoordinateChange,
            onAdditionalOptionButtonClicked = actions.onAdditionalOptionButtonClicked,
            onEditCancelButtonClicked = actions.onEditCancelButtonClicked,
            onEditSaveButtonClicked = actions.onEditSaveButtonClicked,
            onChangeSelfDeletionTimeClicked = actions.onSelfDeletionOptionButtonClicked,
            isFileSharingEnabled = isFileSharingEnabled,
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CollapseIconButtonBox(
    transition: Transition<MessageComposeInputState>,
    toggleFullScreen: () -> Unit
) {
    transition.AnimatedVisibility(visible = { state -> (state is MessageComposeInputState.Active) }) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            val collapseButtonRotationDegree by transition.animateFloat(
                label = stringResource(R.string.animation_label_button_rotation_degree_transition)
            ) { state ->
                if (state.isExpanded) 180f
                else 0f
            }
            CollapseIconButton(
                onCollapseClick = toggleFullScreen,
                collapseRotation = collapseButtonRotationDegree
            )
        }
    }
}

@Composable
private fun CollapseIconButton(onCollapseClick: () -> Unit, modifier: Modifier = Modifier, collapseRotation: Float = 0f) {
    IconButton(
        onClick = onCollapseClick,
        modifier = modifier.size(20.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_collapse),
            contentDescription = stringResource(R.string.content_description_drop_down_icon),
            tint = colorsScheme().onSecondaryButtonDisabled,
            modifier = Modifier.rotate(collapseRotation)
        )
    }
}

data class MessageComposerInputActions(
    val onMessageTextChanged: (TextFieldValue) -> Unit = {},
    val onSendButtonClicked: () -> Unit = {},
    val onToggleFullScreen: () -> Unit = {},
    val onMentionPicked: (Contact) -> Unit = {},
    val onCancelReply: () -> Unit = {},
    val startMention: () -> Unit = {},
    val onInputFocusChanged: (Boolean) -> Unit = {},
    val onAdditionalOptionButtonClicked: () -> Unit = {},
    val onEditSaveButtonClicked: () -> Unit = {},
    val onEditCancelButtonClicked: () -> Unit = {},
    val onPingClicked: () -> Unit = {},
    val onSelfDeletionOptionButtonClicked: () -> Unit = { },
    val onSendSelfDeletingMessageClicked: () -> Unit = {},
    val onRichTextEditingButtonClicked: () -> Unit = {},
    val onCloseRichTextEditingButtonClicked: () -> Unit = {},
    val toRichTextEditingHeader: () -> Unit = {},
    val toRichTextEditingBold: () -> Unit = {},
    val toRichTextEditingItalic: () -> Unit = {}
)

@Composable
private fun generatePreviewWithState(state: MessageComposeInputState) {
    EnabledMessageComposerInput(
        transition = updateTransition(targetState = state, label = ""),
        securityClassificationType = SecurityClassificationType.NONE,
        messageComposeInputState = state,
        quotedMessageData = null,
        membersToMention = listOf(),
        actions = MessageComposerInputActions(),
        inputFocusRequester = FocusRequester(),
        isFileSharingEnabled = true,
        showSelfDeletingOption = true
    )
}

@Preview
@Composable
fun PreviewEnabledMessageComposerInputInactive() {
    generatePreviewWithState(MessageComposeInputState.Inactive())
}

@Preview
@Composable
fun PreviewEnabledMessageComposerInputActiveCollapsed() {
    generatePreviewWithState(MessageComposeInputState.Active(size = MessageComposeInputSize.COLLAPSED))
}

@Preview
@Composable
fun PreviewEnabledMessageComposerInputActiveExpanded() {
    generatePreviewWithState(MessageComposeInputState.Active(size = MessageComposeInputSize.EXPANDED))
}

@Preview
@Composable
fun PreviewEnabledMessageComposerInputActiveEdit() {
    generatePreviewWithState(
        MessageComposeInputState.Active(
            type = MessageComposeInputType.EditMessage(
                "",
                ""
            )
        )
    )
}
