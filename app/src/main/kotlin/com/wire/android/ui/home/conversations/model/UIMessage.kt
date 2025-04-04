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

package com.wire.android.ui.home.conversations.model

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import com.wire.android.R
import com.wire.android.mapper.MessageDateTimeGroup
import com.wire.android.mapper.groupedUIMessageDateTime
import com.wire.android.mapper.shouldDisplayDatesDifferenceDivider
import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.ui.markdown.MarkdownConstants
import com.wire.android.ui.theme.Accent
import com.wire.android.util.Copyable
import com.wire.android.util.ui.LocalizedStringResource
import com.wire.android.util.ui.UIText
import com.wire.android.util.uiMessageDateTime
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageAttachment
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.util.DateTimeUtil.toIsoDateTimeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
sealed interface UIMessage {
    val conversationId: ConversationId
    val header: MessageHeader
    val source: MessageSource
    val messageContent: UIMessageContent?
    val sendingFailed: Boolean
    val decryptionFailed: Boolean
    val isPending: Boolean

    @Serializable
    data class Regular(
        override val conversationId: ConversationId,
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

        private val isReplyableContent: Boolean
            get() = messageContent is UIMessageContent.TextMessage ||
                    messageContent is UIMessageContent.AssetMessage ||
                    messageContent is UIMessageContent.AudioAssetMessage ||
                    messageContent is UIMessageContent.Location ||
                    messageContent is UIMessageContent.Regular

        /**
         * The message was sent from the sender (either self or others), and is available for other users
         * to retrieve from the backend, or is already retrieved.
         */
        private val isTheMessageAvailableToOtherUsers: Boolean
            get() = header.messageStatus.flowStatus is MessageFlowStatus.Delivered ||
                    header.messageStatus.flowStatus is MessageFlowStatus.Sent ||
                    header.messageStatus.flowStatus is MessageFlowStatus.Read

        val isReplyable: Boolean
            get() = isReplyableContent &&
                    isTheMessageAvailableToOtherUsers &&
                    !isDeleted &&
                    header.messageStatus.expirationStatus is ExpirationStatus.NotExpirable

        val isTextContentWithoutQuote = messageContent is UIMessageContent.TextMessage && messageContent.messageBody.quotedMessage == null

        val isLocation: Boolean = messageContent is UIMessageContent.Location
    }

    @Serializable
    data class System(
        override val conversationId: ConversationId,
        override val header: MessageHeader,
        override val source: MessageSource,
        override val messageContent: UIMessageContent.SystemMessage
    ) : UIMessage {
        override val sendingFailed: Boolean = header.messageStatus.flowStatus is MessageFlowStatus.Failure.Send
        override val decryptionFailed: Boolean = header.messageStatus.flowStatus is MessageFlowStatus.Failure.Decryption
        override val isPending: Boolean = header.messageStatus.flowStatus == MessageFlowStatus.Sending
        val addingFailed: Boolean = messageContent is UIMessageContent.SystemMessage.MemberFailedToAdd
    }
}

@Stable
@Serializable
data class MessageHeader(
    val username: UIText,
    val membership: Membership,
    val showLegalHoldIndicator: Boolean,
    val messageTime: MessageTime,
    val messageStatus: MessageStatus,
    val messageId: String,
    val userId: UserId? = null,
    val connectionState: ConnectionState?,
    val isSenderDeleted: Boolean,
    val isSenderUnavailable: Boolean,
    val clientId: ClientId? = null,
    val accent: Accent = Accent.Unknown,
    val guestExpiresAt: Instant? = null
)

@Stable
@Serializable
data class MessageFooter(
    val messageId: String,
    val reactions: Map<String, Int> = emptyMap(),
    val ownReactions: Set<String> = emptySet()
)

@Serializable
sealed interface ExpirationStatus {

    @Serializable
    data class Expirable(
        val expireAfter: Duration,
        val selfDeletionStatus: Message.ExpirationData.SelfDeletionStatus
    ) : ExpirationStatus

    @Serializable
    data object NotExpirable : ExpirationStatus
}

@Serializable
sealed interface MessageEditStatus {

    @Serializable
    data object NonEdited : MessageEditStatus

    @Serializable
    data class Edited(val formattedEditTimeStamp: String) : MessageEditStatus
}

@Serializable
sealed interface MessageFlowStatus {

    @Serializable
    data object Sending : MessageFlowStatus

    @Serializable
    data object Sent : MessageFlowStatus

