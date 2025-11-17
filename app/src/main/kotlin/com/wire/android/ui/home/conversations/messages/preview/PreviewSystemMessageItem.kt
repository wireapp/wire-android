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

package com.wire.android.ui.home.conversations.messages.preview

import androidx.compose.runtime.Composable
import com.wire.android.ui.home.conversations.messages.item.SystemMessageItem
import com.wire.android.ui.home.conversations.mock.mockMessageWithKnock
import com.wire.android.ui.home.conversations.mock.mockUsersUITexts
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.data.conversation.Conversation

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageAdded7Users() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.MemberAdded(
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
fun PreviewSystemMessageAdded7UsersExpanded() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.MemberAdded(
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
            ),
            initiallyExpanded = true,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageAdded4Users() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.MemberAdded(
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
                messageContent = UIMessageContent.SystemMessage.MemberRemoved(
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
                messageContent = UIMessageContent.SystemMessage.MemberLeft(UIText.DynamicString("Barbara Cotolina"))
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
                messageContent = UIMessageContent.SystemMessage.MissedCall.OtherCalled(UIText.DynamicString("Barbara Cotolina"))
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
                messageContent = UIMessageContent.SystemMessage.Knock(UIText.DynamicString("Barbara Cotolina"), true)
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
                messageContent = UIMessageContent.SystemMessage.MemberFailedToAdd(
                    listOf(UIText.DynamicString("Barbara Cotolina")),
                    UIMessageContent.SystemMessage.MemberFailedToAdd.Type.Federation
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
                messageContent = UIMessageContent.SystemMessage.MemberFailedToAdd(
                    listOf(
                        UIText.DynamicString("Barbara Cotolina"),
                        UIText.DynamicString("Albert Lewis")
                    ),
                    UIMessageContent.SystemMessage.MemberFailedToAdd.Type.Federation
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
                messageContent = UIMessageContent.SystemMessage.MemberFailedToAdd(
                    listOf(
                        UIText.DynamicString("Barbara Cotolina"),
                        UIText.DynamicString("Albert Lewis")
                    ),
                    UIMessageContent.SystemMessage.MemberFailedToAdd.Type.Federation
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
                messageContent = UIMessageContent.SystemMessage.MemberFailedToAdd(
                    listOf(UIText.DynamicString("Barbara Cotolina")),
                    UIMessageContent.SystemMessage.MemberFailedToAdd.Type.LegalHold
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFailedToAddLegalHoldSingleExpanded() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.MemberFailedToAdd(
                    listOf(UIText.DynamicString("Barbara Cotolina")),
                    UIMessageContent.SystemMessage.MemberFailedToAdd.Type.LegalHold
                )
            ),
            initiallyExpanded = true,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageFailedToAddLegalHoldMultiple() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.MemberFailedToAdd(
                    listOf(
                        UIText.DynamicString("Barbara Cotolina"),
                        UIText.DynamicString("Albert Lewis")
                    ),
                    UIMessageContent.SystemMessage.MemberFailedToAdd.Type.LegalHold
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
                messageContent = UIMessageContent.SystemMessage.MemberFailedToAdd(
                    listOf(
                        UIText.DynamicString("Barbara Cotolina"),
                        UIText.DynamicString("Albert Lewis")
                    ),
                    UIMessageContent.SystemMessage.MemberFailedToAdd.Type.LegalHold
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
                messageContent = UIMessageContent.SystemMessage.FederationMemberRemoved(
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
                messageContent = UIMessageContent.SystemMessage.FederationMemberRemoved(
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
                messageContent = UIMessageContent.SystemMessage.FederationStopped(
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
                messageContent = UIMessageContent.SystemMessage.FederationStopped(
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
        SystemMessageItem(message = mockMessageWithKnock.copy(messageContent = UIMessageContent.SystemMessage.LegalHold.Enabled.Self))
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLegalHoldDisabledSelf() {
    WireTheme {
        SystemMessageItem(message = mockMessageWithKnock.copy(messageContent = UIMessageContent.SystemMessage.LegalHold.Disabled.Self))
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLegalHoldEnabledOthers() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.LegalHold.Enabled.Others(
                    mockUsersUITexts
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLegalHoldDisabledOthers() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.LegalHold.Disabled.Others(
                    mockUsersUITexts
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLegalHoldDisabledConversation() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.LegalHold.Disabled.Conversation
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageLegalHoldEnabledConversation() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.LegalHold.Enabled.Conversation
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageConversationVerifiedProteus() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.ConversationVerified(Conversation.Protocol.PROTEUS)
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
                messageContent = UIMessageContent.SystemMessage.ConversationVerified(Conversation.Protocol.MLS)
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
                messageContent = UIMessageContent.SystemMessage.ConversationDegraded(Conversation.Protocol.PROTEUS)
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
                messageContent = UIMessageContent.SystemMessage.ConversationDegraded(Conversation.Protocol.MLS)
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageConversationMessageCreatedUnverifiedWarning() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.ConversationMessageCreatedUnverifiedWarning
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSystemMessageConversationMessageAppsAccessEnabled() {
    WireTheme {
        SystemMessageItem(
            message = mockMessageWithKnock.copy(
                messageContent = UIMessageContent.SystemMessage.ConversationAppsEnabledChanged(
                    author = UIText.DynamicString("Barbara"),
                    isAuthorSelfUser = true,
                    isAccessEnabled = true
                )
            )
        )
    }
}
