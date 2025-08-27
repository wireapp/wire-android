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

package com.wire.android.ui.home.conversations.messages.item

import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.common.TextWithLinkSuffix
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent.SystemMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent.SystemMessage.MemberFailedToAdd.Type
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.MarkdownTextStyle
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.markdownBold
import com.wire.android.util.ui.markdownText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId

@Suppress("ComplexMethod")
@Composable
fun SystemMessageItem(
    message: UIMessage.System,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    failureInteractionAvailable: Boolean = true,
    onFailedMessageRetryClicked: (String, ConversationId) -> Unit = { _, _ -> },
    onFailedMessageCancelClicked: (String) -> Unit = {},
) = with(message.messageContent.buildContent()) {
    val textStyle = MaterialTheme.wireTypography.body01
    val lineHeightDp: Dp = with(LocalDensity.current) { textStyle.lineHeight.toDp() }
    MessageItemTemplate(
        modifier = modifier
            .background(backgroundColor ?: Color.Transparent)
            .padding(vertical = additionalVerticalPaddings),
        fullAvatarOuterPadding = dimensions().avatarClickablePadding,
        leading = {
            SystemMessageItemLeading(
                modifier = Modifier
                    .padding(end = dimensions().spacing16x)
                    .defaultMinSize(minHeight = lineHeightDp),
                messageContent = this
            )
        },
        content = {
            Column(
                Modifier
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
            ) {
                val context = LocalContext.current
                var expanded: Boolean by remember { mutableStateOf(initiallyExpanded) }
                val learnMoreLink = learnMoreLinkResId?.let { stringResource(id = it) }

                TextWithLinkSuffix(
                    textStyle = MaterialTheme.wireTypography.body01,
                    text = annotatedStringBuilder(expanded),
                    linkText = when {
                        learnMoreLink != null && (!expandable || expanded) -> stringResource(id = R.string.label_learn_more)
                        else -> null
                    },
                    textColor = MaterialTheme.wireColorScheme.secondaryText,
                    onLinkClick = { learnMoreLink?.let { CustomTabsHelper.launchUrl(context, it) } },
                    modifier = Modifier.defaultMinSize(minHeight = lineHeightDp),
                )

                if (expandable) {
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
                        isInteractionAvailable = failureInteractionAvailable,
                        onRetryClick = remember { { onFailedMessageRetryClicked(message.header.messageId, message.conversationId) } },
                        onCancelClick = remember { { onFailedMessageCancelClicked(message.header.messageId) } }
                    )
                }
            }
        }
    )
}

