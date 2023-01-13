package com.wire.android.ui.home.messagecomposer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.SecurityClassificationBanner
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.mention.MemberItemToMention
import com.wire.android.ui.home.conversations.messages.QuotedMessagePreview
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.QuotedMessageUIData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.messagecomposer.attachment.AttachmentOptions
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import okio.Path

@Composable
fun MessageComposer(
    messageComposerState: MessageComposerInnerState,
    messageContent: @Composable () -> Unit,
    onSendTextMessage: (String, List<UiMention>, messageId: String?) -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onMentionMember: (String?) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    isFileSharingEnabled: Boolean,
    interactionAvailability: InteractionAvailability,
    tempCachePath: Path,
    securityClassificationType: SecurityClassificationType,
    membersToMention: List<Contact>
) {
    BoxWithConstraints {
        val onSendButtonClicked = remember {
            {
                onSendTextMessage(
                    messageComposerState.messageText.text,
                    messageComposerState.mentions,
                    messageComposerState.quotedMessageData?.messageId,
                )
                messageComposerState.quotedMessageData = null
                messageComposerState.setMessageTextValue(TextFieldValue(""))
            }
        }

        val onSendAttachmentClicked = remember {
            { attachmentBundle: AttachmentBundle? ->
                onSendAttachment(attachmentBundle)
                messageComposerState.toggleAttachmentOptionsVisibility()
            }
        }

        val onMentionPicked = remember {
            { contact: Contact ->
                messageComposerState.addMention(contact)
            }
        }

        LaunchedEffect(Unit) {
            messageComposerState.mentionQueryFlowState
                .collect { onMentionMember(it) }
        }

        MessageComposer(
            messagesContent = messageContent,
            messageComposerState = messageComposerState,
            isFileSharingEnabled = isFileSharingEnabled,
            tempCachePath = tempCachePath,
            interactionAvailability = interactionAvailability,
            membersToMention = membersToMention,
            onMessageComposerError = onMessageComposerError,
            onSendAttachmentClicked = onSendAttachmentClicked,
            securityClassificationType = securityClassificationType,
            onSendButtonClicked = onSendButtonClicked,
            onMentionPicked = onMentionPicked,
        )
    }
}

