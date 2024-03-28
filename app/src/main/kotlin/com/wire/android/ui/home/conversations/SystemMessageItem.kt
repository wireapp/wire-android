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
@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home.conversations

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.mock.mockMessageWithKnock
import com.wire.android.ui.home.conversations.mock.mockUsersUITexts
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent.SystemMessage
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.LocalizedStringResource
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.markdownBold
import com.wire.android.util.ui.markdownText
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import kotlin.math.roundToInt

@Suppress("ComplexMethod")
@Composable
fun SystemMessageItem(
    message: UIMessage.System,
    initiallyExpanded: Boolean = false,
    isInteractionAvailable: Boolean = true,
    onFailedMessageRetryClicked: (String, ConversationId) -> Unit = { _, _ -> },
    onFailedMessageCancelClicked: (String) -> Unit = {},
    onSelfDeletingMessageRead: (UIMessage) -> Unit = {}
) {
    val selfDeletionTimerState = rememberSelfDeletionTimer(message.header.messageStatus.expirationStatus)
    if (
        selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable &&
        !message.isPending &&
        !message.sendingFailed
    ) {
        selfDeletionTimerState.startDeletionTimer(
            message = message,
            assetTransferStatus = null,
            onStartMessageSelfDeletion = onSelfDeletingMessageRead
        )
    }
    val fullAvatarOuterPadding = dimensions().avatarClickablePadding + dimensions().avatarStatusBorderSize
    Row(
        Modifier
            .customizeMessageBackground(message.sendingFailed, message.decryptionFailed)
            .padding(
                end = dimensions().spacing16x,
                top = fullAvatarOuterPadding,
                bottom = dimensions().messageItemBottomPadding
            )
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(
                    dimensions().avatarDefaultSize
                            + (dimensions().avatarStatusBorderSize * 2)
                            + (dimensions().avatarClickablePadding * 2)
                )
                .padding(end = fullAvatarOuterPadding)
                .alignBy { it.measuredHeight / 2 },
            contentAlignment = Alignment.TopEnd
        ) {
            if (message.messageContent.iconResId != null) {
                Image(
                    painter = painterResource(id = message.messageContent.iconResId),
                    contentDescription = null,
                    colorFilter = getColorFilter(message.messageContent),
                    modifier = Modifier.size(
                        if (message.messageContent.isSmallIcon) dimensions().systemMessageIconSize
                        else dimensions().systemMessageIconLargeSize
                    ),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.width(dimensions().messageItemHorizontalPadding - fullAvatarOuterPadding))
        val lineHeight = MaterialTheme.wireTypography.body02.lineHeight.value
        var centerOfFirstLine by remember { mutableStateOf(lineHeight / 2f) }
        Column(
            Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .alignBy { centerOfFirstLine.roundToInt() }
        ) {
            val context = LocalContext.current
            var expanded: Boolean by remember { mutableStateOf(initiallyExpanded) }
            val annotatedString = message.messageContent.annotatedString(
                res = context.resources,
                expanded = expanded,
                normalStyle = MaterialTheme.wireTypography.body01,
                boldStyle = MaterialTheme.wireTypography.body02,
                normalColor = MaterialTheme.wireColorScheme.secondaryText,
                boldColor = MaterialTheme.wireColorScheme.onBackground,
                errorColor = MaterialTheme.wireColorScheme.error,
                isErrorString = message.addingFailed,
            )
            val learnMoreAnnotatedString = message.messageContent.learnMoreResId?.let {
                val learnMoreLink = stringResource(id = message.messageContent.learnMoreResId)
                val learnMoreText = stringResource(id = R.string.label_learn_more)
                buildAnnotatedString {
                    append(learnMoreText)
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = 0,
                        end = learnMoreText.length
                    )
                    addStringAnnotation(tag = TAG_LEARN_MORE, annotation = learnMoreLink, start = 0, end = learnMoreText.length)
                }
            }
            val fullAnnotatedString = when {
                learnMoreAnnotatedString == null -> annotatedString
                message.messageContent.expandable && expanded -> annotatedString + AnnotatedString("\n") + learnMoreAnnotatedString
                message.messageContent.expandable && !expanded -> annotatedString
                else -> annotatedString + AnnotatedString(" ") + learnMoreAnnotatedString
            }

            ClickableText(
                modifier = Modifier.defaultMinSize(minHeight = dimensions().spacing20x),
                text = fullAnnotatedString,
                onClick = { offset ->
                    fullAnnotatedString.getStringAnnotations(TAG_LEARN_MORE, offset, offset)
                        .firstOrNull()?.let { result -> CustomTabsHelper.launchUrl(context, result.item) }
                },
                style = MaterialTheme.wireTypography.body02,
                onTextLayout = {
                    centerOfFirstLine = if (it.lineCount == 0) 0f else ((it.getLineTop(0) + it.getLineBottom(0)) / 2)
                }
            )

            if (message.messageContent.expandable) {
                VerticalSpace.x8()
                WireSecondaryButton(
                    onClick = { expanded = !expanded },
                    text = stringResource(if (expanded) R.string.label_show_less else R.string.label_show_all),
                    fillMaxWidth = false,
                    minSize = dimensions().buttonSmallMinSize,
                    minClickableSize = dimensions().buttonSmallMinSize,
                    shape = RoundedCornerShape(size = dimensions().corner12x),
                    contentPadding = PaddingValues(horizontal = dimensions().spacing12x, vertical = dimensions().spacing8x),
                )
            }
            if (message.sendingFailed) {
                MessageSendFailureWarning(
                    messageStatus = message.header.messageStatus.flowStatus as MessageFlowStatus.Failure.Send,
                    isInteractionAvailable = isInteractionAvailable,
                    onRetryClick = remember { { onFailedMessageRetryClicked(message.header.messageId, message.conversationId) } },
                    onCancelClick = remember { { onFailedMessageCancelClicked(message.header.messageId) } }
                )
            }
        }
    }
    if (message.messageContent is SystemMessage.ConversationMessageCreated) {
        Row(
            Modifier
                .background(colorsScheme().background)
                .height(dimensions().spacing24x)
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier
                    .padding(start = dimensions().spacing56x)
                    .align(Alignment.CenterVertically),
                style = MaterialTheme.wireTypography.title03,
                text = message.messageContent.date
            )
        }
    }
}

