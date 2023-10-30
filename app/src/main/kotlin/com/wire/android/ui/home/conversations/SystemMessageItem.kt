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
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent.SystemMessage
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.annotatedText
import com.wire.android.util.ui.toUIText

@Suppress("ComplexMethod")
@Composable
fun SystemMessageItem(
    message: UIMessage.System,
    onFailedMessageRetryClicked: (String) -> Unit = {},
    onFailedMessageCancelClicked: (String) -> Unit = {},
    onSelfDeletingMessageRead: (UIMessage) -> Unit = {}
) {
    val selfDeletionTimerState = rememberSelfDeletionTimer(message.header.messageStatus.expirationStatus)
    if (
        selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable &&
        !message.isPending &&
        !message.sendingFailed
    ) {
        startDeletionTimer(
            message = message,
            expirableTimer = selfDeletionTimerState,
            onStartMessageSelfDeletion = onSelfDeletingMessageRead
        )
    }
    val fullAvatarOuterPadding = dimensions().avatarClickablePadding + dimensions().avatarStatusBorderSize
    Row(
        Modifier
            .customizeMessageBackground(message.sendingFailed, message.decryptionFailed)
            .padding(
                end = dimensions().spacing16x,
                start = dimensions().spacing8x,
                top = fullAvatarOuterPadding,
                bottom = dimensions().messageItemBottomPadding - fullAvatarOuterPadding
            )
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(dimensions().avatarDefaultSize),
            contentAlignment = Alignment.TopEnd
        ) {
            if (message.messageContent.iconResId != null) {
                Box(
                    modifier = Modifier.size(
                        width = dimensions().systemMessageIconLargeSize,
                        height = dimensions().spacing20x
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    val size =
                        if (message.messageContent.isSmallIcon) {
                            dimensions().systemMessageIconSize
                        } else {
                            dimensions().systemMessageIconLargeSize
                        }
                    Image(
                        painter = painterResource(id = message.messageContent.iconResId),
                        contentDescription = null,
                        colorFilter = getColorFilter(message.messageContent),
                        modifier = Modifier.size(size),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        Spacer(Modifier.padding(start = dimensions().spacing16x))
        Column(
            Modifier
                .defaultMinSize(minHeight = dimensions().spacing20x)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
        ) {
            val context = LocalContext.current
            var expanded: Boolean by remember { mutableStateOf(false) }
            Text(
                modifier = Modifier.defaultMinSize(minHeight = dimensions().spacing20x),
                style = MaterialTheme.wireTypography.body01,
                lineHeight = MaterialTheme.wireTypography.body02.lineHeight,
                text = message.messageContent.annotatedString(
                    res = context.resources,
                    expanded = expanded,
                    normalStyle = MaterialTheme.wireTypography.body01,
                    boldStyle = MaterialTheme.wireTypography.body02,
                    normalColor = MaterialTheme.wireColorScheme.secondaryText,
                    boldColor = MaterialTheme.wireColorScheme.onBackground,
                    errorColor = MaterialTheme.wireColorScheme.error,
                    isErrorString = message.addingFailed
                )
            )
            if ((message.addingFailed && expanded) || message.singleUserAddFailed) {
                OfflineBackendsLearnMoreLink()
            }
            if (message.messageContent is SystemMessage.Knock) {
                VerticalSpace.x8()
            }
            if (message.messageContent.expandable) {
                WireSecondaryButton(
                    onClick = { expanded = !expanded },
                    text = stringResource(if (expanded) R.string.label_show_less else R.string.label_show_all),
                    fillMaxWidth = false,
                    minSize = dimensions().buttonSmallMinSize,
                    minClickableSize = dimensions().buttonMinClickableSize,
                    shape = RoundedCornerShape(size = dimensions().corner12x),
                    contentPadding = PaddingValues(horizontal = dimensions().spacing12x, vertical = dimensions().spacing8x),
                )
            }
            if (message.sendingFailed) {
                MessageSendFailureWarning(
                    messageStatus = message.header.messageStatus.flowStatus as MessageFlowStatus.Failure.Send,
                    onRetryClick = remember { { onFailedMessageRetryClicked(message.header.messageId) } },
                    onCancelClick = remember { { onFailedMessageCancelClicked(message.header.messageId) } }
                )
            }
            if (message.messageContent.learnMoreResId != null) {
                val learnMoreLink = stringResource(id = message.messageContent.learnMoreResId)
                val learnMoreText = stringResource(id = R.string.label_learn_more)
                val annotatedString = buildAnnotatedString {
                    append(learnMoreText)
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = 0,
                        end = learnMoreText.length
                    )
                }
                ClickableText(
                    text = annotatedString,
                    onClick = {
                        CustomTabsHelper.launchUrl(
                            context,
                            learnMoreLink
                        )
                    },
                    style = MaterialTheme.wireTypography.body01,
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
        is SystemMessage.MemberFailedToAdd -> ColorFilter.tint(colorsScheme().error)
        is SystemMessage.MemberAdded,
        is SystemMessage.MemberJoined,
        is SystemMessage.MemberLeft,
        is SystemMessage.MemberRemoved,
        is SystemMessage.CryptoSessionReset,
        is SystemMessage.RenamedConversation,
        is SystemMessage.TeamMemberRemoved,
        is SystemMessage.ConversationReceiptModeChanged,
        is SystemMessage.HistoryLost,
        is SystemMessage.HistoryLostProtocolChanged,
        is SystemMessage.NewConversationReceiptMode,
        is SystemMessage.ConversationProtocolChanged,
        is SystemMessage.ConversationMessageTimerActivated,
        is SystemMessage.ConversationMessageCreated,
        is SystemMessage.ConversationStartedWithMembers,
        is SystemMessage.ConversationMessageTimerDeactivated,
        is SystemMessage.FederationMemberRemoved,
        is SystemMessage.FederationStopped,
        is SystemMessage.ConversationMessageCreatedUnverifiedWarning,
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
fun PreviewSystemMessageFailedToAddSingle() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberFailedToAdd(
                    listOf(UIText.DynamicString("Barbara Cotolina"))
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFailedToAddMultiple() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = SystemMessage.MemberFailedToAdd(
                    listOf(
                        UIText.DynamicString("Barbara Cotolina"),
                        UIText.DynamicString("Albert Lewis")
                    )
                )
            )
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

private val SystemMessage.expandable
    get() = when (this) {
        is SystemMessage.MemberAdded -> this.memberNames.size > EXPANDABLE_THRESHOLD
        is SystemMessage.MemberRemoved -> this.memberNames.size > EXPANDABLE_THRESHOLD
        is SystemMessage.FederationMemberRemoved -> this.memberNames.size > EXPANDABLE_THRESHOLD
        is SystemMessage.MemberJoined -> false
        is SystemMessage.MemberLeft -> false
        is SystemMessage.MissedCall -> false
        is SystemMessage.RenamedConversation -> false
        is SystemMessage.TeamMemberRemoved -> false
        is SystemMessage.CryptoSessionReset -> false
        is SystemMessage.NewConversationReceiptMode -> false
        is SystemMessage.ConversationReceiptModeChanged -> false
        is SystemMessage.Knock -> false
        is SystemMessage.HistoryLost -> false
        is SystemMessage.HistoryLostProtocolChanged -> false
        is SystemMessage.ConversationProtocolChanged -> false
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
    }

private fun List<String>.toUserNamesListString(res: Resources): String = when {
    this.isEmpty() -> ""
    this.size == 1 -> this[0]
    else -> res.getString(R.string.label_system_message_and, this.dropLast(1).joinToString(", "), this.last())
}

private fun List<UIText>.limitUserNamesList(
    res: Resources,
    threshold: Int,
    @PluralsRes quantityString: Int = R.plurals.label_system_message_x_more
): List<String> =
    if (this.size <= threshold) {
        this.map { it.asString(res) }
    } else {
        val moreCount = this.size - (threshold - 1) // the last visible place is taken by "and X more"
        this.take(threshold - 1)
            .map { it.asString(res) }
            .plus(res.getQuantityString(quantityString, moreCount, moreCount))
    }

@Suppress("LongParameterList", "SpreadOperator", "ComplexMethod")
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
    val args = when (this) {
        is SystemMessage.MemberAdded ->
            arrayOf(
                author.asString(res),
                memberNames.limitUserNamesList(res, if (expanded) memberNames.size else EXPANDABLE_THRESHOLD).toUserNamesListString(res)
            )

        is SystemMessage.MemberRemoved ->
            arrayOf(
                author.asString(res),
                memberNames.limitUserNamesList(res, if (expanded) memberNames.size else EXPANDABLE_THRESHOLD).toUserNamesListString(res)
            )

        is SystemMessage.FederationMemberRemoved ->
            arrayOf(
                memberNames.limitUserNamesList(res, if (expanded) memberNames.size else EXPANDABLE_THRESHOLD).toUserNamesListString(res)
            )

        is SystemMessage.MemberJoined -> arrayOf(author.asString(res))
        is SystemMessage.MemberLeft -> arrayOf(author.asString(res))
        is SystemMessage.MissedCall -> arrayOf(author.asString(res))
        is SystemMessage.RenamedConversation -> arrayOf(author.asString(res), additionalContent)
        is SystemMessage.TeamMemberRemoved -> arrayOf(content.userName)
        is SystemMessage.CryptoSessionReset -> arrayOf(author.asString(res))
        is SystemMessage.NewConversationReceiptMode -> arrayOf(receiptMode.asString(res))
        is SystemMessage.ConversationReceiptModeChanged -> arrayOf(author.asString(res), receiptMode.asString(res))
        is SystemMessage.Knock -> arrayOf(author.asString(res))
        is SystemMessage.HistoryLost -> arrayOf()
        is SystemMessage.MLSWrongEpochWarning -> arrayOf()
        is SystemMessage.ConversationDegraded -> arrayOf()
        is SystemMessage.ConversationVerified -> arrayOf()
        is SystemMessage.HistoryLostProtocolChanged -> arrayOf()
        is SystemMessage.ConversationProtocolChanged -> arrayOf()
        is SystemMessage.ConversationMessageTimerActivated -> arrayOf(
            author.asString(res),
            selfDeletionDuration.longLabel.asString(res)
        )

        is SystemMessage.ConversationMessageTimerDeactivated -> arrayOf(author.asString(res))
        is SystemMessage.ConversationMessageCreated -> arrayOf(author.asString(res))
        is SystemMessage.ConversationStartedWithMembers ->
            arrayOf(
                memberNames.limitUserNamesList(res, if (expanded) memberNames.size else EXPANDABLE_THRESHOLD)
                    .toUserNamesListString(res)
            )

        is SystemMessage.MemberFailedToAdd ->
            return this.toFailedToAddAnnotatedText(
                res, normalStyle, boldStyle, normalColor, boldColor, errorColor, isErrorString,
                if (usersCount > SINGLE_EXPANDABLE_THRESHOLD) expanded else true
            )

        is SystemMessage.FederationStopped -> domainList.toTypedArray()
        is SystemMessage.ConversationMessageCreatedUnverifiedWarning -> arrayOf()
    }

    return res.annotatedText(stringResId, normalStyle, boldStyle, normalColor, boldColor, errorColor, isErrorString, *args)
}

@Suppress("LongParameterList", "SpreadOperator", "ComplexMethod")
private fun SystemMessage.MemberFailedToAdd.toFailedToAddAnnotatedText(
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
            res.annotatedText(
                R.string.label_system_message_conversation_failed_add_members_summary,
                normalStyle,
                boldStyle,
                normalColor,
                boldColor,
                errorColor,
                isErrorString,
                this.usersCount.toString()
            )
        )
    }

    if (expanded) {
        if (isMultipleUsersFailure) failedToAddAnnotatedText.append("\n")
        failedToAddAnnotatedText.append(
            res.annotatedText(
                stringResId,
                normalStyle,
                boldStyle,
                normalColor,
                boldColor,
                errorColor,
                isErrorString,
                memberNames.limitUserNamesList(res, EXPANDABLE_THRESHOLD).toUserNamesListString(res)
            )
        )
    }
    return failedToAddAnnotatedText.toAnnotatedString()
}

private const val EXPANDABLE_THRESHOLD = 4
private const val SINGLE_EXPANDABLE_THRESHOLD = 1