    @Serializable
    sealed interface Failure : MessageFlowStatus {
        val errorText: UIText

        @Serializable
        sealed interface Send : Failure {
            @Serializable
            data class Locally(val isEdited: Boolean) : Send {
                override val errorText: UIText = when {
                    isEdited -> UIText.StringResource(R.string.label_message_edit_sent_failure)
                    else -> UIText.StringResource(R.string.label_message_sent_failure)
                }
            }

            @Serializable
            data class Remotely(val isEdited: Boolean, val backendWithFailure: String) : Send {
                override val errorText: UIText = when {
                    isEdited -> UIText.StringResource(R.string.label_message_edit_sent_remotely_failure, backendWithFailure)
                    else -> UIText.StringResource(R.string.label_message_sent_remotely_failure, backendWithFailure)
                }
            }
        }

        @Serializable
        data class Decryption(val isDecryptionResolved: Boolean, private val errorCode: Int?) : Failure {
            override val errorText: UIText = errorCode?.let {
                UIText.StringResource(R.string.label_message_decryption_failure_message_with_error_code, it)
            } ?: UIText.StringResource(R.string.label_message_decryption_failure_message)
        }
    }

    @Serializable
    data object Delivered : MessageFlowStatus

    @Serializable
    data class Read(val count: Long) : MessageFlowStatus
}

@Stable
@Serializable
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
@Serializable
sealed interface UILastMessageContent {

    @Serializable
    data object None : UILastMessageContent

    @Serializable
    data class TextMessage(val messageBody: MessageBody) : UILastMessageContent

    @Serializable
    data class SenderWithMessage(
        val sender: UIText,
        val message: UIText,
        val separator: String = MarkdownConstants.NON_BREAKING_SPACE
    ) : UILastMessageContent

    @Serializable
    data class MultipleMessage(
        val messages: List<UIText>,
        val separator: String = MarkdownConstants.NON_BREAKING_SPACE
    ) : UILastMessageContent

    @Serializable
    data class Connection(val connectionState: ConnectionState, val userId: UserId) : UILastMessageContent

    @Serializable
    data class VerificationChanged(@StringRes val textResId: Int) : UILastMessageContent
}

@Serializable
sealed interface UIMessageContent {

    @Serializable
    sealed interface Regular : UIMessageContent

    /**
     * IncompleteAssetMessage is a displayable asset that's missing the remote data.
     * Sometimes client receives two events about the same asset, first one with only part of the data ("preview" type from web),
     * so such asset shouldn't be shown until all the required data is received.
     */
    @Serializable
    data object IncompleteAssetMessage : UIMessageContent

    @Serializable
    sealed interface PartialDeliverable {
        val deliveryStatus: DeliveryStatusContent
    }

