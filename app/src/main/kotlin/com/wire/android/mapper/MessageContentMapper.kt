package com.wire.android.mapper

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.QuotedMessageUIData
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.asset.isDisplayableImageMimeType
import com.wire.kalium.logic.data.conversation.UnreadEventCount
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.MessageContent.Asset
import com.wire.kalium.logic.data.message.MessageContent.MemberChange
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.Added
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.Removed
import com.wire.kalium.logic.data.message.UnreadEventType
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.sync.receiver.conversation.message.hasValidRemoteData
import com.wire.kalium.logic.util.isGreaterThan
import javax.inject.Inject

// TODO: splits mapping into more classes
class MessageContentMapper @Inject constructor(
    private val messageResourceProvider: MessageResourceProvider,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val isoFormatter: ISOFormatter,
) {

    fun fromMessage(
        message: Message.Standalone,
        userList: List<User>
    ): UIMessageContent? {
        return when (message.visibility) {
            Message.Visibility.VISIBLE ->
                return when (message) {
                    is Message.Regular -> mapRegularMessage(message, userList.findUser(message.senderUserId))
                    is Message.System -> mapSystemMessage(message, userList)
                }

            Message.Visibility.DELETED, // for deleted, there is a state label displayed only
            Message.Visibility.HIDDEN -> null // we don't want to show hidden nor deleted message content in any way
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
                    null // we don't want to show "You added you to the conversation"
                } else {
                    UIMessageContent.SystemMessage.MemberAdded(author = authorName, memberNames = memberNameList)
                }

            is Removed ->
                if (isAuthorSelfAction) {
                    UIMessageContent.SystemMessage.MemberLeft(author = authorName)
                } else {
                    UIMessageContent.SystemMessage.MemberRemoved(author = authorName, memberNames = memberNameList)
                }
        }
    }

    private fun mapRegularMessage(
        message: Message.Regular,
        sender: User?
    ) = when (val content = message.content) {
        is Asset -> {
            val assetMessageContentMetadata = AssetMessageContentMetadata(content.value)
            toUIMessageContent(assetMessageContentMetadata, message, sender)
        }

        is MessageContent.RestrictedAsset -> toRestrictedAsset(content.mimeType, content.sizeInBytes, content.name)
        else -> toText(message.conversationId, content)
    }

    fun toText(conversationId: ConversationId, content: MessageContent) = MessageBody(
        when (content) {
            is MessageContent.Text -> UIText.DynamicString(content.value, content.mentions)
            is MessageContent.Unknown -> UIText.StringResource(
                messageResourceProvider.sentAMessageWithContent, content.typeName ?: "Unknown"
            )

            is MessageContent.FailedDecryption -> UIText.StringResource(R.string.label_message_decryption_failure_message)
            else -> UIText.StringResource(messageResourceProvider.sentAMessageWithContent, "Unknown")
        },
        quotedMessage = (content as? MessageContent.Text)?.quotedMessageDetails?.let { mapQuoteData(conversationId, it) }
    ).let { messageBody -> UIMessageContent.TextMessage(messageBody = messageBody) }

    private fun mapQuoteData(conversationId: ConversationId, it: MessageContent.QuotedMessageDetails) = QuotedMessageUIData(
        it.messageId,
        it.senderId,
        it.senderName,
        UIText.StringResource(R.string.label_quote_original_message_date, isoFormatter.fromISO8601ToTimeFormat(it.timeInstant.toString())),
        it.editInstant?.let { instant ->
            UIText.StringResource(R.string.label_message_status_edited_with_date, isoFormatter.fromISO8601ToTimeFormat(instant.toString()))
        },
        when (val quotedContent = it.quotedContent) {
            is MessageContent.QuotedMessageDetails.Asset -> when (AttachmentType.fromMimeTypeString(quotedContent.assetMimeType)) {
                AttachmentType.IMAGE -> QuotedMessageUIData.DisplayableImage(
                    ImageAsset.PrivateAsset(
                        wireSessionImageLoader,
                        conversationId,
                        it.messageId,
                        it.isQuotingSelfUser
                    )
                )

                AttachmentType.GENERIC_FILE -> QuotedMessageUIData.GenericAsset(
                    quotedContent.assetName,
                    quotedContent.assetMimeType
                )
            }

            is MessageContent.QuotedMessageDetails.Text -> QuotedMessageUIData.Text(quotedContent.value)
            MessageContent.QuotedMessageDetails.Deleted -> QuotedMessageUIData.Deleted
            MessageContent.QuotedMessageDetails.Invalid -> QuotedMessageUIData.Invalid
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

@Suppress("ReturnCount")
fun Message.Standalone?.toUIPreview(
    unreadEventCount: UnreadEventCount,
    unreadMentionsCount: Long,
): UILastMessageContent {
    if (this == null) {
        return UILastMessageContent.None
    }

    val sortedUnreadContent = unreadEventCount
        .plus(if (unreadMentionsCount > 0) mapOf(Pair(UnreadEventType.MENTION, unreadMentionsCount.toInt())) else mapOf())
        .toSortedMap()

    // we want to show last text message content instead of counter when there are only unread text messages
    if (!(sortedUnreadContent.size == 1 && sortedUnreadContent.contains(UnreadEventType.MESSAGE))) {
        val unreadContentTexts = sortedUnreadContent
            .mapNotNull { type ->
                when (type.key) {
                    UnreadEventType.KNOCK -> UIText.PluralResource(R.plurals.unread_event_knock, type.value, type.value)
                    UnreadEventType.MISSED_CALL -> UIText.PluralResource(R.plurals.unread_event_call, type.value, type.value)
                    UnreadEventType.MENTION -> UIText.PluralResource(R.plurals.unread_event_mention, type.value, type.value)
                    // TODO we need to decrease number of text messages by mentions count because currently they contain them
                    UnreadEventType.MESSAGE -> {
                        val messagesWithoutMentions = type.value - unreadMentionsCount.toInt()
                        if (messagesWithoutMentions > 0) {
                            UIText.PluralResource(
                                R.plurals.unread_event_message,
                                messagesWithoutMentions,
                                messagesWithoutMentions
                            )
                        } else {
                            null
                        }
                    }
                    UnreadEventType.IGNORED -> null
                    null -> null
                }
            }
        if (unreadContentTexts.size > 1) {
            val first = unreadContentTexts.first()
            val second = unreadContentTexts.elementAt(1)
            return UILastMessageContent.MultipleMessage(first, second)
        } else if (unreadContentTexts.isNotEmpty()) {
            return UILastMessageContent.TextMessage(MessageBody(unreadContentTexts.first()))
        }
    }

    return uiLastMessageContent()
}

@Suppress("LongMethod", "ComplexMethod")
private fun Message.Standalone.uiLastMessageContent(): UILastMessageContent {
    val senderUIText = when(this) {
        is Message.Regular -> when {
            isSelfMessage -> UIText.StringResource(R.string.member_name_you_label_titlecase)
            senderUserName != null -> UIText.DynamicString(senderUserName!!)
            else -> UIText.StringResource(R.string.username_unavailable_label)
        }

        is Message.System -> when {
            senderUserName != null -> UIText.DynamicString(senderUserName!!)
            else -> UIText.StringResource(R.string.username_unavailable_label)
        }
    }

    return when (this) {
        is Message.Regular -> when (content) {
            is Asset ->
                when ((content as Asset).value.metadata) {
                    is AssetContent.AssetMetadata.Audio ->
                        UILastMessageContent.SenderWithMessage(senderUIText, UIText.StringResource(R.string.last_message_audio))
                    is AssetContent.AssetMetadata.Image ->
                        UILastMessageContent.SenderWithMessage(senderUIText, UIText.StringResource(R.string.last_message_image))
                    is AssetContent.AssetMetadata.Video ->
                        UILastMessageContent.SenderWithMessage(senderUIText, UIText.StringResource(R.string.last_message_video))
                    null ->
                        UILastMessageContent.SenderWithMessage(senderUIText, UIText.StringResource(R.string.last_message_asset))
                }
            is MessageContent.FailedDecryption -> UILastMessageContent.None
            is MessageContent.Knock -> UILastMessageContent.SenderWithMessage(
                senderUIText,
                UIText.StringResource(R.string.last_message_knock)
            )
            is MessageContent.RestrictedAsset ->
                UILastMessageContent.SenderWithMessage(senderUIText, UIText.StringResource(R.string.last_message_asset))
            is MessageContent.Text -> UILastMessageContent.SenderWithMessage(
                sender = senderUIText,
                message = UIText.DynamicString((content as MessageContent.Text).value),
                separator = ": "
            )
            is MessageContent.Unknown -> UILastMessageContent.None
        }
        is Message.System -> when (content) {
            is MessageContent.ConversationRenamed -> UILastMessageContent.SenderWithMessage(
                senderUIText,
                UIText.StringResource(R.string.last_message_change_conversation_name)
            )
            is Added -> UILastMessageContent.SenderWithMessage(
                senderUIText,
                UIText.PluralResource(
                    R.plurals.last_message_people_added,
                    (content as Added).members.size,
                    (content as Added).members.size
                )
            )
            is Removed -> UILastMessageContent.SenderWithMessage(
                senderUIText,
                UIText.PluralResource(
                    R.plurals.last_message_people_removed,
                    (content as Removed).members.size,
                    (content as Removed).members.size
                )
            )
            MessageContent.MissedCall -> UILastMessageContent.SenderWithMessage(
                senderUIText,
                UIText.StringResource(R.string.last_message_call)
            )
            is MessageContent.TeamMemberRemoved -> UILastMessageContent.None
        }
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
