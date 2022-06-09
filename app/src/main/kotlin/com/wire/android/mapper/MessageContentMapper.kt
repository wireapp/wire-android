package com.wire.android.mapper

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.name
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent.Asset
import com.wire.kalium.logic.data.message.MessageContent.Text
import com.wire.kalium.logic.data.message.MessageContent.Server
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import javax.inject.Inject

class MessageContentMapper @Inject constructor(
    private val isoFormatter: ISOFormatter,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val messageResourceProvider: MessageResourceProvider
) {

    enum class SelfNameType {
        ResourceLowercase, ResourceTitlecase, NameOrDeleted
    }

    suspend fun fromMessage(message: Message, members: List<MemberDetails>): MessageContent? {
        return when (message.visibility) {
            Message.Visibility.VISIBLE ->
                when (val content = message.content) {
                    is Asset -> toAsset(
                        conversationId = message.conversationId,
                        messageId = message.id,
                        assetContent = content.value
                    )
                    is Text -> toText(
                        content = content.value,
                        editStatus = message.editStatus
                    )
                    is Server -> toServer(
                        content = content,
                        senderUserId = message.senderUserId,
                        members = members
                    )
                    else -> toText()
                }
            Message.Visibility.DELETED -> MessageContent.DeletedMessage
            Message.Visibility.HIDDEN -> MessageContent.DeletedMessage
        }
    }

    private fun toText(content: String, editStatus: Message.EditStatus) =
        when (editStatus) {
            is Message.EditStatus.Edited -> {
                MessageContent.EditedMessage(
                    messageBody = MessageBody(message = UIText.DynamicString(content)),
                    editTimeStamp = isoFormatter.fromISO8601ToTimeFormat(utcISO = editStatus.lastTimeStamp)
                )
            }
            Message.EditStatus.NotEdited -> {
                MessageContent.TextMessage(
                    messageBody = MessageBody(message = UIText.DynamicString(content))
                )
            }
        }

    fun toServer(
        content: Server,
        senderUserId: UserId,
        members: List<MemberDetails>
    ): MessageContent = when (content) {
        is com.wire.kalium.logic.data.message.MessageContent.MemberChange -> {
            val sender = members.findUser(senderUserId)
            val isAuthorSelfAction = content.members.size == 1 && senderUserId == content.members.first().id
            val authorName = toSystemMessageMemberName(sender, SelfNameType.ResourceTitlecase)
            val memberNameList = content.members.map {
                toSystemMessageMemberName(members.findUser(it.id), SelfNameType.ResourceLowercase)
            }
            when (content) {
                is com.wire.kalium.logic.data.message.MessageContent.MemberChange.Added -> MessageContent.ServerMessage.MemberAdded(
                    authorName,
                    memberNameList
                )
                is com.wire.kalium.logic.data.message.MessageContent.MemberChange.Removed ->
                    if (isAuthorSelfAction) MessageContent.ServerMessage.MemberLeft(authorName)
                    else MessageContent.ServerMessage.MemberRemoved(authorName, memberNameList)
            }
        }
    }

    private suspend fun toAsset(
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
                mimeType.contains("image") -> MessageContent.ImageMessage(
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
                    MessageContent.AssetMessage(
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
            SelfNameType.ResourceTitlecase -> UIText.StringResource(messageResourceProvider.memberNameYouTitlecase)
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
}

data class MessageResourceProvider(
    @StringRes val memberNameDeleted: Int = R.string.member_name_deleted_label,
    @StringRes val memberNameYouLowercase: Int = R.string.member_name_you_label_lowercase,
    @StringRes val memberNameYouTitlecase: Int = R.string.member_name_you_label_titlecase,
    @StringRes val contentNotAvailable: Int = R.string.content_is_not_available
)