    @Serializable
    data class TextMessage(
        val messageBody: MessageBody,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular, PartialDeliverable, Copyable {
        override fun textToCopy(resources: Resources): String = messageBody.message.asString(resources)
    }

    @Serializable
    data class Multipart(
        val messageBody: MessageBody?,
        val attachments: PersistentList<MessageAttachment>,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular, PartialDeliverable, Copyable {
        override fun textToCopy(resources: Resources): String? = messageBody?.message?.asString(resources)
    }

    @Serializable
    data class Composite(
        val messageBody: MessageBody?,
        val buttonList: PersistentList<MessageButton>
    ) : Regular, Copyable {
        override fun textToCopy(resources: Resources): String? = messageBody?.message?.asString(resources)
    }

    @Serializable
    data object Deleted : Regular

    @Serializable
    data class RestrictedAsset(
        val mimeType: String,
        val assetSizeInBytes: Long,
        val assetName: String,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular, PartialDeliverable

    @Stable
    @Serializable
    data class AssetMessage(
        val assetName: String,
        val assetExtension: String,
        val assetId: AssetId,
        val assetSizeInBytes: Long,
        val assetDataPath: String?,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular, PartialDeliverable

    @Serializable
    data class ImageMessage(
        val assetId: AssetId,
        val asset: ImageAsset.PrivateAsset?,
        val width: Int,
        val height: Int,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular, PartialDeliverable

    @Serializable
    data class VideoMessage(
        val assetName: String,
        val assetExtension: String,
        val assetId: AssetId,
        val assetSizeInBytes: Long,
        val assetDataPath: String?,
        val width: Int?,
        val height: Int?,
        val duration: Long?,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular, PartialDeliverable

    @Stable
    @Serializable
    data class AudioAssetMessage(
        val assetName: String,
        val assetExtension: String,
        val assetId: AssetId,
        val audioMessageDurationInMs: Long,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery,
        val sizeInBytes: Long,
    ) : Regular, PartialDeliverable

    @Stable
    @Serializable
    data class Location(
        val latitude: Float,
        val longitude: Float,
        val name: String,
        val zoom: Int = DEFAULT_LOCATION_ZOOM,
        @StringRes val urlCoordinates: Int = R.string.url_maps_location_coordinates_fallback,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular, PartialDeliverable

    @Serializable
    sealed interface SystemMessage : UIMessageContent {
        val iconResId: Int?
        val stringRes: LocalizedStringResource
        val learnMoreResId: Int? get() = null
        val isSmallIcon: Boolean get() = true

        @Serializable
        data class Knock(
            val author: UIText,
            val isSelfTriggered: Boolean
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_ping
            override val stringRes = when {
                isSelfTriggered -> R.string.label_system_message_self_user_knock
                else -> R.string.label_system_message_other_user_knock
            }.toLocalizedStringResource()
        }

        @Serializable
        data class MemberAdded(
            val author: UIText,
            val memberNames: List<UIText>,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_add
            override val stringRes = when {
                isSelfTriggered -> R.string.label_system_message_added_by_self
                else -> R.string.label_system_message_added_by_other
            }.toLocalizedStringResource()
        }

        @Serializable
        data class MemberJoined(
            val author: UIText,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_add
            override val stringRes = when {
                isSelfTriggered -> R.string.label_system_message_joined_the_conversation_by_self
                else -> R.string.label_system_message_joined_the_conversation_by_other
            }.toLocalizedStringResource()
        }

        @Serializable
        data class MemberRemoved(
            val author: UIText,
            val memberNames: List<UIText>,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_minus
            override val stringRes = when {
                isSelfTriggered -> R.string.label_system_message_removed_by_self
                else -> R.string.label_system_message_removed_by_other
            }.toLocalizedStringResource()
        }

        @Serializable
        data class TeamMemberRemoved(
            val author: UIText,
            val memberNames: List<UIText>,
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_minus
            override val stringRes = R.plurals.label_system_message_team_member_left.toLocalizedPluralResource(memberNames.size)
        }

        @Serializable
        data class MemberLeft(
            val author: UIText,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_minus
            override val stringRes = when {
                isSelfTriggered -> R.string.label_system_message_left_the_conversation_by_self
                else -> R.string.label_system_message_left_the_conversation_by_other
            }.toLocalizedStringResource()
        }

        @Serializable
        data class FederationMemberRemoved(
            val memberNames: List<UIText>
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_minus
            override val stringRes = when {
                memberNames.size > 1 -> R.string.label_system_message_federation_many_member_removed
                else -> R.string.label_system_message_federation_one_member_removed
            }.toLocalizedStringResource()
        }

        @Serializable
        data class FederationStopped(
            val domainList: List<String>
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_info
            override val stringRes = when {
                domainList.size > 1 -> R.string.label_system_message_federation_conection_removed
                else -> R.string.label_system_message_federation_removed
            }.toLocalizedStringResource()
            override val learnMoreResId = R.string.url_federation_support
        }

        @Serializable
        sealed interface MissedCall : SystemMessage {
            val author: UIText
            override val iconResId get() = R.drawable.ic_call_end
            override val isSmallIcon get() = false

            @Serializable
            data class YouCalled(override val author: UIText) : MissedCall {
                override val stringRes = R.string.label_system_message_you_called.toLocalizedStringResource()
            }

            @Serializable
            data class OtherCalled(override val author: UIText) : MissedCall {
                override val stringRes = R.string.label_system_message_other_called.toLocalizedStringResource()
            }
        }

        @Serializable
        data class RenamedConversation(
            val author: UIText,
            val conversationName: String
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_edit
            override val stringRes = R.string.label_system_message_renamed_the_conversation.toLocalizedStringResource()
        }

        @Deprecated("Use TeamMemberRemoved")
        @Suppress("ClassNaming")
        @Serializable
        data class TeamMemberRemoved_Legacy(
            val userName: String
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_minus
            override val stringRes = R.plurals.label_system_message_team_member_left.toLocalizedPluralResource(0)
        }

        @Serializable
        data class CryptoSessionReset(
            val author: UIText
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_info
            override val stringRes = R.string.label_system_message_session_reset.toLocalizedStringResource()
        }

        @Serializable
        data class NewConversationReceiptMode(
            val receiptMode: UIText
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_view
            override val stringRes = R.string.label_system_message_new_conversation_receipt_mode.toLocalizedStringResource()
        }

        @Serializable
        data class ConversationReceiptModeChanged(
            val author: UIText,
            val receiptMode: UIText,
            val isAuthorSelfUser: Boolean = false
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_view
            override val stringRes = when {
                isAuthorSelfUser -> R.string.label_system_message_read_receipt_changed_by_self
                else -> R.string.label_system_message_read_receipt_changed_by_other
            }.toLocalizedStringResource()
        }

        @Serializable
        data class ConversationMessageTimerActivated(
            val author: UIText,
            val isAuthorSelfUser: Boolean = false,
            val selfDeletionDuration: SelfDeletionDuration
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_timer
            override val stringRes = when {
                isAuthorSelfUser -> R.string.label_system_message_conversation_message_timer_activated_by_self
                else -> R.string.label_system_message_conversation_message_timer_activated_by_other
            }.toLocalizedStringResource()
        }

        @Serializable
        data class ConversationMessageTimerDeactivated(
            val author: UIText,
            val isAuthorSelfUser: Boolean = false
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_timer
            override val stringRes = when {
                isAuthorSelfUser -> R.string.label_system_message_conversation_message_timer_deactivated_by_self
                else -> R.string.label_system_message_conversation_message_timer_deactivated_by_other
            }.toLocalizedStringResource()
        }

        @Serializable
        data object MLSWrongEpochWarning : SystemMessage {
            override val iconResId = R.drawable.ic_info
            override val stringRes = R.string.label_system_message_conversation_mls_wrong_epoch_error_handled.toLocalizedStringResource()
            override val learnMoreResId = R.string.url_system_message_learn_more_about_mls
        }

        @Serializable
        data class ConversationProtocolChanged(
            val protocol: Conversation.Protocol
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_info
            override val stringRes = when (protocol) {
                Conversation.Protocol.PROTEUS -> R.string.label_system_message_conversation_protocol_changed_proteus
                Conversation.Protocol.MIXED -> R.string.label_system_message_conversation_protocol_changed_mixed
                Conversation.Protocol.MLS -> R.string.label_system_message_conversation_protocol_changed_mls
            }.toLocalizedStringResource()
            override val learnMoreResId = when (protocol) {
                Conversation.Protocol.PROTEUS -> null
                Conversation.Protocol.MIXED -> null
                Conversation.Protocol.MLS -> R.string.url_system_message_learn_more_about_mls
            }
        }

        @Serializable
        data object ConversationProtocolChangedWithCallOngoing : SystemMessage {
            override val iconResId = R.drawable.ic_info
            override val stringRes = R.string.label_system_message_conversation_protocol_changed_during_a_call.toLocalizedStringResource()
        }

        @Serializable
        data object HistoryLost : SystemMessage {
            override val iconResId = R.drawable.ic_info
            override val stringRes = R.string.label_system_message_conversation_history_lost.toLocalizedStringResource()
        }

        @Serializable
        data object HistoryLostProtocolChanged : SystemMessage {
            override val iconResId = R.drawable.ic_info
            override val stringRes = R.string.label_system_message_conversation_history_lost_protocol_changed.toLocalizedStringResource()
        }

        @Serializable
        data class ConversationMessageCreated(
            val author: UIText,
            val isAuthorSelfUser: Boolean = false,
            val date: String
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_conversation
            override val stringRes = when {
                isAuthorSelfUser -> R.string.label_system_message_conversation_started_by_self
                else -> R.string.label_system_message_conversation_started_by_other
            }.toLocalizedStringResource()
        }

        @Serializable
        data class ConversationStartedWithMembers(
            val memberNames: List<UIText>
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_contact
            override val stringRes = R.string.label_system_message_conversation_started_with_members.toLocalizedStringResource()
        }

        @Serializable
        data class MemberFailedToAdd(
            val memberNames: List<UIText>,
            val type: Type,
        ) : SystemMessage {
            override val iconResId = R.drawable.ic_info
            override val stringRes = when {
                memberNames.size > 1 -> R.string.label_system_message_conversation_failed_add_many_members_details
                else -> R.string.label_system_message_conversation_failed_add_one_member_details
            }.toLocalizedStringResource()
            override val learnMoreResId = when (type) {
                Type.Federation -> R.string.url_message_details_offline_backends_learn_more
                Type.LegalHold -> R.string.url_legal_hold_learn_more
                Type.Unknown -> null
            }
            val usersCount = memberNames.size

            enum class Type { Federation, LegalHold, Unknown; }
        }

        @Serializable
        data class ConversationDegraded(
            val protocol: Conversation.Protocol
        ) : SystemMessage {
            override val iconResId =
                if (protocol == Conversation.Protocol.MLS) R.drawable.ic_conversation_degraded_mls
                else R.drawable.ic_shield_holo
            override val stringRes = LocalizedStringResource.String(
                if (protocol == Conversation.Protocol.MLS) R.string.label_system_message_conversation_degraded_mls
                else R.string.label_system_message_conversation_degraded_proteus
            )
        }

        @Serializable
        data class ConversationVerified(
            val protocol: Conversation.Protocol
        ) : SystemMessage {
            override val iconResId =
                if (protocol == Conversation.Protocol.MLS) R.drawable.ic_certificate_valid_mls
                else R.drawable.ic_certificate_valid_proteus
            override val stringRes = LocalizedStringResource.String(
                if (protocol == Conversation.Protocol.MLS) R.string.label_system_message_conversation_verified_mls
                else R.string.label_system_message_conversation_verified_proteus
            )
        }

        @Serializable
        data object ConversationMessageCreatedUnverifiedWarning : SystemMessage {
            override val iconResId = R.drawable.ic_info
            override val stringRes = LocalizedStringResource.String(
                R.string.label_system_message_conversation_started_sensitive_information
            )
        }

        @Serializable
        sealed interface LegalHold : SystemMessage {
            val memberNames: List<UIText>? get() = null
            override val iconResId get() = R.drawable.ic_legal_hold

            @Serializable
            sealed interface Enabled : LegalHold {
                override val learnMoreResId get() = R.string.url_legal_hold_learn_more

                @Serializable
                data object Self : Enabled {
                    override val stringRes = LocalizedStringResource.String(R.string.legal_hold_system_message_enabled_self)
                }

                @Serializable
                data class Others(override val memberNames: List<UIText>) : Enabled {
                    override val stringRes = LocalizedStringResource.String(R.string.legal_hold_system_message_enabled_others)
                }

                @Serializable
                data object Conversation : Enabled {
                    override val stringRes = LocalizedStringResource.String(R.string.legal_hold_system_message_enabled_conversation)
                }
            }

            @Serializable
            sealed interface Disabled : LegalHold {

                @Serializable
                data object Self : Disabled {
                    override val stringRes = LocalizedStringResource.String(R.string.legal_hold_system_message_disabled_self)
                }

                @Serializable
                data class Others(override val memberNames: List<UIText>) : Disabled {
                    override val stringRes = LocalizedStringResource.String(R.string.legal_hold_system_message_disabled_others)
                }

                @Serializable
                data object Conversation : Disabled {
                    override val stringRes =
                        LocalizedStringResource.String(R.string.legal_hold_system_message_disabled_conversation)
                }
            }
        }
    }
}

@Serializable
data class MessageBody(
    val message: UIText,
    val quotedMessage: UIQuotedMessage? = null
)

enum class MessageSource {
    Self, OtherUser
}

@Serializable
data class MessageTime(val instant: Instant) {
    val utcISO: String = instant.toIsoDateTimeString()
    val formattedDate: String = utcISO.uiMessageDateTime() ?: ""
    fun getFormattedDateGroup(now: Long): MessageDateTimeGroup? = utcISO.groupedUIMessageDateTime(now = now)
    fun shouldDisplayDatesDifferenceDivider(previousDate: String): Boolean =
        utcISO.shouldDisplayDatesDifferenceDivider(previousDate = previousDate)
}

@Stable
@Serializable
sealed interface DeliveryStatusContent {

    @Serializable
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

    @Serializable
    data object CompleteDelivery : DeliveryStatusContent
}

@Stable
@Serializable
data class MessageButton(
    val id: String,
    val text: String,
    val isSelected: Boolean,
)

private fun @receiver:StringRes Int.toLocalizedStringResource() = LocalizedStringResource.String(this)
private fun @receiver:PluralsRes Int.toLocalizedPluralResource(quantity: Int) = LocalizedStringResource.Plural(this, quantity)

const val DEFAULT_LOCATION_ZOOM = 20

fun UIMessageContent.isEditable() = this is UIMessageContent.TextMessage || this is UIMessageContent.Multipart