@Suppress("ComplexMethod")
@Composable
private fun getColorFilter(message: SystemMessage): ColorFilter? {
    return when (message) {
        is SystemMessage.MissedCall.OtherCalled -> null
        is SystemMessage.MissedCall.YouCalled -> null
        is SystemMessage.ConversationDegraded -> null
        is SystemMessage.ConversationVerified -> null
        is SystemMessage.Knock -> ColorFilter.tint(colorsScheme().primary)
        is SystemMessage.LegalHold,
        is SystemMessage.MemberFailedToAdd -> ColorFilter.tint(colorsScheme().error)

        is SystemMessage.MemberAdded,
        is SystemMessage.MemberJoined,
        is SystemMessage.MemberLeft,
        is SystemMessage.MemberRemoved,
        is SystemMessage.CryptoSessionReset,
        is SystemMessage.RenamedConversation,
        is SystemMessage.TeamMemberRemoved_Legacy,
        is SystemMessage.ConversationReceiptModeChanged,
        is SystemMessage.HistoryLost,
        is SystemMessage.HistoryLostProtocolChanged,
        is SystemMessage.NewConversationReceiptMode,
        is SystemMessage.ConversationProtocolChanged,
        is SystemMessage.ConversationProtocolChangedWithCallOngoing,
        is SystemMessage.ConversationMessageTimerActivated,
        is SystemMessage.ConversationMessageCreated,
        is SystemMessage.ConversationStartedWithMembers,
        is SystemMessage.ConversationMessageTimerDeactivated,
        is SystemMessage.FederationMemberRemoved,
        is SystemMessage.FederationStopped,
        is SystemMessage.ConversationMessageCreatedUnverifiedWarning,
        is SystemMessage.TeamMemberRemoved,
        is SystemMessage.MLSWrongEpochWarning -> ColorFilter.tint(colorsScheme().onBackground)
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageAdded7Users() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberAdded(
                    "Barbara Cotolina".toUIText(),
                    listOf(
                        "Albert Lewis".toUIText(),
                        "Bert Strunk".toUIText(),
                        "Claudia Schiffer".toUIText(),
                        "Dorothee Friedrich".toUIText(),
                        "Erich Weinert".toUIText(),
                        "Frieda Kahlo".toUIText(),
                        "Gudrun Gut".toUIText()
                    )
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageAdded4Users() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberAdded(
                    "Barbara Cotolina".toUIText(),
                    listOf(
                        "Albert Lewis".toUIText(),
                        "Bert Strunk".toUIText(),
                        "Claudia Schiffer".toUIText(),
                        "Dorothee Friedrich".toUIText()
                    )
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageRemoved4Users() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberRemoved(
                    "Barbara Cotolina".toUIText(),
                    listOf(
                        "Albert Lewis".toUIText(),
                        "Bert Strunk".toUIText(),
                        "Claudia Schiffer".toUIText(),
                        "Dorothee Friedrich".toUIText()
                    )
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLeft() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberLeft(UIText.DynamicString("Barbara Cotolina"))
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageMissedCall() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MissedCall.OtherCalled(UIText.DynamicString("Barbara Cotolina"))
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageKnock() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.Knock(UIText.DynamicString("Barbara Cotolina"), true)
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFailedToAddFederationSingle() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberFailedToAdd(
                    listOf(UIText.DynamicString("Barbara Cotolina")),
                    SystemMessage.MemberFailedToAdd.Type.Federation
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFailedToAddFederationMultiple() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberFailedToAdd(
                    listOf(
                        UIText.DynamicString("Barbara Cotolina"),
                        UIText.DynamicString("Albert Lewis")
                    ),
                    SystemMessage.MemberFailedToAdd.Type.Federation
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFailedToAddFederationMultipleExpanded() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberFailedToAdd(
                    listOf(
                        UIText.DynamicString("Barbara Cotolina"),
                        UIText.DynamicString("Albert Lewis")
                    ),
                    SystemMessage.MemberFailedToAdd.Type.Federation
                )
            ),
            initiallyExpanded = true,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFailedToAddLegalHoldSingle() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberFailedToAdd(
                    listOf(UIText.DynamicString("Barbara Cotolina")),
                    SystemMessage.MemberFailedToAdd.Type.LegalHold
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFailedToAddLegalHoldMultiple() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberFailedToAdd(
                    listOf(
                        UIText.DynamicString("Barbara Cotolina"),
                        UIText.DynamicString("Albert Lewis")
                    ),
                    SystemMessage.MemberFailedToAdd.Type.LegalHold
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFailedToAddLegalHoldMultipleExpanded() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberFailedToAdd(
                    listOf(
                        UIText.DynamicString("Barbara Cotolina"),
                        UIText.DynamicString("Albert Lewis")
                    ),
                    SystemMessage.MemberFailedToAdd.Type.LegalHold
                )
            ),
            initiallyExpanded = true,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFederationMemberRemoved() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.FederationMemberRemoved(
                    listOf(
                        "Barbara Cotolina".toUIText(),
                        "Albert Lewis".toUIText()
                    )
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFederationMemberRemoved7Users() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.FederationMemberRemoved(
                    listOf(
                        "Albert Lewis".toUIText(),
                        "Bert Strunk".toUIText(),
                        "Claudia Schiffer".toUIText(),
                        "Dorothee Friedrich".toUIText(),
                        "Erich Weinert".toUIText(),
                        "Frieda Kahlo".toUIText(),
                        "Gudrun Gut".toUIText()
                    )
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFederationStopped() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.FederationStopped(
                    listOf(
                        "bella.wire.link",
                        "foma.wire.link"
                    )
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFederationStoppedSelf() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.FederationStopped(
                    listOf("foma.wire.link")
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLegalHoldEnabledSelf() {
    WireTheme {
        SystemMessageItem(message = mockMessageWithKnock.copy(messageContent = SystemMessage.LegalHold.Enabled.Self))
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLegalHoldDisabledSelf() {
    WireTheme {
        SystemMessageItem(message = mockMessageWithKnock.copy(messageContent = SystemMessage.LegalHold.Disabled.Self))
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLegalHoldEnabledOthers() {
    WireTheme {
        SystemMessageItem(message = mockMessageWithKnock.copy(messageContent = SystemMessage.LegalHold.Enabled.Others(mockUsersUITexts)))
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLegalHoldDisabledOthers() {
    WireTheme {
        SystemMessageItem(message = mockMessageWithKnock.copy(messageContent = SystemMessage.LegalHold.Disabled.Others(mockUsersUITexts)))
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLegalHoldDisabledConversation() {
    WireTheme {
        SystemMessageItem(message = mockMessageWithKnock.copy(messageContent = SystemMessage.LegalHold.Disabled.Conversation))
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLegalHoldEnabledConversation() {
    WireTheme {
        SystemMessageItem(message = mockMessageWithKnock.copy(messageContent = SystemMessage.LegalHold.Enabled.Conversation))
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageConversationVerifiedProteus() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.ConversationVerified(Conversation.Protocol.PROTEUS)
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageConversationVerifiedMLS() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.ConversationVerified(Conversation.Protocol.MLS)
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageConversationDegradedProteus() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.ConversationDegraded(Conversation.Protocol.PROTEUS)
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageConversationDegradedMLS() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.ConversationDegraded(Conversation.Protocol.MLS)
            )
        )
    }
}

private val SystemMessage.expandable
    get() = when (this) {
        is SystemMessage.MemberAdded -> this.memberNames.size > EXPANDABLE_THRESHOLD
        is SystemMessage.MemberRemoved -> this.memberNames.size > EXPANDABLE_THRESHOLD
        is SystemMessage.FederationMemberRemoved -> this.memberNames.size > EXPANDABLE_THRESHOLD
        is SystemMessage.MemberJoined -> false
        is SystemMessage.MemberLeft -> false
        is SystemMessage.MissedCall -> false
        is SystemMessage.RenamedConversation -> false
        is SystemMessage.TeamMemberRemoved_Legacy -> false
        is SystemMessage.CryptoSessionReset -> false
        is SystemMessage.NewConversationReceiptMode -> false
        is SystemMessage.ConversationReceiptModeChanged -> false
        is SystemMessage.Knock -> false
        is SystemMessage.HistoryLost -> false
        is SystemMessage.HistoryLostProtocolChanged -> false
        is SystemMessage.ConversationProtocolChanged -> false
        is SystemMessage.ConversationProtocolChangedWithCallOngoing -> false
        is SystemMessage.ConversationMessageTimerActivated -> false
        is SystemMessage.ConversationMessageTimerDeactivated -> false
        is SystemMessage.ConversationMessageCreated -> false
        is SystemMessage.MLSWrongEpochWarning -> false
        is SystemMessage.ConversationStartedWithMembers -> this.memberNames.size > EXPANDABLE_THRESHOLD
        is SystemMessage.MemberFailedToAdd -> this.usersCount > SINGLE_EXPANDABLE_THRESHOLD
        is SystemMessage.ConversationDegraded -> false
        is SystemMessage.ConversationVerified -> false
        is SystemMessage.FederationStopped -> false
        is SystemMessage.ConversationMessageCreatedUnverifiedWarning -> false
        is SystemMessage.LegalHold -> false
        is SystemMessage.TeamMemberRemoved -> this.memberNames.size > EXPANDABLE_THRESHOLD
    }

private fun List<String>.toUserNamesListMarkdownString(res: Resources): String = when {
    this.isEmpty() -> ""
    this.size == 1 -> this[0].markdownBold()
    else -> res.getString(
        R.string.label_system_message_and,
        this.dropLast(1).joinToString(", ") { it.markdownBold() },
        this.last().markdownBold()
    )
}

private fun List<UIText>.limitUserNamesList(
    res: Resources,
    expanded: Boolean,
    collapsedSize: Int = EXPANDABLE_THRESHOLD,
    @PluralsRes quantityString: Int = R.plurals.label_system_message_x_more
): List<String> =
    if (expanded || this.size <= collapsedSize) {
        this.map { it.asString(res) }
    } else {
        val moreCount = this.size - (collapsedSize - 1) // the last visible place is taken by "and X more"
        this.take(collapsedSize - 1)
            .map { it.asString(res) }
            .plus(res.getQuantityString(quantityString, moreCount, moreCount))
    }

@Suppress("LongParameterList", "SpreadOperator", "ComplexMethod", "LongMethod")
fun SystemMessage.annotatedString(
    res: Resources,
    expanded: Boolean,
    normalStyle: TextStyle,
    boldStyle: TextStyle,
    normalColor: Color,
    boldColor: Color,
    errorColor: Color,
    isErrorString: Boolean = false
): AnnotatedString {
    val markdownArgs = when (this) {
        is SystemMessage.MemberAdded ->
            arrayOf(
                author.asString(res).markdownBold(),
                memberNames.limitUserNamesList(res, expanded).toUserNamesListMarkdownString(res)
            )

        is SystemMessage.MemberRemoved ->
            arrayOf(
                author.asString(res).markdownBold(),
                memberNames.limitUserNamesList(res, expanded).toUserNamesListMarkdownString(res)
            )

        is SystemMessage.TeamMemberRemoved -> arrayOf(
            author.asString(res).markdownBold(),
            memberNames.limitUserNamesList(res, expanded).toUserNamesListMarkdownString(res)
        )

        is SystemMessage.FederationMemberRemoved ->
            arrayOf(
                memberNames.limitUserNamesList(res, expanded).toUserNamesListMarkdownString(res)
            )

        is SystemMessage.MemberJoined -> arrayOf(author.asString(res).markdownBold())
        is SystemMessage.MemberLeft -> arrayOf(author.asString(res).markdownBold())
        is SystemMessage.MissedCall -> arrayOf(author.asString(res).markdownBold())
        is SystemMessage.RenamedConversation -> arrayOf(author.asString(res).markdownBold(), content.conversationName.markdownBold())
        is SystemMessage.CryptoSessionReset -> arrayOf(author.asString(res).markdownBold())
        is SystemMessage.NewConversationReceiptMode -> arrayOf(receiptMode.asString(res).markdownBold())
        is SystemMessage.ConversationReceiptModeChanged -> arrayOf(
            author.asString(res).markdownBold(),
            receiptMode.asString(res).markdownBold()
        )

        is SystemMessage.TeamMemberRemoved_Legacy -> arrayOf(content.userName)
        is SystemMessage.Knock -> arrayOf(author.asString(res).markdownBold())
        is SystemMessage.HistoryLost -> arrayOf()
        is SystemMessage.MLSWrongEpochWarning -> arrayOf()
        is SystemMessage.ConversationDegraded -> arrayOf()
        is SystemMessage.ConversationVerified -> arrayOf()
        is SystemMessage.HistoryLostProtocolChanged -> arrayOf()
        is SystemMessage.ConversationProtocolChanged -> arrayOf()
        is SystemMessage.ConversationProtocolChangedWithCallOngoing -> arrayOf()
        is SystemMessage.ConversationMessageTimerActivated -> arrayOf(
            author.asString(res).markdownBold(),
            selfDeletionDuration.longLabel.asString(res).markdownBold()
        )

        is SystemMessage.ConversationMessageTimerDeactivated -> arrayOf(author.asString(res).markdownBold())
        is SystemMessage.ConversationMessageCreated -> arrayOf(author.asString(res).markdownBold())
        is SystemMessage.ConversationStartedWithMembers ->
            arrayOf(memberNames.limitUserNamesList(res, expanded).toUserNamesListMarkdownString(res))

        is SystemMessage.MemberFailedToAdd ->
            return this.toFailedToAddMarkdownText(
                res, normalStyle, boldStyle, normalColor, boldColor, errorColor, isErrorString,
                if (usersCount > SINGLE_EXPANDABLE_THRESHOLD) expanded else true
            )

        is SystemMessage.FederationStopped -> domainList.toTypedArray()
        is SystemMessage.ConversationMessageCreatedUnverifiedWarning -> arrayOf()
        is SystemMessage.LegalHold -> memberNames?.let { memberNames ->
            arrayOf(memberNames.limitUserNamesList(res, true).toUserNamesListMarkdownString(res))
        } ?: arrayOf()
    }
    val markdownString = when (stringResId) {
        is LocalizedStringResource.PluralResource -> res.getQuantityString(
            (stringResId as LocalizedStringResource.PluralResource).id,
            (stringResId as LocalizedStringResource.PluralResource).quantity,
            *markdownArgs
        )

        is LocalizedStringResource.StringResource -> res.getString(
            (stringResId as LocalizedStringResource.StringResource).id,
            *markdownArgs
        )
    }

    return markdownText(markdownString, normalStyle, boldStyle, normalColor, boldColor, errorColor, isErrorString)
}

@Suppress("LongParameterList", "SpreadOperator", "ComplexMethod")
private fun SystemMessage.MemberFailedToAdd.toFailedToAddMarkdownText(
    res: Resources,
    normalStyle: TextStyle,
    boldStyle: TextStyle,
    normalColor: Color,
    boldColor: Color,
    errorColor: Color,
    isErrorString: Boolean,
    expanded: Boolean
): AnnotatedString {
    val failedToAddAnnotatedText = AnnotatedString.Builder()
    val isMultipleUsersFailure = usersCount > SINGLE_EXPANDABLE_THRESHOLD
    if (isMultipleUsersFailure) {
        failedToAddAnnotatedText.append(
            markdownText(
                res.getString(R.string.label_system_message_conversation_failed_add_members_summary, usersCount.toString().markdownBold()),
                normalStyle,
                boldStyle,
                normalColor,
                boldColor,
                errorColor,
                isErrorString,
            )
        )
    }

    if (expanded) {
        if (isMultipleUsersFailure) failedToAddAnnotatedText.append("\n\n")
        failedToAddAnnotatedText.append(
            markdownText(
                when (stringResId) {
                    is LocalizedStringResource.PluralResource -> res.getQuantityString(
                        stringResId.id,
                        stringResId.quantity,
                        stringResId.formatArgs
                    )

                    is LocalizedStringResource.StringResource -> res.getString(
                        stringResId.id,
                        memberNames.limitUserNamesList(res, true).toUserNamesListMarkdownString(res)
                    )
                },
                normalStyle,
                boldStyle,
                normalColor,
                boldColor,
                errorColor,
                isErrorString,
            )
        )
    }
    return failedToAddAnnotatedText.toAnnotatedString()
}

private const val EXPANDABLE_THRESHOLD = 4
private const val SINGLE_EXPANDABLE_THRESHOLD = 1
private const val TAG_LEARN_MORE = "tag_learn_more"
