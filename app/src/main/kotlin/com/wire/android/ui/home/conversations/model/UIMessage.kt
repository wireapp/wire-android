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

package com.wire.android.ui.home.conversations.model

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.util.Copyable
import com.wire.android.util.ui.UIText
import com.wire.android.util.uiMessageDateTime
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration

sealed interface UIMessage {
    val header: MessageHeader
    val source: MessageSource
    val messageContent: UIMessageContent?
    val sendingFailed: Boolean
    val decryptionFailed: Boolean
    val isPending: Boolean

    data class Regular(
        override val header: MessageHeader,
        override val source: MessageSource,
        val userAvatarData: UserAvatarData,
        override val messageContent: UIMessageContent.Regular?,
        val messageFooter: MessageFooter,
    ) : UIMessage {
        val isDeleted: Boolean = header.messageStatus.isDeleted
        override val sendingFailed: Boolean = header.messageStatus.flowStatus is MessageFlowStatus.Failure.Send
        override val decryptionFailed: Boolean = header.messageStatus.flowStatus is MessageFlowStatus.Failure.Decryption
        val isAvailable: Boolean = !isDeleted && !sendingFailed && !decryptionFailed
        override val isPending: Boolean = header.messageStatus.flowStatus == MessageFlowStatus.Sending
        val isMyMessage = source == MessageSource.Self
        val isAssetMessage = messageContent is UIMessageContent.AssetMessage
                || messageContent is UIMessageContent.ImageMessage
                || messageContent is UIMessageContent.AudioAssetMessage
        val isTextContentWithoutQuote = messageContent is UIMessageContent.TextMessage && messageContent.messageBody.quotedMessage == null
    }

    data class System(
        override val header: MessageHeader,
        override val source: MessageSource,
        override val messageContent: UIMessageContent.SystemMessage
    ) : UIMessage {
        override val sendingFailed: Boolean = header.messageStatus.flowStatus is MessageFlowStatus.Failure.Send
        override val decryptionFailed: Boolean = header.messageStatus.flowStatus is MessageFlowStatus.Failure.Decryption
        override val isPending: Boolean = header.messageStatus.flowStatus == MessageFlowStatus.Sending
        val addingFailed: Boolean = messageContent is UIMessageContent.SystemMessage.MemberFailedToAdd
        val singleUserAddFailed: Boolean =
            messageContent is UIMessageContent.SystemMessage.MemberFailedToAdd && messageContent.usersCount == 1
    }
}

@Stable
data class MessageHeader(
    val username: UIText,
    val membership: Membership,
    val isLegalHold: Boolean,
    val messageTime: MessageTime,
    val messageStatus: MessageStatus,
    val messageId: String,
    val userId: UserId? = null,
    val connectionState: ConnectionState?,
    val isSenderDeleted: Boolean,
    val isSenderUnavailable: Boolean,
    val clientId: ClientId? = null
)

@Stable
data class MessageFooter(
    val messageId: String,
    val reactions: Map<String, Int> = emptyMap(),
    val ownReactions: Set<String> = emptySet()
)

sealed class ExpirationStatus {
    data class Expirable(
        val expireAfter: Duration,
        val selfDeletionStatus: Message.ExpirationData.SelfDeletionStatus
    ) : ExpirationStatus()

    object NotExpirable : ExpirationStatus()
}

sealed class MessageEditStatus {
    object NonEdited : MessageEditStatus()
    data class Edited(val formattedEditTimeStamp: String) : MessageEditStatus()
}

sealed class MessageFlowStatus {

    object Sending : MessageFlowStatus()
    object Sent : MessageFlowStatus()
    sealed class Failure(val errorText: UIText) : MessageFlowStatus() {
        sealed class Send(errorText: UIText) : Failure(errorText) {
            data class Locally(val isEdited: Boolean) : Send(
                if (isEdited) {
                    UIText.StringResource(R.string.label_message_edit_sent_failure)
                } else {
                    UIText.StringResource(R.string.label_message_sent_failure)
                }
            )

            data class Remotely(val isEdited: Boolean, val backendWithFailure: String) : Send(
                if (isEdited) {
                    UIText.StringResource(
                        R.string.label_message_edit_sent_remotely_failure,
                        backendWithFailure
                    )
                } else {
                    UIText.StringResource(
                        R.string.label_message_sent_remotely_failure,
                        backendWithFailure
                    )
                }
            )
        }