@Suppress("LongParameterList", "SpreadOperator", "ComplexMethod", "LongMethod")
@Composable
private fun SystemMessage.buildContent() = when (this) {
    is SystemMessage.MemberAdded -> buildContent(
        iconResId = R.drawable.ic_add,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
        expandable = memberNames.size > EXPANDABLE_THRESHOLD
    ) { expanded ->
        stringResource(
            id = when {
                isSelfTriggered -> R.string.label_system_message_added_by_self
                else -> R.string.label_system_message_added_by_other
            },
            formatArgs = arrayOf(author.asString().markdownBold(), memberNames.limitList(expanded).toListMarkdownString())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.MemberRemoved -> buildContent(
        iconResId = R.drawable.ic_minus,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
        expandable = memberNames.size > EXPANDABLE_THRESHOLD
    ) { expanded ->
        stringResource(
            id = when {
                isSelfTriggered -> R.string.label_system_message_removed_by_self
                else -> R.string.label_system_message_removed_by_other
            },
            formatArgs = arrayOf(author.asString().markdownBold(), memberNames.limitList(expanded).toListMarkdownString())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.TeamMemberRemoved -> buildContent(
        iconResId = R.drawable.ic_minus,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
        expandable = memberNames.size > EXPANDABLE_THRESHOLD
    ) { expanded ->
        pluralStringResource(
            id = R.plurals.label_system_message_team_member_left,
            count = memberNames.size,
            formatArgs = arrayOf(author.asString().markdownBold(), memberNames.limitList(expanded).toListMarkdownString())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.FederationMemberRemoved -> buildContent(
        iconResId = R.drawable.ic_minus,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
        expandable = memberNames.size > EXPANDABLE_THRESHOLD
    ) { expanded ->
        pluralStringResource(
            id = R.plurals.label_system_message_federation_member_removed,
            count = memberNames.size,
            formatArgs = arrayOf(memberNames.limitList(expanded).toListMarkdownString())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.MemberJoined -> buildContent(
        iconResId = R.drawable.ic_add,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        stringResource(
            id = when {
                isSelfTriggered -> R.string.label_system_message_joined_the_conversation_by_self
                else -> R.string.label_system_message_joined_the_conversation_by_other
            },
            formatArgs = arrayOf(author.asString().markdownBold())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.MemberLeft -> buildContent(
        iconResId = R.drawable.ic_minus,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        stringResource(
            id = when {
                isSelfTriggered -> R.string.label_system_message_left_the_conversation_by_self
                else -> R.string.label_system_message_left_the_conversation_by_other
            },
            formatArgs = arrayOf(author.asString().markdownBold())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.MissedCall -> buildContent(
        iconResId = R.drawable.ic_call_end,
        iconTintColor = MaterialTheme.wireColorScheme.error,
        iconSize = MaterialTheme.wireDimensions.systemMessageIconLargeSize,
    ) {
        stringResource(
            id = when (this) {
                is SystemMessage.MissedCall.YouCalled -> R.string.label_system_message_you_called
                is SystemMessage.MissedCall.OtherCalled -> R.string.label_system_message_other_called
            },
            formatArgs = arrayOf(author.asString().markdownBold())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.RenamedConversation -> buildContent(
        iconResId = R.drawable.ic_edit,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        stringResource(
            id = R.string.label_system_message_renamed_the_conversation,
            formatArgs = arrayOf(author.asString().markdownBold(), conversationName.markdownBold())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.CryptoSessionReset -> buildContent(
        iconResId = R.drawable.ic_info,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        stringResource(
            id = R.string.label_system_message_session_reset,
            formatArgs = arrayOf(author.asString().markdownBold())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.NewConversationReceiptMode -> buildContent(
        iconResId = R.drawable.ic_view,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        stringResource(
            id = R.string.label_system_message_new_conversation_receipt_mode,
            formatArgs = arrayOf(receiptMode.asString().markdownBold())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.ConversationReceiptModeChanged -> buildContent(
        iconResId = R.drawable.ic_view,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        stringResource(
            id = when {
                isAuthorSelfUser -> R.string.label_system_message_read_receipt_changed_by_self
                else -> R.string.label_system_message_read_receipt_changed_by_other
            },
            formatArgs = arrayOf(author.asString().markdownBold(), receiptMode.asString().markdownBold())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.TeamMemberRemoved_Legacy -> buildContent(
        iconResId = R.drawable.ic_minus,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        pluralStringResource(
            id = R.plurals.label_system_message_team_member_left,
            count = 0,
            formatArgs = arrayOf(userName)
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.Knock -> buildContent(
        iconResId = R.drawable.ic_ping,
        iconTintColor = MaterialTheme.wireColorScheme.primary,
    ) {
        stringResource(
            id = when {
                isSelfTriggered -> R.string.label_system_message_self_user_knock
                else -> R.string.label_system_message_other_user_knock
            },
            formatArgs = arrayOf(author.asString().markdownBold())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.HistoryLost -> buildContent(
        iconResId = R.drawable.ic_info,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        stringResource(R.string.label_system_message_conversation_history_lost).toMarkdownAnnotatedString()
    }

    is SystemMessage.MLSWrongEpochWarning -> buildContent(
        iconResId = R.drawable.ic_info,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
        learnMoreLinkResId = R.string.url_system_message_learn_more_about_mls
    ) {
        stringResource(id = R.string.label_system_message_conversation_mls_wrong_epoch_error_handled).toMarkdownAnnotatedString()
    }

    is SystemMessage.ConversationDegraded -> buildContent(
        iconResId = R.drawable.ic_conversation_degraded_mls,
        iconTintColor = MaterialTheme.wireColorScheme.error,
    ) {
        stringResource(
            id = when (protocol) {
                Conversation.Protocol.MLS -> R.string.label_system_message_conversation_degraded_mls
                else -> R.string.label_system_message_conversation_degraded_proteus
            }
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.ConversationVerified -> buildContent(
        iconResId = when (protocol) {
            Conversation.Protocol.MLS -> R.drawable.ic_certificate_valid_mls
            else -> R.drawable.ic_certificate_valid_proteus
        },
        iconTintColor = when (protocol) {
            Conversation.Protocol.MLS -> MaterialTheme.wireColorScheme.positive
            else -> null
        },
    ) {
        stringResource(
            id = when (protocol) {
                Conversation.Protocol.MLS -> R.string.label_system_message_conversation_verified_mls
                else -> R.string.label_system_message_conversation_verified_proteus
            }
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.HistoryLostProtocolChanged -> buildContent(
        iconResId = R.drawable.ic_info,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        stringResource(id = R.string.label_system_message_conversation_history_lost_protocol_changed).toMarkdownAnnotatedString()
    }

    is SystemMessage.ConversationProtocolChanged -> buildContent(
        iconResId = R.drawable.ic_info,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
        learnMoreLinkResId = when (protocol) {
            Conversation.Protocol.PROTEUS -> null
            Conversation.Protocol.MIXED -> null
            Conversation.Protocol.MLS -> R.string.url_system_message_learn_more_about_mls
        }
    ) {
        stringResource(
            id = when (protocol) {
                Conversation.Protocol.PROTEUS -> R.string.label_system_message_conversation_protocol_changed_proteus
                Conversation.Protocol.MIXED -> R.string.label_system_message_conversation_protocol_changed_mixed
                Conversation.Protocol.MLS -> R.string.label_system_message_conversation_protocol_changed_mls
            }
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.ConversationProtocolChangedWithCallOngoing -> buildContent {
        stringResource(id = R.string.label_system_message_conversation_protocol_changed_during_a_call).toMarkdownAnnotatedString()
    }

    is SystemMessage.ConversationMessageTimerActivated -> buildContent(
        iconResId = R.drawable.ic_timer,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        stringResource(
            id = when {
                isAuthorSelfUser -> R.string.label_system_message_conversation_message_timer_activated_by_self
                else -> R.string.label_system_message_conversation_message_timer_activated_by_other
            },
            formatArgs = arrayOf(author.asString().markdownBold(), selfDeletionDuration.longLabel.asString().markdownBold())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.ConversationMessageTimerDeactivated -> buildContent(
        iconResId = R.drawable.ic_timer,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        stringResource(
            id = when {
                isAuthorSelfUser -> R.string.label_system_message_conversation_message_timer_deactivated_by_self
                else -> R.string.label_system_message_conversation_message_timer_deactivated_by_other
            },
            formatArgs = arrayOf(author.asString().markdownBold())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.ConversationMessageCreated -> buildContent(
        iconResId = R.drawable.ic_conversation,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
    ) {
        stringResource(
            id = when {
                isAuthorSelfUser -> R.string.label_system_message_conversation_started_by_self
                else -> R.string.label_system_message_conversation_started_by_other
            },
            formatArgs = arrayOf(author.asString().markdownBold())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.ConversationStartedWithMembers -> buildContent(
        iconResId = R.drawable.ic_contact,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
        expandable = memberNames.size > EXPANDABLE_THRESHOLD,
    ) { expanded ->
        stringResource(
            id = R.string.label_system_message_conversation_started_with_members,
            formatArgs = arrayOf(memberNames.limitList(expanded).toListMarkdownString())
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.FederationStopped -> buildContent(
        iconResId = R.drawable.ic_info,
        iconTintColor = MaterialTheme.wireColorScheme.onBackground,
        learnMoreLinkResId = R.string.url_federation_support,
    ) {
        stringResource(
            id = when {
                domainList.size > 1 -> R.string.label_system_message_federation_conection_removed
                else -> R.string.label_system_message_federation_removed
            },
            formatArgs = domainList.toTypedArray()
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.LegalHold -> buildContent(
        iconResId = R.drawable.ic_legal_hold,
        iconTintColor = MaterialTheme.wireColorScheme.error,
        learnMoreLinkResId = R.string.url_legal_hold_learn_more,
    ) {
        stringResource(
            id = when (this) {
                SystemMessage.LegalHold.Enabled.Self -> R.string.legal_hold_system_message_enabled_self
                is SystemMessage.LegalHold.Enabled.Others -> R.string.legal_hold_system_message_enabled_others
                SystemMessage.LegalHold.Enabled.Conversation -> R.string.legal_hold_system_message_enabled_conversation
                SystemMessage.LegalHold.Disabled.Self -> R.string.legal_hold_system_message_disabled_self
                is SystemMessage.LegalHold.Disabled.Others -> R.string.legal_hold_system_message_disabled_others
                SystemMessage.LegalHold.Disabled.Conversation -> R.string.legal_hold_system_message_disabled_conversation
            },
            formatArgs = memberNames?.let { memberNames ->
                arrayOf(memberNames.limitList(true).toListMarkdownString())
            } ?: arrayOf()
        ).toMarkdownAnnotatedString()
    }

    is SystemMessage.MemberFailedToAdd -> buildContent(
        iconResId = R.drawable.ic_info,
        iconTintColor = MaterialTheme.wireColorScheme.error,
        expandable = true,
        learnMoreLinkResId = when (type) {
            Type.Federation -> R.string.url_message_details_offline_backends_learn_more
            Type.LegalHold -> R.string.url_legal_hold_learn_more
            Type.Unknown -> null
        }
    ) { expanded ->
        val markdownTextStyle = DefaultMarkdownTextStyle.copy(
            normalColor = MaterialTheme.wireColorScheme.error,
            boldColor = MaterialTheme.wireColorScheme.error
        )
        val header = pluralStringResource(
            id = R.plurals.label_system_message_conversation_failed_add_members_header,
            count = memberNames.size,
            formatArgs = arrayOf(memberNames.size)
        )
        val message = pluralStringResource(
            id = when (type) {
                Type.Federation -> R.plurals.label_system_message_conversation_failed_add_members_details_federation
                Type.LegalHold -> R.plurals.label_system_message_conversation_failed_add_members_details_legal_hold
                Type.Unknown -> R.plurals.label_system_message_conversation_failed_add_members_details_unknown
            },
            count = memberNames.size,
            formatArgs = arrayOf(memberNames.limitList(expanded).toListMarkdownString())
        )
        buildAnnotatedString {
            append(header.toMarkdownAnnotatedString(markdownTextStyle))
            if (expanded) {
                appendVerticalSpace()
                append(message.toMarkdownAnnotatedString(markdownTextStyle))
            }
        }
    }

    is SystemMessage.ConversationMessageCreatedUnverifiedWarning -> buildContent(
        iconResId = R.drawable.ic_shield_holo,
        iconTintColor = MaterialTheme.wireColorScheme.onPositiveVariant,
        backgroundColor = MaterialTheme.wireColorScheme.positiveVariant,
        additionalVerticalPaddings = MaterialTheme.wireDimensions.spacing12x,
        learnMoreLinkResId = R.string.url_system_message_learn_more_about_e2ee
    ) {
        val markdownTextStyle = DefaultMarkdownTextStyle.copy(
            normalColor = MaterialTheme.wireColorScheme.onSurface,
            boldColor = MaterialTheme.wireColorScheme.onPositiveVariant
        )
        val header = stringResource(R.string.label_system_message_conversation_started_sensitive_information_header)
        val message = stringResource(R.string.label_system_message_conversation_started_sensitive_information_message)
        val footer = stringResource(R.string.label_system_message_conversation_started_sensitive_information_footer)
        buildAnnotatedString {
            append(header.toMarkdownAnnotatedString(markdownTextStyle))
            appendVerticalSpace()
            append(message.toMarkdownAnnotatedString(markdownTextStyle))
            appendVerticalSpace()
            append(footer.toMarkdownAnnotatedString(markdownTextStyle))
            appendVerticalSpace()
            append("") // so that "learn more" can be on a new line below another vertical space
        }
    }
}

private fun AnnotatedString.Builder.appendVerticalSpace() = withStyle(ParagraphStyle()) { append(" ") }

@Composable
fun String.toMarkdownAnnotatedString(style: MarkdownTextStyle = DefaultMarkdownTextStyle) = markdownText(this, style)

@Composable
private fun List<String>.toListMarkdownString(): String = when {
    this.isEmpty() -> ""
    this.size == 1 -> this[0].markdownBold()
    else -> stringResource(
        R.string.label_system_message_and,
        this.dropLast(1).joinToString(", ") { it.markdownBold() },
        this.last().markdownBold()
    )
}

@Composable
private fun List<UIText>.limitList(
    expanded: Boolean,
    collapsedSize: Int = EXPANDABLE_THRESHOLD,
    @PluralsRes quantityString: Int = R.plurals.label_system_message_x_more
): List<String> =
    if (expanded || this.size <= collapsedSize) {
        this.map { it.asString() }
    } else {
        val moreCount = this.size - (collapsedSize - 1) // the last visible place is taken by "and X more"
        this.take(collapsedSize - 1)
            .map { it.asString() }
            .plus(pluralStringResource(quantityString, moreCount, moreCount))
    }

@Composable
private fun buildContent(
    expandable: Boolean = false,
    @StringRes learnMoreLinkResId: Int? = null,
    @DrawableRes iconResId: Int = R.drawable.ic_info,
    iconTintColor: Color? = MaterialTheme.wireColorScheme.onBackground,
    iconSize: Dp = MaterialTheme.wireDimensions.systemMessageIconSize,
    additionalVerticalPaddings: Dp = MaterialTheme.wireDimensions.spacing0x,
    backgroundColor: Color? = null,
    annotatedStringBuilder: @Composable (expanded: Boolean) -> AnnotatedString,
) = SystemMessageContent(
    expandable = expandable,
    learnMoreLinkResId = learnMoreLinkResId,
    iconResId = iconResId,
    iconTintColor = iconTintColor,
    iconSize = iconSize,
    additionalVerticalPaddings = additionalVerticalPaddings,
    backgroundColor = backgroundColor,
    annotatedStringBuilder = annotatedStringBuilder
)

val DefaultMarkdownTextStyle
    @Composable get() = MarkdownTextStyle(
        normalStyle = MaterialTheme.wireTypography.body01,
        boldStyle = MaterialTheme.wireTypography.body02,
        normalColor = MaterialTheme.wireColorScheme.secondaryText,
        boldColor = MaterialTheme.wireColorScheme.onBackground
    )

@Stable
data class SystemMessageContent(
    val expandable: Boolean,
    @get:StringRes val learnMoreLinkResId: Int?,
    @get:DrawableRes val iconResId: Int?,
    val iconTintColor: Color?,
    val iconSize: Dp,
    val additionalVerticalPaddings: Dp,
    val backgroundColor: Color?,
    val annotatedStringBuilder: @Composable (expanded: Boolean) -> AnnotatedString
)

private const val EXPANDABLE_THRESHOLD = 4
