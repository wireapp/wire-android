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

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import com.wire.android.R
import com.wire.android.ui.common.TextWithLearnMore
import com.wire.android.ui.common.banner.SecurityClassificationBannerForConversation
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.messagecomposer.model.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.model.Ping
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputStateHolder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.PermissionDenialType
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.data.conversation.Conversation.TypingIndicatorMode
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import kotlin.math.roundToInt
import kotlin.time.Duration

@Composable
fun MessageComposer(
    conversationId: ConversationId,
    messageComposerStateHolder: MessageComposerStateHolder,
    messageListContent: @Composable () -> Unit,
    onSendMessageBundle: (MessageBundle) -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onClearMentionSearchResult: () -> Unit,
    onCaptureVideoPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit,
    tempWritableVideoUri: Uri?,
    tempWritableImageUri: Uri?,
    onImagesPicked: (List<Uri>) -> Unit
) {
    with(messageComposerStateHolder) {
        when (messageComposerViewState.value.interactionAvailability) {
            InteractionAvailability.BLOCKED_USER -> {
                DisabledInteractionMessageComposer(
                    conversationId = conversationId,
                    warningText = warningTextWithStyledArgs(
                        R.string.label_system_message_blocked_user,
                        stringResource(id = R.string.member_name_you_label_titlecase)
                    ),
                    messageListContent = messageListContent
                )
            }

            InteractionAvailability.DELETED_USER -> DisabledInteractionMessageComposer(
                conversationId = conversationId,
                warningText = warningTextWithStyledArgs(R.string.label_system_message_user_not_available),
                messageListContent = messageListContent
            )

            InteractionAvailability.UNSUPPORTED_PROTOCOL -> DisabledInteractionMessageComposer(
                conversationId = conversationId,
                warningText = warningTextWithStyledArgs(R.string.label_system_message_unsupported_protocol),
                messageListContent = messageListContent
            )

            InteractionAvailability.LEGAL_HOLD -> DisabledInteractionMessageComposer(
                conversationId = conversationId,
                warningText = warningTextWithStyledArgs(R.string.legal_hold_system_message_interaction_disabled),
                learnMoreLink = stringResource(id = R.string.url_legal_hold_learn_more),
                messageListContent = messageListContent
            )

            InteractionAvailability.NOT_MEMBER, InteractionAvailability.DISABLED -> DisabledInteractionMessageComposer(
                conversationId = conversationId,
                warningText = null,
                messageListContent = messageListContent
            )

            InteractionAvailability.ENABLED -> {
                EnabledMessageComposer(
                    conversationId = conversationId,
                    messageComposerStateHolder = messageComposerStateHolder,
                    messageListContent = messageListContent,
                    onSendButtonClicked = {
                        onSendMessageBundle(messageCompositionHolder.toMessageBundle(conversationId))
                        onClearMentionSearchResult()
                        clearMessage()
                    },
                    onPingOptionClicked = { onSendMessageBundle(Ping(conversationId)) },
                    onImagesPicked = onImagesPicked,
                    onAttachmentPicked = { onSendMessageBundle(ComposableMessageBundle.UriPickedBundle(conversationId, it)) },
                    onAudioRecorded = { onSendMessageBundle(ComposableMessageBundle.AudioMessageBundle(conversationId, it)) },
                    onLocationPicked = {
                        onSendMessageBundle(
                            ComposableMessageBundle.LocationBundle(
                                conversationId,
                                it.getFormattedAddress(),
                                it.location
                            )
                        )
                    },
                    onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                    onClearMentionSearchResult = onClearMentionSearchResult,
                    onCaptureVideoPermissionPermanentlyDenied = onCaptureVideoPermissionPermanentlyDenied,
                    tempWritableVideoUri = tempWritableVideoUri,
                    tempWritableImageUri = tempWritableImageUri,
                )
            }
        }
    }
}

@Composable
private fun warningTextWithStyledArgs(@StringRes stringResId: Int, vararg formatArgs: String) =
    LocalContext.current.resources.stringWithStyledArgs(
        stringResId = stringResId,
        normalStyle = MaterialTheme.wireTypography.body01,
        argsStyle = MaterialTheme.wireTypography.body02,
        normalColor = colorsScheme().secondaryText,
        argsColor = colorsScheme().onBackground,
        formatArgs = formatArgs
    )

