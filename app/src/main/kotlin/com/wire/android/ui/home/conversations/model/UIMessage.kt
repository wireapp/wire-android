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
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import com.wire.android.R
import com.wire.android.mapper.MessageDateTimeGroup
import com.wire.android.mapper.groupedUIMessageDateTime
import com.wire.android.mapper.shouldDisplayDatesDifferenceDivider
import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.messagetypes.image.VisualMediaParams
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.ui.markdown.MarkdownConstants
import com.wire.android.ui.theme.Accent
import com.wire.android.util.Copyable
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

        val assetParams: VisualMediaParams? = when (messageContent) {
            is UIMessageContent.ImageMessage -> messageContent.params
            is UIMessageContent.VideoMessage -> messageContent.params
            else -> null
        }

        val hasAssetParams: Boolean = assetParams != null && !decryptionFailed

        private val isReplyableContent: Boolean
            get() = messageContent is UIMessageContent.TextMessage ||
                    messageContent is UIMessageContent.AssetMessage ||
                    messageContent is UIMessageContent.AudioAssetMessage ||
                    messageContent is UIMessageContent.VideoMessage ||
                    messageContent is UIMessageContent.Location ||
                    messageContent is UIMessageContent.Multipart ||
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

        val isReactionAllowed: Boolean
            get() = !isDeleted &&
                    !isPending &&
                    messageContent !is UIMessageContent.Composite &&
                    header.messageStatus.expirationStatus !is ExpirationStatus.Expirable

        val isSwipeable: Boolean
            get() = isReplyable || isReactionAllowed

        val isTextContentWithoutQuote = messageContent is UIMessageContent.TextMessage && messageContent.messageBody.quotedMessage == null

        val isLocation: Boolean = messageContent is UIMessageContent.Location
        val isMultipart: Boolean = messageContent is UIMessageContent.Multipart
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
    val reactionMap: Map<String, Reaction> = emptyMap()
) {
    // Backward-compatible properties for gradual migration
    @Deprecated("Use reactionMap instead", ReplaceWith("reactionMap.mapValues { it.value.count }"))
    val reactions: Map<String, Int>
        get() = reactionMap.mapValues { it.value.count }

    @Deprecated("Use reactionMap instead", ReplaceWith("reactionMap.filter { it.value.isSelf }.keys"))
    val ownReactions: Set<String>
        get() = reactionMap.filter { it.value.isSelf }.keys
}