        data class Decryption(val isDecryptionResolved: Boolean) : Failure(
            UIText.StringResource(R.string.label_message_decryption_failure_message)
        )
    }

    object Delivered : MessageFlowStatus()

    data class Read(val count: Long) : MessageFlowStatus()
}

@Stable
data class MessageStatus(
    val flowStatus: MessageFlowStatus,
    val expirationStatus: ExpirationStatus,
    val editStatus: MessageEditStatus = MessageEditStatus.NonEdited,
    val isDeleted: Boolean = false
) {

    // text shown between the user name and the content in the outlined box with a text inside
    val badgeText: UIText? = when {
        isDeleted -> UIText.StringResource(R.string.deleted_message_text)
        editStatus is MessageEditStatus.Edited -> UIText.StringResource(
            R.string.label_message_status_edited_with_date,
            editStatus.formattedEditTimeStamp
        )

        else -> null
    }
}

@Stable
sealed class UILastMessageContent {
    object None : UILastMessageContent()

    data class TextMessage(val messageBody: MessageBody) : UILastMessageContent()

    data class SenderWithMessage(val sender: UIText, val message: UIText, val separator: String = " ") : UILastMessageContent()

    data class MultipleMessage(val messages: List<UIText>, val separator: String = " ") : UILastMessageContent()

    data class Connection(val connectionState: ConnectionState, val userId: UserId) : UILastMessageContent()

    data class VerificationChanged(@StringRes val textResId: Int) : UILastMessageContent()
}

sealed class UIMessageContent {

    sealed class Regular : UIMessageContent()

    /**
     * IncompleteAssetMessage is a displayable asset that's missing the remote data.
     * Sometimes client receives two events about the same asset, first one with only part of the data ("preview" type from web),
     * so such asset shouldn't be shown until all the required data is received.
     */
    object IncompleteAssetMessage : UIMessageContent()

    interface PartialDeliverable {
        val deliveryStatus: DeliveryStatusContent
    }

