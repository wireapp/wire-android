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

package com.wire.android.mapper

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.asset.isDisplayableImageMimeType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.MessageContent.Asset
import com.wire.kalium.logic.data.message.MessageContent.MemberChange
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.Added
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.Removed
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.sync.receiver.conversation.message.hasValidRemoteData
import com.wire.kalium.logic.util.isGreaterThan
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

// TODO: splits mapping into more classes
@Suppress("TooManyFunctions")
class MessageContentMapper @Inject constructor(
    private val messageResourceProvider: MessageResourceProvider,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val isoFormatter: ISOFormatter,
) {

    fun fromMessage(
        message: Message.Standalone,
        userList: List<User>
    ): UIMessageContent? {
        return when (message) {
            is Message.Regular -> {
                when (message.visibility) {
                    Message.Visibility.VISIBLE -> mapRegularMessage(message, userList.findUser(message.senderUserId))
                    Message.Visibility.DELETED -> UIMessageContent.Deleted // for deleted , there is a state label displayed only
                    Message.Visibility.HIDDEN -> null // we don't want to show hidden message content in any way
                }
            }

            is Message.System -> {
                when (message.visibility) {
                    Message.Visibility.VISIBLE -> mapSystemMessage(message, userList)
                    Message.Visibility.DELETED,
                    Message.Visibility.HIDDEN -> null // we don't want to show hidden nor deleted message content in any way
                }
            }
        }
    }

    private fun mapSystemMessage(
        message: Message.System,
        members: List<User>
    ) = when (val content = message.content) {
        is MemberChange -> mapMemberChangeMessage(content, message.senderUserId, members)
        is MessageContent.MissedCall -> mapMissedCallMessage(message.senderUserId, members)
        is MessageContent.ConversationRenamed -> mapConversationRenamedMessage(message.senderUserId, content, members)
        is MessageContent.TeamMemberRemoved -> mapTeamMemberRemovedMessage(content)
        is MessageContent.CryptoSessionReset -> mapResetSession(message.senderUserId, members)
        is MessageContent.NewConversationReceiptMode -> mapNewConversationReceiptMode(content)
        is MessageContent.ConversationReceiptModeChanged -> mapConversationReceiptModeChanged(message.senderUserId, content, members)
        is MessageContent.HistoryLost -> mapConversationHistoryLost()
        is MessageContent.ConversationMessageTimerChanged -> mapConversationTimerChanged(message.senderUserId, content, members)
    }

    // TODO KBX cover with tests
    private fun mapConversationTimerChanged(
        senderUserId: UserId,
        content: MessageContent.ConversationMessageTimerChanged,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = toSystemMessageMemberName(
            user = sender,
            type = SelfNameType.ResourceTitleCase
        )

        return if (content.messageTimer != null) {
            UIMessageContent.SystemMessage.ConversationMessageTimerActivated(
                author = authorName,
                isAuthorSelfUser = sender is SelfUser,
                selfDeletionDuration = (content.messageTimer ?: 0L).milliseconds.toSelfDeletionDuration()
            )
        } else {
            UIMessageContent.SystemMessage.ConversationMessageTimerDeactivated(
                author = authorName,
                isAuthorSelfUser = sender is SelfUser
            )
        }
    }

    private fun mapResetSession(
        senderUserId: UserId,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = toSystemMessageMemberName(user = sender, type = SelfNameType.ResourceTitleCase)
        return UIMessageContent.SystemMessage.CryptoSessionReset(authorName)
    }

    private fun mapMissedCallMessage(
        senderUserId: UserId,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = toSystemMessageMemberName(
            user = sender,
            type = SelfNameType.ResourceTitleCase
        )
        return if (sender is SelfUser) {
            UIMessageContent.SystemMessage.MissedCall.YouCalled(authorName)
        } else {
            UIMessageContent.SystemMessage.MissedCall.OtherCalled(authorName)
        }
    }

    private fun mapNewConversationReceiptMode(
        content: MessageContent.NewConversationReceiptMode
    ): UIMessageContent.SystemMessage {
        return UIMessageContent.SystemMessage.NewConversationReceiptMode(
            receiptMode = when (content.receiptMode) {
                true -> UIText.StringResource(R.string.label_system_message_receipt_mode_on)
                else -> UIText.StringResource(R.string.label_system_message_receipt_mode_off)
            }
        )
    }

    private fun mapConversationReceiptModeChanged(
        senderUserId: UserId,
        content: MessageContent.ConversationReceiptModeChanged,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = toSystemMessageMemberName(
            user = sender,
            type = SelfNameType.ResourceTitleCase
        )
        return UIMessageContent.SystemMessage.ConversationReceiptModeChanged(
            author = authorName,
            receiptMode = when (content.receiptMode) {
                true -> UIText.StringResource(R.string.label_system_message_receipt_mode_on)
                else -> UIText.StringResource(R.string.label_system_message_receipt_mode_off)
            },
            isAuthorSelfUser = sender is SelfUser
        )
    }

    private fun mapTeamMemberRemovedMessage(
        content: MessageContent.TeamMemberRemoved,
    ): UIMessageContent.SystemMessage = UIMessageContent.SystemMessage.TeamMemberRemoved(content)

    private fun mapConversationRenamedMessage(
        senderUserId: UserId,
        content: MessageContent.ConversationRenamed,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = toSystemMessageMemberName(
            user = sender,
            type = SelfNameType.ResourceTitleCase
        )
        return UIMessageContent.SystemMessage.RenamedConversation(authorName, content)
    }

    fun mapMemberChangeMessage(
        content: MemberChange,
        senderUserId: UserId,
        userList: List<User>
    ): UIMessageContent.SystemMessage? {
        val sender = userList.findUser(userId = senderUserId)
        val isAuthorSelfAction = content.members.size == 1 && senderUserId == content.members.first()
        val isSelfTriggered = sender is SelfUser
        val authorName = toSystemMessageMemberName(user = sender, type = SelfNameType.ResourceTitleCase)
        val memberNameList = content.members.map {
            toSystemMessageMemberName(
                user = userList.findUser(userId = it),
                type = SelfNameType.ResourceLowercase
            )
        }
        return when (content) {
            is Added ->
                if (isAuthorSelfAction) {
                    UIMessageContent.SystemMessage.MemberJoined(author = authorName, isSelfTriggered = isSelfTriggered)
                } else {
                    UIMessageContent.SystemMessage.MemberAdded(
                        author = authorName,
                        memberNames = memberNameList,
                        isSelfTriggered = isSelfTriggered
                    )
                }

            is Removed ->
                if (isAuthorSelfAction) {
                    UIMessageContent.SystemMessage.MemberLeft(author = authorName, isSelfTriggered = isSelfTriggered)
                } else {
                    UIMessageContent.SystemMessage.MemberRemoved(
                        author = authorName,
                        memberNames = memberNameList,
                        isSelfTriggered = isSelfTriggered
                    )
                }
        }
    }

    private fun mapConversationHistoryLost(): UIMessageContent.SystemMessage = UIMessageContent.SystemMessage.HistoryLost()

    private fun mapRegularMessage(
        message: Message.Regular,
        sender: User?
    ) = when (val content = message.content) {
        is Asset -> {
            when (val metadata = content.value.metadata) {
                is AssetContent.AssetMetadata.Audio -> {
                    mapAudio(
                        assetContent = content.value,
                        metadata = metadata
                    )
                }

                is AssetContent.AssetMetadata.Image, is AssetContent.AssetMetadata.Video, null -> {
                    val assetMessageContentMetadata = AssetMessageContentMetadata(content.value)
                    toUIMessageContent(assetMessageContentMetadata, message, sender)
                }
            }
        }
        // We are mapping regular knock message to system message, because it's UI is almost the same as system message
        is MessageContent.Knock -> UIMessageContent.SystemMessage.Knock(
            if (message.isSelfMessage) {
                UIText.StringResource(messageResourceProvider.memberNameYouTitlecase)
            } else {
                sender?.name.orUnknownName()
            }
        )

        is MessageContent.RestrictedAsset -> toRestrictedAsset(content.mimeType, content.sizeInBytes, content.name)
        else -> toText(message.conversationId, content)
    }

    private fun mapAudio(
        assetContent: AssetContent,
        metadata: AssetContent.AssetMetadata.Audio,
    ): UIMessageContent {
        with(assetContent) {
            return UIMessageContent.AudioAssetMessage(
                assetName = name ?: "",
                assetExtension = mimeType,
                assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                audioMessageDurationInMs = metadata.durationMs ?: 0,
                uploadStatus = uploadStatus,
                downloadStatus = downloadStatus
            )
        }
    }

    fun toText(conversationId: ConversationId, content: MessageContent): UIMessageContent.TextMessage {
        val messageTextContent = (content as? MessageContent.Text)

        val quotedMessage = messageTextContent?.quotedMessageDetails?.let { mapQuoteData(conversationId, it) }
            ?: if (messageTextContent?.quotedMessageReference?.quotedMessageId != null) {
                UIQuotedMessage.UnavailableData
            } else {
                null
            }

        return MessageBody(
            when (content) {
                is MessageContent.Text -> UIText.DynamicString(content.value, content.mentions)
                is MessageContent.Unknown -> content.typeName?.let {
                    UIText.StringResource(
                        messageResourceProvider.sentAMessageWithContent, it
                    )
                } ?: UIText.StringResource(R.string.sent_a_message_with_unknown_content)

                is MessageContent.FailedDecryption -> UIText.StringResource(R.string.label_message_decryption_failure_message)
                else -> UIText.StringResource(R.string.sent_a_message_with_unknown_content)
            },
            quotedMessage = quotedMessage
        ).let { messageBody -> UIMessageContent.TextMessage(messageBody = messageBody) }
    }

    private fun mapQuoteData(conversationId: ConversationId, it: MessageContent.QuotedMessageDetails) = UIQuotedMessage.UIQuotedData(
        it.messageId,
        it.senderId,
        it.senderName.orUnknownName(),
        UIText.StringResource(R.string.label_quote_original_message_date, isoFormatter.fromISO8601ToTimeFormat(it.timeInstant.toString())),
        it.editInstant?.let { instant ->
            UIText.StringResource(R.string.label_message_status_edited_with_date, isoFormatter.fromISO8601ToTimeFormat(instant.toString()))
        },
        when (val quotedContent = it.quotedContent) {
            is MessageContent.QuotedMessageDetails.Asset -> when (AttachmentType.fromMimeTypeString(quotedContent.assetMimeType)) {
                AttachmentType.IMAGE -> UIQuotedMessage.UIQuotedData.DisplayableImage(
                    ImageAsset.PrivateAsset(
                        wireSessionImageLoader,
                        conversationId,
                        it.messageId,
                        it.isQuotingSelfUser
                    )
                )

                AttachmentType.AUDIO -> UIQuotedMessage.UIQuotedData.AudioMessage
                AttachmentType.VIDEO,
                AttachmentType.GENERIC_FILE -> UIQuotedMessage.UIQuotedData.GenericAsset(
                    quotedContent.assetName,
                    quotedContent.assetMimeType
                )
            }

            is MessageContent.QuotedMessageDetails.Text -> UIQuotedMessage.UIQuotedData.Text(quotedContent.value)
            MessageContent.QuotedMessageDetails.Deleted -> UIQuotedMessage.UIQuotedData.Deleted
            MessageContent.QuotedMessageDetails.Invalid -> UIQuotedMessage.UIQuotedData.Invalid
        }
    )

    fun toUIMessageContent(assetMessageContentMetadata: AssetMessageContentMetadata, message: Message, sender: User?): UIMessageContent =
        with(assetMessageContentMetadata.assetMessageContent) {
            when {
                assetMessageContentMetadata.isDisplayableImage() && !assetMessageContentMetadata.assetMessageContent.hasValidRemoteData() ->
                    UIMessageContent.PreviewAssetMessage

                // If it's an image, we delegate the download it right away to coil
                assetMessageContentMetadata.isDisplayableImage() -> {
                    UIMessageContent.ImageMessage(
                        assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                        asset = ImageAsset.PrivateAsset(
                            wireSessionImageLoader,
                            message.conversationId,
                            message.id,
                            sender is SelfUser
                        ),
                        width = assetMessageContentMetadata.imgWidth,
                        height = assetMessageContentMetadata.imgHeight,
                        uploadStatus = uploadStatus,
                        downloadStatus = downloadStatus
                    )
                }

                // It's a generic Asset Message so let's not download it yet
                else -> {
                    UIMessageContent.AssetMessage(
                        assetName = name ?: "",
                        assetExtension = name?.split(".")?.last() ?: "",
                        assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                        assetSizeInBytes = sizeInBytes,
                        uploadStatus = uploadStatus,
                        downloadStatus = downloadStatus
                    )
                }
            }
        }

    private fun toRestrictedAsset(
        mimeType: String,
        assetSize: Long,
        assetName: String
    ): UIMessageContent {
        return UIMessageContent.RestrictedAsset(
            mimeType = mimeType,
            assetSizeInBytes = assetSize,
            assetName = assetName
        )
    }

    fun toSystemMessageMemberName(user: User?, type: SelfNameType = SelfNameType.NameOrDeleted): UIText = when (user) {
        is OtherUser -> user.name?.let { UIText.DynamicString(it) } ?: UIText.StringResource(messageResourceProvider.memberNameDeleted)
        is SelfUser -> when (type) {
            SelfNameType.ResourceLowercase -> UIText.StringResource(messageResourceProvider.memberNameYouLowercase)
            SelfNameType.ResourceTitleCase -> UIText.StringResource(messageResourceProvider.memberNameYouTitlecase)
            SelfNameType.NameOrDeleted -> user.name?.let { UIText.DynamicString(it) }
                ?: UIText.StringResource(messageResourceProvider.memberNameDeleted)
        }

        else -> UIText.StringResource(messageResourceProvider.memberNameDeleted)
    }

    // TODO: should we keep it here ?
    enum class SelfNameType {
        ResourceLowercase, ResourceTitleCase, NameOrDeleted
    }
}

class AssetMessageContentMetadata(val assetMessageContent: AssetContent) {
    val imgWidth
        get() = when (val md = assetMessageContent.metadata) {
            is AssetContent.AssetMetadata.Image -> md.width
            else -> 0
        }

    val imgHeight
        get() = when (val md = assetMessageContent.metadata) {
            is AssetContent.AssetMetadata.Image -> md.height
            else -> 0
        }

    fun isDisplayableImage(): Boolean = isDisplayableImageMimeType(assetMessageContent.mimeType) &&
            imgWidth.isGreaterThan(0) && imgHeight.isGreaterThan(0)
}

// TODO: should we keep it here ?
data class MessageResourceProvider(
    @StringRes val memberNameDeleted: Int = R.string.member_name_deleted_label,
    @StringRes val memberNameYouLowercase: Int = R.string.member_name_you_label_lowercase,
    @StringRes val memberNameYouTitlecase: Int = R.string.member_name_you_label_titlecase,
    @StringRes val sentAMessageWithContent: Int = R.string.sent_a_message_with_content
)

private fun String?.orUnknownName(): UIText = when {
    this != null -> UIText.DynamicString(this)
    else -> UIText.StringResource(R.string.username_unavailable_label)
}