@Suppress("ComplexMethod", "ComplexCondition")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageComposer(
    messagesContent: @Composable () -> Unit,
    messageComposerState: MessageComposerInnerState,
    isFileSharingEnabled: Boolean,
    tempCachePath: Path,
    interactionAvailability: InteractionAvailability,
    membersToMention: List<Contact>,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    onSendAttachmentClicked: (AttachmentBundle?) -> Unit,
    securityClassificationType: SecurityClassificationType,
    onSendButtonClicked: () -> Unit,
    onMentionPicked: (Contact) -> Unit,
) {
    Surface(color = colorsScheme().messageComposerBackgroundColor) {
        val transition = updateTransition(
            targetState = messageComposerState.messageComposeInputState,
            label = stringResource(R.string.animation_label_message_compose_input_state_transition)
        )

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val currentScreenHeight: Dp = with(LocalDensity.current) { constraints.maxHeight.toDp() }

            Column(Modifier.fillMaxSize()) {

                // when MessageComposer is composed for the first time we do not know the height until users opens the keyboard
                var keyboardHeight: KeyboardHeight by remember { mutableStateOf(KeyboardHeight.NotKnown) }
                val isKeyboardVisible = WindowInsets.isImeVisible
                if (isKeyboardVisible) {
                    // ime covers also the navigation bar so we need to subtract navigation bars height
                    val calculatedImeHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                    val calculatedNavBarHeight = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
                    val calculatedKeyboardHeight = calculatedImeHeight - calculatedNavBarHeight
                    val notKnownAndCalculated = keyboardHeight is KeyboardHeight.NotKnown && calculatedKeyboardHeight > 0.dp
                    val knownAndDifferent = keyboardHeight is KeyboardHeight.Known && keyboardHeight.height != calculatedKeyboardHeight
                    if (notKnownAndCalculated || knownAndDifferent)
                        keyboardHeight = KeyboardHeight.Known(calculatedKeyboardHeight)
                }
                val attachmentOptionsVisible = messageComposerState.attachmentOptionsDisplayed
                        && !isKeyboardVisible
                        && interactionAvailability == InteractionAvailability.ENABLED
                val contentHeight by remember(keyboardHeight, currentScreenHeight, attachmentOptionsVisible) {
                    derivedStateOf {
                        if (attachmentOptionsVisible) currentScreenHeight - keyboardHeight.height
                        else currentScreenHeight
                    }
                }


                LaunchedEffect(isKeyboardVisible, messageComposerState.attachmentOptionsDisplayed) {
                    if (!isKeyboardVisible && !messageComposerState.attachmentOptionsDisplayed) {
                        messageComposerState.toEnabled()
                        messageComposerState.focusManager.clearFocus()
                    }
                }

                Column(
                    Modifier
                        .height(contentHeight)
                        .fillMaxWidth()
                ) {
                    Box(
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        messageComposerState.focusManager.clearFocus()
                                        messageComposerState.toEnabled()
                                    },
                                    onDoubleTap = { /* Called on Double Tap */ },
                                    onLongPress = { /* Called on Long Press */ },
                                    onTap = { /* Called on Tap */ }
                                )
                            }
                            .background(color = colorsScheme().backgroundVariant)
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        messagesContent()
                        if (membersToMention.isNotEmpty())
                            MembersMentionList(
                                membersToMention = membersToMention,
                                onMentionPicked = onMentionPicked
                            )
                    }

                    MessageComposerInput(
                        transition = transition,
                        messageComposerInnerState = messageComposerState,
                        interactionAvailability = interactionAvailability,
                        securityClassificationType = securityClassificationType,
                        membersToMention = membersToMention,
                        onMentionPicked = onMentionPicked,
                        onSendButtonClicked = onSendButtonClicked,
                        onToggleFullScreen = messageComposerState::toggleFullScreen,
                        onCancelReply = messageComposerState::cancelReply,
                    )
                }

                // Box wrapping for additional options content
                // we want to offset the AttachmentOptionsComponent equal to where
                // the device keyboard is displayed, so that when the keyboard is closed,
                // we get the effect of overlapping it
                if (attachmentOptionsVisible) {
                    AttachmentOptions(
                        attachmentInnerState = messageComposerState.attachmentInnerState,
                        onSendAttachment = onSendAttachmentClicked,
                        onMessageComposerError = onMessageComposerError,
                        isFileSharingEnabled = isFileSharingEnabled,
                        tempCachePath = tempCachePath,
                        modifier = Modifier
                            .height(keyboardHeight.height)
                            .fillMaxWidth()
                            .background(colorsScheme().messageComposerBackgroundColor)
                    )
                }
                // This covers the situation when the user switches from attachment options to the input keyboard - there is a moment when
                // both attachmentOptionsDisplayed and isKeyboardVisible are false, but right after that keyboard shows, so if we know that
                // the input already has a focus, we can show an empty Box which has a height of the keyboard to prevent flickering.

                else if (!messageComposerState.attachmentOptionsDisplayed && !isKeyboardVisible &&
                    messageComposerState.messageComposeInputFocused && interactionAvailability == InteractionAvailability.ENABLED
                ) {
                    Box(
                        modifier = Modifier
                            .height(keyboardHeight.height)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    BackHandler(messageComposerState.attachmentOptionsDisplayed) {
        messageComposerState.hideAttachmentOptions()
        messageComposerState.toEnabled()
    }
}

@Composable
private fun MessageComposerInput(
    transition: Transition<MessageComposeInputState>,
    interactionAvailability: InteractionAvailability,
    securityClassificationType: SecurityClassificationType,
    messageComposerInnerState: MessageComposerInnerState,
    membersToMention: List<Contact>,
    onSendButtonClicked: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onMentionPicked: (Contact) -> Unit,
    onCancelReply: () -> Unit
) {
    when (interactionAvailability) {
        InteractionAvailability.BLOCKED_USER -> BlockedUserComposerInput()
        InteractionAvailability.DELETED_USER -> DeletedUserComposerInput()
        InteractionAvailability.NOT_MEMBER, InteractionAvailability.DISABLED -> {}
        InteractionAvailability.ENABLED -> {
            EnabledMessageComposerInput(
                transition = transition,
                securityClassificationType = securityClassificationType,
                messageComposerInnerState = messageComposerInnerState,
                membersToMention = membersToMention,
                onSendButtonClicked = onSendButtonClicked,
                onToggleFullScreen = onToggleFullScreen,
                onMentionPicked = onMentionPicked,
                onCancelReply = onCancelReply
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EnabledMessageComposerInput(
    transition: Transition<MessageComposeInputState>,
    securityClassificationType: SecurityClassificationType,
    messageComposerInnerState: MessageComposerInnerState,
    membersToMention: List<Contact>,
    onSendButtonClicked: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onMentionPicked: (Contact) -> Unit,
    onCancelReply: () -> Unit
) {
    Column {
        var currentSelectedLineIndex by remember {
            mutableStateOf(0)
        }

        var cursorCoordinateY by remember {
            mutableStateOf(0F)
        }

        MessageComposeInput(
            transition = transition,
            messageComposerState = messageComposerInnerState,
            quotedMessageData = messageComposerInnerState.quotedMessageData,
            securityClassificationType = securityClassificationType,
            onCancelReply = onCancelReply,
            onToggleFullScreen = onToggleFullScreen,
            onSendButtonClicked = onSendButtonClicked,
            onSelectedLineIndexChange = { currentSelectedLineIndex = it },
            onLineBottomCoordinateChange = { cursorCoordinateY = it },
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (messageComposerInnerState.messageComposeInputState == MessageComposeInputState.FullScreen)
                        it.weight(1f)
                    else
                        it.wrapContentHeight()
                }
        )
        MessageComposeActionsBox(
            transition,
            messageComposerInnerState,
            membersToMention.isNotEmpty()
        )
        if (membersToMention.isNotEmpty()
            && messageComposerInnerState.messageComposeInputState == MessageComposeInputState.FullScreen
        ) {
            DropDownMentionsSuggestions(currentSelectedLineIndex, cursorCoordinateY, membersToMention, onMentionPicked)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MessageComposeInput(
    transition: Transition<MessageComposeInputState>,
    messageComposerState: MessageComposerInnerState,
    quotedMessageData: QuotedMessageUIData?,
    securityClassificationType: SecurityClassificationType,
    onCancelReply: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onSendButtonClicked: () -> Unit,
    onSelectedLineIndexChange: (Int) -> Unit,
    onLineBottomCoordinateChange: (Float) -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        val isClassifiedConversation = securityClassificationType != SecurityClassificationType.NONE
        if (isClassifiedConversation) {
            Box(Modifier.wrapContentSize()) {
                VerticalSpace.x8()
                SecurityClassificationBanner(securityClassificationType = securityClassificationType)
            }
        }
        Divider()
        CollapseIconButtonBox(
            transition = transition,
            toggleFullScreen = onToggleFullScreen
        )

        if (quotedMessageData != null) {
            Row(modifier = Modifier.padding(horizontal = dimensions().spacing8x)) {
                QuotedMessagePreview(
                    quotedMessageData = quotedMessageData,
                    onCancelReply = onCancelReply
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
            messageComposerState = messageComposerState,
            onSendButtonClicked = onSendButtonClicked,
            onSelectedLineIndexChanged = onSelectedLineIndexChange,
            onLineBottomYCoordinateChanged = onLineBottomCoordinateChange,
        )
    }
}

@Composable
private fun MembersMentionList(
    membersToMention: List<Contact>,
    onMentionPicked: (Contact) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (membersToMention.isNotEmpty())
            Divider()
        LazyColumn(
            modifier = Modifier.background(colorsScheme().background),
            reverseLayout = true
        ) {
            membersToMention.forEach {
                if (it.membership != Membership.Service) {
                    item {
                        MemberItemToMention(
                            avatarData = it.avatarData,
                            name = it.name,
                            label = it.label,
                            membership = it.membership,
                            clickable = Clickable(enabled = true) { onMentionPicked(it) },
                            modifier = Modifier
                        )
                        Divider(
                            color = MaterialTheme.wireColorScheme.divider,
                            thickness = Dp.Hairline
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun CollapseIconButtonBox(
    transition: Transition<MessageComposeInputState>,
    toggleFullScreen: () -> Unit
) {
    transition.AnimatedVisibility(visible = { state -> (state != MessageComposeInputState.Enabled) }) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            val collapseButtonRotationDegree by transition.animateFloat(
                label = stringResource(R.string.animation_label_button_rotation_degree_transition)
            ) { state ->
                when (state) {
                    MessageComposeInputState.Active, MessageComposeInputState.Enabled -> 0f
                    MessageComposeInputState.FullScreen -> 180f
                }
            }
            CollapseIconButton(
                onCollapseClick = toggleFullScreen,
                collapseRotation = collapseButtonRotationDegree
            )
        }
    }
}

// if attachment is visible we want to align the bottom of the compose actions
// to top of the guideline
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

sealed class KeyboardHeight(open val height: Dp) {
    object NotKnown : KeyboardHeight(DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET)
    data class Known(override val height: Dp) : KeyboardHeight(height)

    companion object {
        val DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET = 250.dp
    }
}