@Composable
private fun DisabledInteractionMessageComposer(
    conversationId: ConversationId,
    warningText: AnnotatedString?,
    learnMoreLink: String? = null,
    messageListContent: @Composable () -> Unit
) {
    Surface(color = colorsScheme().messageComposerBackgroundColor) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val fillRemainingSpaceBetweenMessageListContentAndMessageComposer = Modifier
                .fillMaxWidth()
                .weight(1f)

            Box(
                Modifier
                    .background(color = colorsScheme().backgroundVariant)
                    .then(fillRemainingSpaceBetweenMessageListContentAndMessageComposer)
            ) {
                messageListContent()
            }
            if (warningText != null) {
                HorizontalDivider(color = MaterialTheme.wireColorScheme.outline)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = colorsScheme().backgroundVariant)
                        .padding(dimensions().spacing16x)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_info),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "",
                        modifier = Modifier
                            .padding(start = dimensions().spacing8x)
                            .alignBy { it.measuredHeight / 2 },
                    )
                    val lineHeight = MaterialTheme.wireTypography.body01.lineHeight.value
                    var centerOfFirstLine by remember { mutableStateOf(lineHeight / 2f) }
                    val textModifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(start = dimensions().spacing16x)
                        .alignBy { centerOfFirstLine.roundToInt() }
                    val onTextLayout: (TextLayoutResult) -> Unit = {
                        centerOfFirstLine = if (it.lineCount == 0) 0f else ((it.getLineTop(0) + it.getLineBottom(0)) / 2)
                    }
                    if (learnMoreLink.isNullOrEmpty()) {
                        Text(
                            text = warningText,
                            modifier = textModifier,
                            onTextLayout = onTextLayout
                        )
                    } else {
                        TextWithLearnMore(
                            textAnnotatedString = warningText,
                            learnMoreLink = learnMoreLink,
                            modifier = textModifier,
                            onTextLayout = onTextLayout
                        )
                    }
                }
            }
            SecurityClassificationBannerForConversation(conversationId = conversationId)
        }
    }
}

@Composable
private fun BaseComposerPreview(
    interactionAvailability: InteractionAvailability = InteractionAvailability.ENABLED,
) = WireTheme {
    val messageComposerViewState = remember {
        mutableStateOf(
            MessageComposerViewState(
                interactionAvailability = interactionAvailability
            )
        )
    }
    val messageTextState = rememberTextFieldState()
    val messageComposition = remember { mutableStateOf(MessageComposition(ConversationId("value", "domain"))) }
    val selfDeletionTimer = remember { mutableStateOf(SelfDeletionTimer.Enabled(Duration.ZERO)) }

    MessageComposer(
        conversationId = ConversationId("value", "domain"),
        messageComposerStateHolder = MessageComposerStateHolder(
            messageComposerViewState = messageComposerViewState,
            messageCompositionInputStateHolder = MessageCompositionInputStateHolder(
                messageTextState = messageTextState,
                selfDeletionTimer = selfDeletionTimer
            ),
            messageCompositionHolder = MessageCompositionHolder(
                messageComposition = messageComposition,
                messageTextState = messageTextState,
                onSaveDraft = {},
                onSearchMentionQueryChanged = {},
                onClearMentionSearchResult = {},
                onTypingEvent = {}
            ),
            additionalOptionStateHolder = AdditionalOptionStateHolder(),
            modalBottomSheetState = WireModalSheetState()
        ),
        messageListContent = { },
        onChangeSelfDeletionClicked = { },
        onClearMentionSearchResult = { },
        onCaptureVideoPermissionPermanentlyDenied = { },
        onSendMessageBundle = { },
        tempWritableVideoUri = null,
        tempWritableImageUri = null,
        onImagesPicked = {}
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewMessageComposerDeletedUser() = WireTheme {
    BaseComposerPreview(interactionAvailability = InteractionAvailability.DELETED_USER)
}

@PreviewMultipleThemes
@Composable
private fun PreviewMessageComposerBlockedUser() = WireTheme {
    BaseComposerPreview(interactionAvailability = InteractionAvailability.BLOCKED_USER)
}

@PreviewMultipleThemes
@Composable
private fun PreviewMessageComposerUnsupportedProtocol() = WireTheme {
    BaseComposerPreview(interactionAvailability = InteractionAvailability.UNSUPPORTED_PROTOCOL)
}

@PreviewMultipleThemes
@Composable
private fun PreviewMessageComposerLegalHold() = WireTheme {
    BaseComposerPreview(interactionAvailability = InteractionAvailability.LEGAL_HOLD)
}

@PreviewMultipleThemes
@Composable
private fun PreviewMessageComposerEnabled() = WireTheme {
    BaseComposerPreview(interactionAvailability = InteractionAvailability.ENABLED)
}