    data class TextMessage(
        val messageBody: MessageBody,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular(), PartialDeliverable, Copyable {
        override fun textToCopy(resources: Resources): String = messageBody.message.asString(resources)
    }

    data class Composite(
        val messageBody: MessageBody?,
        val buttonList: List<MessageButton>
    ) : Regular(), Copyable {
        override fun textToCopy(resources: Resources): String? = messageBody?.message?.asString(resources)
    }

    object Deleted : Regular()

    data class RestrictedAsset(
        val mimeType: String,
        val assetSizeInBytes: Long,
        val assetName: String,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular(), PartialDeliverable

    @Stable
    data class AssetMessage(
        val assetName: String,
        val assetExtension: String,
        val assetId: AssetId,
        val assetSizeInBytes: Long,
        val uploadStatus: Message.UploadStatus,
        val downloadStatus: Message.DownloadStatus,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular(), PartialDeliverable

    data class ImageMessage(
        val assetId: AssetId,
        val asset: ImageAsset.PrivateAsset?,
        val width: Int,
        val height: Int,
        val uploadStatus: Message.UploadStatus,
        val downloadStatus: Message.DownloadStatus,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular(), PartialDeliverable

    @Stable
    data class AudioAssetMessage(
        val assetName: String,
        val assetExtension: String,
        val assetId: AssetId,
        val audioMessageDurationInMs: Long,
        val uploadStatus: Message.UploadStatus,
        val downloadStatus: Message.DownloadStatus,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular(), PartialDeliverable

    sealed class SystemMessage(
        @DrawableRes val iconResId: Int?,
        @StringRes open val stringResId: Int,
        val isSmallIcon: Boolean = true,
        val additionalContent: String = "",
        @StringRes val learnMoreResId: Int? = null
    ) : UIMessageContent() {

        data class Knock(val author: UIText, val isSelfTriggered: Boolean) : SystemMessage(
            R.drawable.ic_ping,
            if (isSelfTriggered) R.string.label_system_message_self_user_knock else R.string.label_system_message_other_user_knock
        )

        data class MemberAdded(
            val author: UIText,
            val memberNames: List<UIText>,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage(
            R.drawable.ic_add,
            if (isSelfTriggered) R.string.label_system_message_added_by_self else R.string.label_system_message_added_by_other
        )

        data class MemberJoined(
            val author: UIText,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage(
            R.drawable.ic_add,
            if (isSelfTriggered) {
                R.string.label_system_message_joined_the_conversation_by_self
            } else {
                R.string.label_system_message_joined_the_conversation_by_other
            }
        )

        data class MemberRemoved(
            val author: UIText,
            val memberNames: List<UIText>,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage(
            R.drawable.ic_minus,
            if (isSelfTriggered) R.string.label_system_message_removed_by_self else R.string.label_system_message_removed_by_other
        )

        data class MemberLeft(
            val author: UIText,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage(
            R.drawable.ic_minus,
            if (isSelfTriggered) {
                R.string.label_system_message_left_the_conversation_by_self
            } else {
                R.string.label_system_message_left_the_conversation_by_other
            }
        )

        data class FederationMemberRemoved(
            val memberNames: List<UIText>
        ) : SystemMessage(
            R.drawable.ic_minus,
            if (memberNames.size > 1) {
                R.string.label_system_message_federation_many_member_removed
            } else {
                R.string.label_system_message_federation_one_member_removed
            }
        )

        data class FederationStopped(
            val domainList: List<String>
        ) : SystemMessage(
            R.drawable.ic_info,
            if (domainList.size > 1) {
                R.string.label_system_message_federation_conection_removed
            } else {
                R.string.label_system_message_federation_removed
            },
            learnMoreResId = R.string.url_federation_support
        )

        sealed class MissedCall(
            open val author: UIText,
            @StringRes override val stringResId: Int
        ) : SystemMessage(R.drawable.ic_call_end, stringResId, false) {

            data class YouCalled(override val author: UIText) : MissedCall(author, R.string.label_system_message_you_called)
            data class OtherCalled(override val author: UIText) : MissedCall(author, R.string.label_system_message_other_called)
        }

        data class RenamedConversation(val author: UIText, val content: MessageContent.ConversationRenamed) :
            SystemMessage(R.drawable.ic_edit, R.string.label_system_message_renamed_the_conversation, true, content.conversationName)

        data class TeamMemberRemoved(val content: MessageContent.TeamMemberRemoved) :
            SystemMessage(R.drawable.ic_minus, R.string.label_system_message_team_member_left, true, content.userName)

        data class CryptoSessionReset(val author: UIText) :
            SystemMessage(R.drawable.ic_info, R.string.label_system_message_session_reset, true)

        data class NewConversationReceiptMode(
            val receiptMode: UIText
        ) : SystemMessage(R.drawable.ic_view, R.string.label_system_message_new_conversation_receipt_mode)

        data class ConversationReceiptModeChanged(
            val author: UIText,
            val receiptMode: UIText,
            val isAuthorSelfUser: Boolean = false
        ) : SystemMessage(
            R.drawable.ic_view,
            if (isAuthorSelfUser) {
                R.string.label_system_message_read_receipt_changed_by_self
            } else {
                R.string.label_system_message_read_receipt_changed_by_other
            }
        )

        data class ConversationMessageTimerActivated(
            val author: UIText,
            val isAuthorSelfUser: Boolean = false,
            val selfDeletionDuration: SelfDeletionDuration
        ) : SystemMessage(
            R.drawable.ic_timer,
            if (isAuthorSelfUser) {
                R.string.label_system_message_conversation_message_timer_activated_by_self
            } else {
                R.string.label_system_message_conversation_message_timer_activated_by_other
            }
        )

        data class ConversationMessageTimerDeactivated(
            val author: UIText,
            val isAuthorSelfUser: Boolean = false
        ) : SystemMessage(
            R.drawable.ic_timer,
            if (isAuthorSelfUser) {
                R.string.label_system_message_conversation_message_timer_deactivated_by_self
            } else {
                R.string.label_system_message_conversation_message_timer_deactivated_by_other
            }
        )

        class MLSWrongEpochWarning : SystemMessage(
            iconResId = R.drawable.ic_info,
            stringResId = R.string.label_system_message_conversation_mls_wrong_epoch_error_handled,
            isSmallIcon = true
        )

        data class ConversationProtocolChanged(
            val protocol: Conversation.Protocol
        ) : SystemMessage(
            R.drawable.ic_info,
            when (protocol) {
                Conversation.Protocol.PROTEUS -> R.string.label_system_message_conversation_protocol_changed_proteus
                Conversation.Protocol.MIXED -> R.string.label_system_message_conversation_protocol_changed_mixed
                Conversation.Protocol.MLS -> R.string.label_system_message_conversation_protocol_changed_mls
            }
        )

        object HistoryLost : SystemMessage(
            R.drawable.ic_info,
            R.string.label_system_message_conversation_history_lost,
            true
        )

        object HistoryLostProtocolChanged : SystemMessage(
            R.drawable.ic_info,
            R.string.label_system_message_conversation_history_lost_protocol_changed,
            true
        )

        data class ConversationMessageCreated(
            val author: UIText,
            val isAuthorSelfUser: Boolean = false,
            val date: String
        ) : SystemMessage(
            R.drawable.ic_conversation,
            if (isAuthorSelfUser) {
                R.string.label_system_message_conversation_started_by_self
            } else {
                R.string.label_system_message_conversation_started_by_other
            }
        )

        data class ConversationStartedWithMembers(
            val memberNames: List<UIText>
        ) : SystemMessage(
            R.drawable.ic_contact,
            R.string.label_system_message_conversation_started_with_members
        )

        data class MemberFailedToAdd(
            val memberNames: List<UIText>
        ) : SystemMessage(
            R.drawable.ic_info,
            if (memberNames.size > 1) {
                R.string.label_system_message_conversation_failed_add_many_members_details
            } else {
                R.string.label_system_message_conversation_failed_add_one_member_details
            }
        ) {
            val usersCount = memberNames.size
        }

        data class ConversationDegraded(val protocol: Conversation.Protocol) : SystemMessage(
            if (protocol == Conversation.Protocol.MLS) R.drawable.ic_conversation_degraded_mls
            else R.drawable.ic_shield_holo,
            if (protocol == Conversation.Protocol.MLS) R.string.label_system_message_conversation_degraded_mls
            else R.string.label_system_message_conversation_degraded_proteus
        )

        data class ConversationVerified(val protocol: Conversation.Protocol) : SystemMessage(
            if (protocol == Conversation.Protocol.MLS) R.drawable.ic_certificate_valid_mls
            else R.drawable.ic_certificate_valid_proteus,
            if (protocol == Conversation.Protocol.MLS) R.string.label_system_message_conversation_verified_mls
            else R.string.label_system_message_conversation_verified_proteus
        )

        data object ConversationMessageCreatedUnverifiedWarning : SystemMessage(
            R.drawable.ic_info,
            R.string.label_system_message_conversation_started_sensitive_information
        )
    }
}

data class MessageBody(
    val message: UIText,
    val quotedMessage: UIQuotedMessage? = null
)

sealed class UIQuotedMessage {

    object UnavailableData : UIQuotedMessage()

    data class UIQuotedData(
        val messageId: String,
        val senderId: UserId,
        val senderName: UIText,
        val originalMessageDateDescription: UIText,
        val editedTimeDescription: UIText?,
        val quotedContent: Content
    ) : UIQuotedMessage() {

        sealed interface Content

        data class Text(val value: String) : Content

        data class GenericAsset(
            val assetName: String?,
            val assetMimeType: String
        ) : Content

        data class DisplayableImage(
            val displayable: ImageAsset.PrivateAsset
        ) : Content

        object AudioMessage : Content

        object Deleted : Content
        object Invalid : Content
    }
}

enum class MessageSource {
    Self, OtherUser
}

data class MessageTime(val utcISO: String) {
    val formattedDate = utcISO.uiMessageDateTime() ?: ""
}

@Stable
sealed interface DeliveryStatusContent {
    class PartialDelivery(
        val failedRecipients: ImmutableList<UIText> = persistentListOf(),
        val noClients: ImmutableMap<String, List<UIText>> = persistentMapOf(),
    ) : DeliveryStatusContent {
        val hasFailures: Boolean
            get() = totalUsersWithFailures > 0

        val filteredRecipientsFailure by lazy { failedRecipients.filter { it !in noClients.values.flatten() }.toImmutableList() }
        val isSingleUserFailure by lazy { totalUsersWithFailures == 1 }
        val totalUsersWithFailures by lazy { (failedRecipients.size + noClients.values.distinct().sumOf { it.size }) }
    }

    object CompleteDelivery : DeliveryStatusContent
}

@Stable
data class MessageButton(
    val id: String,
    val text: String,
    val isSelected: Boolean,
)
