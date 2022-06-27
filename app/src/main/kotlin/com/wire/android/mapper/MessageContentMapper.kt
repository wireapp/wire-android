package com.wire.android.mapper

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.name
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.MessageContent.Asset
import com.wire.kalium.logic.data.message.MessageContent.MemberChange
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.Added
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.Removed
import com.wire.kalium.logic.data.user.AssetId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import javax.inject.Inject
import com.wire.android.ui.home.conversations.model.MessageContent as UIMessageContent

// TODO: splits mapping into more classes
class MessageContentMapper @Inject constructor(
    private val getMessageAsset: GetMessageAssetUseCase,
    private val messageResourceProvider: MessageResourceProvider
) {

    suspend fun fromMessage(
        message: Message,
        members: List<MemberDetails>
    ): UIMessageContent? {
        return when (message.visibility) {
            Message.Visibility.VISIBLE ->
                return when (message) {
                    is Message.Regular -> mapRegularMessage(message)
                    is Message.System -> mapSystemMessage(message, members)
                }
            Message.Visibility.DELETED, // for deleted, there is a state label displayed only
            Message.Visibility.HIDDEN -> null // we don't want to show hidden nor deleted message content in any way
        }
    }

    private fun mapSystemMessage(
        message: Message.System,
        members: List<MemberDetails>
    ) = when (val content = message.content) {
        is MemberChange -> mapMemberChangeMessage(content, message.senderUserId, members)
    }

    fun mapMemberChangeMessage(
        content: MessageContent.MemberChange,
        senderUserId: UserId,
        members: List<MemberDetails>
    ): UIMessageContent.SystemMessage? {
        val sender = members.findUser(userId = senderUserId)
        val isAuthorSelfAction = content.members.size == 1 && senderUserId == content.members.first().id
        val authorName = toSystemMessageMemberName(
            member = sender,
            type = SelfNameType.ResourceTitleCase
        )
        val memberNameList = content.members.map {
            toSystemMessageMemberName(
                member = members.findUser(userId = it.id),
                type = SelfNameType.ResourceLowercase
            )
        }
        return when (content) {
            is Added ->
                if (isAuthorSelfAction) {
                    null // we don't want to show "You added you to the conversation"
                } else {
                    UIMessageContent.SystemMessage.MemberAdded(
                        author = authorName,
                        memberNames = memberNameList
                    )
                }
            is Removed ->
                if (isAuthorSelfAction) {
                    UIMessageContent.SystemMessage.MemberLeft(
                        author = authorName
                    )
                } else {
                    UIMessageContent.SystemMessage.MemberRemoved(
                        author = authorName,
                        memberNames = memberNameList
                    )
                }
        }
    }

    private suspend fun mapRegularMessage(
        message: Message.Regular,
    ) = when (val content = message.content) {
        is Asset -> toAsset(
            conversationId = message.conversationId,
            messageId = message.id,
            assetContent = content.value
        )
        is MessageContent.RestrictedAsset -> toRestrictedAsset(content.mimeType)
        else -> toText(content)
    }

    fun toText(
        content: MessageContent
    ) = MessageBody(
        when (content) {
            is MessageContent.Text -> UIText.DynamicString(content.value)
            is MessageContent.Unknown -> UIText.StringResource(
                messageResourceProvider.sentAMessageWithContent, content.typeName ?: "Unknown"
            )
            else -> UIText.StringResource(messageResourceProvider.sentAMessageWithContent, "Unknown")
        }
    ).let { messageBody -> UIMessageContent.TextMessage(messageBody = messageBody) }

    suspend fun toAsset(
        conversationId: QualifiedID,
        messageId: String,
        assetContent: AssetContent
    ) = with(assetContent) {
        val (imgWidth, imgHeight) = when (val md = metadata) {
            is AssetContent.AssetMetadata.Image -> md.width to md.height
            else -> 0 to 0
        }
        if (remoteData.assetId.isNotEmpty()) {
            when {
                // If it's an image and has valid width and height, we download it right away
                isImage(mimeType) && imgWidth > 0 && imgHeight > 0 -> {
                    val rawData = getRawAssetData(
                        conversationId = conversationId,
                        messageId = messageId
                    )
                    UIMessageContent.ImageMessage(
                        assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                        rawImgData = rawData,
                        width = imgWidth,
                        height = imgHeight
                    )
                }

                // It's a generic Asset Message so let's not download it yet
                else -> {
                    UIMessageContent.AssetMessage(
                        assetName = name ?: "",
                        assetExtension = name?.split(".")?.last() ?: "",
                        assetId = AssetId(remoteData.assetId, remoteData.assetDomain.orEmpty()),
                        assetSizeInBytes = sizeInBytes,
                        downloadStatus = downloadStatus
                    )
                    // On the first asset message received, the asset ID is null, so we filter it out until the second updates it
                }
            }
        } else null
    }

    private fun toRestrictedAsset(
        mimeType: String
    ): UIMessageContent {
        return UIMessageContent.RestrictedAsset(mimeType)
    }

    fun toSystemMessageMemberName(member: MemberDetails?, type: SelfNameType = SelfNameType.NameOrDeleted): UIText = when (member) {
        is MemberDetails.Other -> member.name?.let { UIText.DynamicString(it) }
            ?: UIText.StringResource(messageResourceProvider.memberNameDeleted)
        is MemberDetails.Self -> when (type) {
            SelfNameType.ResourceLowercase -> UIText.StringResource(messageResourceProvider.memberNameYouLowercase)
            SelfNameType.ResourceTitleCase -> UIText.StringResource(messageResourceProvider.memberNameYouTitlecase)
            SelfNameType.NameOrDeleted -> member.name?.let { UIText.DynamicString(it) }
                ?: UIText.StringResource(messageResourceProvider.memberNameDeleted)
        }
        else -> UIText.StringResource(messageResourceProvider.memberNameDeleted)
    }

    private suspend fun getRawAssetData(conversationId: QualifiedID, messageId: String) =
        getMessageAsset(
            conversationId = conversationId,
            messageId = messageId
        ).run {
            when (this) {
                is MessageAssetResult.Success -> decodedAsset
                else -> null
            }
        }

    // TODO: should we keep it here ?
    enum class SelfNameType {
        ResourceLowercase, ResourceTitleCase, NameOrDeleted
    }
}

// TODO: should we keep it here ?
data class MessageResourceProvider(
    @StringRes val memberNameDeleted: Int = R.string.member_name_deleted_label,
    @StringRes val memberNameYouLowercase: Int = R.string.member_name_you_label_lowercase,
    @StringRes val memberNameYouTitlecase: Int = R.string.member_name_you_label_titlecase,
    @StringRes val sentAMessageWithContent: Int = R.string.sent_a_message_with_content
)
