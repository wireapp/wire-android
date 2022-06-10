package com.wire.android.mapper

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent as UIMessageContent
import com.wire.android.ui.home.conversations.name
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent.Asset
import com.wire.kalium.logic.data.message.MessageContent.Text
import com.wire.kalium.logic.data.message.MessageContent.Server
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.Added
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.Removed
import com.wire.kalium.logic.data.message.MessageContent.MemberChange
import javax.inject.Inject


//TODO: splits mapping into more classes
class MessageContentMapper @Inject constructor(
    private val isoFormatter: ISOFormatter,
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
                    is Message.Client -> mapClientMessage(message)
                    is Message.Server -> mapServerMessage(message, members)
                }
            Message.Visibility.DELETED -> UIMessageContent.DeletedMessage
            Message.Visibility.HIDDEN -> null // we don't want to show hidden messages in any way
        }
    }

    private fun mapServerMessage(
        message: Message.Server,
        members: List<MemberDetails>
    ) = when (val content = message.content) {
        is MemberChange -> {
            val sender = members.findUser(userId = message.senderUserId)
            val isAuthorSelfAction = content.members.size == 1 && message.senderUserId == content.members.first().id
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
            when (content) {
                is Added -> UIMessageContent.ServerMessage.MemberAdded(
                    author = authorName,
                    memberNames = memberNameList
                )
                is Removed ->
                    if (isAuthorSelfAction) {
                        UIMessageContent.ServerMessage.MemberLeft(
                            author = authorName
                        )
                    } else {
                        UIMessageContent.ServerMessage.MemberRemoved(
                            author = authorName,
                            memberNames = memberNameList
                        )
                    }
            }
        }
    }

    private suspend fun mapClientMessage(
        message: Message.Client,
    ) = when (val content = message.content) {
        is Asset -> toAsset(
            conversationId = message.conversationId,
            messageId = message.id,
            assetContent = content.value
        )
        else -> toText(content, Message.EditStatus.NotEdited)
    }

    fun toText(
        content: MessageContent,
        editStatus: Message.EditStatus
    ) = MessageBody(when (content) {
            is MessageContent.Text -> UIText.DynamicString(content.value)
            is MessageContent.Unknown -> UIText.StringResource(
                messageResourceProvider.sentAMessageWithContent, content.typeName ?: content::javaClass.name
            )
            else -> UIText.StringResource(messageResourceProvider.sentAMessageWithContent, content::javaClass.name)
        }
    ).let { messageBody ->
        when (editStatus) {
            is Message.EditStatus.Edited -> {
                UIMessageContent.EditedMessage(
                    messageBody = messageBody,
                    editTimeStamp = isoFormatter.fromISO8601ToTimeFormat(utcISO = editStatus.lastTimeStamp)
                )
            }
            Message.EditStatus.NotEdited -> {
                UIMessageContent.TextMessage(messageBody = messageBody)
            }
        }
    }

    fun toServer(
        content: Server,
        senderUserId: UserId,
        members: List<MemberDetails>
    ): UIMessageContent = when (content) {
        is MemberChange -> {
            val sender = members.findUser(senderUserId)
            val isAuthorSelfAction = content.members.size == 1 && senderUserId == content.members.first().id
            val authorName = toSystemMessageMemberName(sender, SelfNameType.ResourceTitleCase)
            val memberNameList = content.members.map {
                toSystemMessageMemberName(members.findUser(it.id), SelfNameType.ResourceLowercase)
            }
            when (content) {
                is Added -> UIMessageContent.ServerMessage.MemberAdded(
                    authorName,
                    memberNameList
                )
                is Removed ->
                    if (isAuthorSelfAction) UIMessageContent.ServerMessage.MemberLeft(authorName)
                    else UIMessageContent.ServerMessage.MemberRemoved(authorName, memberNameList)
            }
        }
    }

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
                // If it's an image, we download it right away
                mimeType.contains("image") -> UIMessageContent.ImageMessage(
                    assetId = remoteData.assetId,
                    rawImgData = getRawAssetData(
                        conversationId = conversationId,
                        messageId = messageId
                    ),
                    width = imgWidth,
                    height = imgHeight
                )

                // It's a generic Asset Message so let's not download it yet
                else -> {
                    UIMessageContent.AssetMessage(
                        assetName = name ?: "",
                        assetExtension = name?.split(".")?.last() ?: "",
                        assetId = remoteData.assetId,
                        assetSizeInBytes = sizeInBytes,
                        downloadStatus = downloadStatus
                    )
                    // On the first asset message received, the asset ID is null, so we filter it out until the second updates it
                }
            }
        } else null
    }

    fun toSystemMessageMemberName(member: MemberDetails, type: SelfNameType = SelfNameType.NameOrDeleted): UIText = when (member) {
        is MemberDetails.Other -> member.name?.let { UIText.DynamicString(it) }
            ?: UIText.StringResource(messageResourceProvider.memberNameDeleted)
        is MemberDetails.Self -> when (type) {
            SelfNameType.ResourceLowercase -> UIText.StringResource(messageResourceProvider.memberNameYouLowercase)
            SelfNameType.ResourceTitleCase -> UIText.StringResource(messageResourceProvider.memberNameYouTitlecase)
            SelfNameType.NameOrDeleted -> member.name?.let { UIText.DynamicString(it) }
                ?: UIText.StringResource(messageResourceProvider.memberNameDeleted)
        }
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

    //TODO: should we keep it here ?
    enum class SelfNameType {
        ResourceLowercase, ResourceTitleCase, NameOrDeleted
    }
}


//TODO: should we keep it here ?
data class MessageResourceProvider(
    @StringRes val memberNameDeleted: Int = R.string.member_name_deleted_label,
    @StringRes val memberNameYouLowercase: Int = R.string.member_name_you_label_lowercase,
    @StringRes val memberNameYouTitlecase: Int = R.string.member_name_you_label_titlecase,
    @StringRes val sentAMessageWithContent: Int = R.string.sent_a_message_with_content
)

