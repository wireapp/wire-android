package com.wire.android.ui.home.messagecomposer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
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
    keyboardHeight: KeyboardHeight,
    content: @Composable () -> Unit,
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
        messageComposerState.fullScreenHeight = with(LocalDensity.current) { constraints.maxHeight.toDp() }

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
            content = content,
            keyboardHeight = keyboardHeight,
            messageComposerState = messageComposerState,
            onSendButtonClicked = onSendButtonClicked,
            onSendAttachmentClicked = onSendAttachmentClicked,
            onMessageComposerError = onMessageComposerError,
            isFileSharingEnabled = isFileSharingEnabled,
            interactionAvailability = interactionAvailability,
            tempCachePath = tempCachePath,
            securityClassificationType = securityClassificationType,
            membersToMention = membersToMention,
            onMentionPicked = onMentionPicked
        )
    }
}

/*
* Message composer is a UI widget that handles the UI logic of sending messages,
* it is a wrapper around the "hosting" widget. It receives a [messageText] and
* exposes a [onMessageChanged] lambda, giving us the option to control its Message Text from outside the Widget.
* it also exposes [onSendButtonClicked] lambda's giving us the option to handle the different message actions
* */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MessageComposer(
    content: @Composable () -> Unit,
    keyboardHeight: KeyboardHeight,
    messageComposerState: MessageComposerInnerState,
    onSendButtonClicked: () -> Unit,
    onSendAttachmentClicked: (AttachmentBundle?) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    isFileSharingEnabled: Boolean,
    interactionAvailability: InteractionAvailability,
    tempCachePath: Path,
    securityClassificationType: SecurityClassificationType,
    membersToMention: List<Contact>,
    onMentionPicked: (Contact) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val messageReplyState = messageComposerState.quotedMessageData

    Surface {
        val transition = updateTransition(
            targetState = messageComposerState.messageComposeInputState,
            label = stringResource(R.string.animation_label_messagecomposeinput_state_transistion)
        )
        // ConstraintLayout wrapping the whole content to give us the possibility to constrain SendButton to top of AdditionalOptions, which
        // constrains to bottom of MessageComposerInput
        // so that MessageComposerInput is the only component animating freely, when going to Fullscreen mode
        ConstraintLayout(Modifier.fillMaxSize()) {
            // This guide line is used was when the attachment options are visible
            // we need to use it to correctly offset the MessageComposerInput so that it is on a static place on the screen
            // to avoid reposition when the keyboard is hiding, this guideline makes space for the keyboard as well as for the
            // AttachmentOptions, the offset is set to DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET as default, whenever the keyboard pops up
            // we are able to calculate the actual needed offset, so that it is equal to the height of the keyboard the user is using
            val topOfKeyboardGuideLine = createGuidelineFromTop(
                offset = messageComposerState.fullScreenHeight - keyboardHeight.height
            )

            val messageComposer = createRef()

            ConstraintLayout(
                Modifier
                    .wrapContentSize()
                    .constrainAs(messageComposer) {
                        top.linkTo(parent.top)

                        if (messageComposerState.attachmentOptionsDisplayed) {
                            bottom.linkTo(topOfKeyboardGuideLine)
                        } else {
                            bottom.linkTo(parent.bottom)
                        }

                        height = Dimension.fillToConstraints
                    }) {

                val (additionalActions, sendActions, messageInput) = createRefs()
                // Column wrapping the content passed as Box with weight = 1f as @Composable lambda and the MessageComposerInput with
                // CollapseIconButton
                Column(
                    Modifier.constrainAs(messageInput) {
                        top.linkTo(parent.top)
                        // we want to align the elements to the guideline only when we display attachmentOptions
                        // or we are having focus on the TextInput field
                        bottom.linkTo(additionalActions.top)

                        height = Dimension.preferredWrapContent
                    }
                ) {
                    Box(
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        focusManager.clearFocus()
                                        messageComposerState.clickOutSideMessageComposer()
                                    },
                                    onDoubleTap = { /* Called on Double Tap */ },
                                    onLongPress = { /* Called on Long Press */ },
                                    onTap = {  /* Called on Tap */ }
                                )
                            }
                            .background(color = colorsScheme().backgroundVariant)
                            .padding(bottom = dimensions().spacing8x)
                            .weight(1f)) {
                        content()
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .animateContentSize()
                        ) {
                            Spacer(modifier = Modifier.weight(1f))

                            LazyColumn(
                                modifier = Modifier.background(Color.White),
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
                    when (interactionAvailability) {
                        InteractionAvailability.BLOCKED_USER -> BlockedUserMessage()
                        InteractionAvailability.DELETED_USER -> DeletedUserMessage()
                        InteractionAvailability.NOT_MEMBER, InteractionAvailability.DISABLED -> {}
                        InteractionAvailability.ENABLED -> {
                            // Column wrapping CollapseIconButton and MessageComposerInput
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .animateContentSize()
                            ) {
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
                                    messageComposerState = messageComposerState
                                )
                                if (messageReplyState != null) {
                                    Row(modifier = Modifier.padding(horizontal = dimensions().spacing8x)) {
                                        QuotedMessagePreview(
                                            quotedMessageData = messageReplyState,
                                            onCancelReply = messageComposerState::cancelReply
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
                                    membersToMention = membersToMention,
                                    onMentionPicked = onMentionPicked
                                )
                            }
                        }
                    }
                }
                if (interactionAvailability == InteractionAvailability.ENABLED) {
                    // Box wrapping the SendActions so that we do not include it in the animationContentSize
                    // changed which is applied only for
                    // MessageComposerInput and CollapsingButton
                    SendActions(
                        Modifier.constrainAs(sendActions) {
                            bottom.linkTo(additionalActions.top)
                            end.linkTo(parent.end)
                        },
                        messageComposerState,
                        transition,
                        onSendButtonClicked
                    )
                    // Box wrapping MessageComposeActions() so that we can constrain it to the bottom of MessageComposerInput and after that
                    // constrain our SendActions to it
                    MessageComposeActionsBox(
                        Modifier
                            .constrainAs(additionalActions) {
                                top.linkTo(messageInput.bottom)
                                bottom.linkTo(parent.bottom)
                            },
                        transition,
                        messageComposerState,
                        focusManager,
                        membersToMention.isNotEmpty()
                    )
                }
            }
            // Box wrapping for additional options content
            // we want to offset the AttachmentOptionsComponent equal to where
            // the device keyboard is displayed, so that when the keyboard is closed,
            // we get the effect of overlapping it
            if (messageComposerState.attachmentOptionsDisplayed && interactionAvailability == InteractionAvailability.ENABLED) {
                AttachmentOptions(
                    keyboardHeight = keyboardHeight,
                    messageComposerState = messageComposerState,
                    onSendAttachment = onSendAttachmentClicked,
                    onMessageComposerError = onMessageComposerError,
                    isFileSharingEnabled = isFileSharingEnabled,
                    tempCachePath = tempCachePath
                )
            }
        }
    }

    BackHandler(enabled = messageComposerState.attachmentOptionsDisplayed) {
        messageComposerState.toggleAttachmentOptionsVisibility()
    }
}

@ExperimentalAnimationApi
@Composable
private fun CollapseIconButtonBox(
    transition: Transition<MessageComposeInputState>,
    messageComposerState: MessageComposerInnerState
) {
    transition.AnimatedVisibility(visible = { state -> (state != MessageComposeInputState.Enabled) }) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            val collapseButtonRotationDegree by transition.animateFloat(
                label = stringResource(R.string.animation_label_button_rotation_degree_transistion)
            ) { state ->
                when (state) {
                    MessageComposeInputState.Active, MessageComposeInputState.Enabled -> 0f
                    MessageComposeInputState.FullScreen -> 180f
                }
            }
            CollapseIconButton(
                onCollapseClick = { messageComposerState.toggleFullScreen() },
                collapseRotation = collapseButtonRotationDegree
            )
        }
    }
}

// if attachment is visible we want to align the bottom of the compose actions
// to top of the guide line
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