@Stable
@Serializable
data class Reaction(
    val count: Int,
    val isSelf: Boolean
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
        val params: VisualMediaParams,
        override val deliveryStatus: DeliveryStatusContent = DeliveryStatusContent.CompleteDelivery
    ) : Regular, PartialDeliverable

    @Serializable
    data class VideoMessage(
        val assetName: String,
        val assetExtension: String,
        val assetId: AssetId,
        val assetSizeInBytes: Long,
        val assetDataPath: String?,
        val params: VisualMediaParams,
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

        @Serializable
        data class Knock(
            val author: UIText,
            val isSelfTriggered: Boolean
        ) : SystemMessage

        @Serializable
        data class MemberAdded(
            val author: UIText,
            val memberNames: List<UIText>,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage

        @Serializable
        data class MemberJoined(
            val author: UIText,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage

        @Serializable
        data class MemberRemoved(
            val author: UIText,
            val memberNames: List<UIText>,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage

        @Serializable
        data class TeamMemberRemoved(
            val author: UIText,
            val memberNames: List<UIText>,
        ) : SystemMessage

        @Serializable
        data class MemberLeft(
            val author: UIText,
            val isSelfTriggered: Boolean = false
        ) : SystemMessage

        @Serializable
        data class FederationMemberRemoved(
            val memberNames: List<UIText>
        ) : SystemMessage

        @Serializable
        data class FederationStopped(
            val domainList: List<String>
        ) : SystemMessage

        @Serializable
        sealed interface MissedCall : SystemMessage {
            val author: UIText

            @Serializable
            data class YouCalled(override val author: UIText) : MissedCall

            @Serializable
            data class OtherCalled(override val author: UIText) : MissedCall
        }

        @Serializable
        data class RenamedConversation(
            val author: UIText,
            val conversationName: String
        ) : SystemMessage

        @Deprecated("Use TeamMemberRemoved")
        @Suppress("ClassNaming")
        @Serializable
        data class TeamMemberRemoved_Legacy(
            val userName: String
        ) : SystemMessage

        @Serializable
        data class CryptoSessionReset(
            val author: UIText
        ) : SystemMessage

        @Serializable
        data class NewConversationReceiptMode(
            val receiptMode: UIText
        ) : SystemMessage

        @Serializable
        data class ConversationReceiptModeChanged(
            val author: UIText,
            val receiptMode: UIText,
            val isAuthorSelfUser: Boolean = false
        ) : SystemMessage

        @Serializable
        data class ConversationMessageTimerActivated(
            val author: UIText,
            val isAuthorSelfUser: Boolean = false,
            val selfDeletionDuration: SelfDeletionDuration
        ) : SystemMessage

        @Serializable
        data class ConversationMessageTimerDeactivated(
            val author: UIText,
            val isAuthorSelfUser: Boolean = false
        ) : SystemMessage

        @Serializable
        data object MLSWrongEpochWarning : SystemMessage

        @Serializable
        data class ConversationProtocolChanged(
            val protocol: Conversation.Protocol
        ) : SystemMessage

        @Serializable
        data object ConversationProtocolChangedWithCallOngoing : SystemMessage

        @Serializable
        data object HistoryLost : SystemMessage

        @Serializable
        data object HistoryLostProtocolChanged : SystemMessage

        @Serializable
        data class ConversationMessageCreated(
            val author: UIText,
            val isAuthorSelfUser: Boolean = false,
            val date: String
        ) : SystemMessage

        @Serializable
        data class ConversationStartedWithMembers(
            val memberNames: List<UIText>
        ) : SystemMessage

        @Serializable
        data class MemberFailedToAdd(
            val memberNames: List<UIText>,
            val type: Type,
        ) : SystemMessage {
            enum class Type { Federation, LegalHold, Unknown; }
        }

        @Serializable
        data class ConversationDegraded(
            val protocol: Conversation.Protocol
        ) : SystemMessage

        @Serializable
        data class ConversationVerified(
            val protocol: Conversation.Protocol
        ) : SystemMessage

        @Serializable
        data object ConversationMessageCreatedUnverifiedWarning : SystemMessage

        @Serializable
        sealed interface LegalHold : SystemMessage {
            val memberNames: List<UIText>? get() = null

            @Serializable
            sealed interface Enabled : LegalHold {

                @Serializable
                data object Self : Enabled

                @Serializable
                data class Others(override val memberNames: List<UIText>) : Enabled

                @Serializable
                data object Conversation : Enabled
            }

            @Serializable
            sealed interface Disabled : LegalHold {

                @Serializable
                data object Self : Disabled

                @Serializable
                data class Others(override val memberNames: List<UIText>) : Disabled

                @Serializable
                data object Conversation : Disabled
            }
        }

        @Serializable
        data object NewConversationWithCellStarted : SystemMessage

        @Serializable
        data object NewConversationWithCellSelfDeleteDisabled : SystemMessage
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

    val hasAnyFailures: Boolean
        get() = when (this) {
            CompleteDelivery -> false
            is PartialDelivery -> hasFailures
        }

    @Serializable
    class PartialDelivery(
        val failedRecipients: ImmutableList<UIText> = persistentListOf(),
        val noClients: ImmutableMap<String, List<UIText>> = persistentMapOf(),
    ) : DeliveryStatusContent {
        val hasFailures: Boolean
            get() = totalUsersWithFailures > 0

        val filteredRecipientsFailure by lazy {
            failedRecipients.filter { it !in noClients.values.flatten() }.toImmutableList()
        }
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

const val DEFAULT_LOCATION_ZOOM = 20

fun UIMessageContent.isEditable() = this is UIMessageContent.TextMessage || this is UIMessageContent.Multipart
