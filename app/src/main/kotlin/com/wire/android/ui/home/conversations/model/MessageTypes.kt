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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.messagetypes.asset.MessageAsset
import com.wire.android.ui.home.conversations.model.messagetypes.image.DisplayableImageMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageFailed
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageInProgress
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImportedImageMessage
import com.wire.android.ui.markdown.DisplayMention
import com.wire.android.ui.markdown.MarkdownConsts.MENTION_MARK
import com.wire.android.ui.markdown.MarkdownDocument
import com.wire.android.ui.markdown.NodeData
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.Message.DownloadStatus.DOWNLOAD_IN_PROGRESS
import com.wire.kalium.logic.data.message.Message.DownloadStatus.FAILED_DOWNLOAD
import com.wire.kalium.logic.data.message.Message.UploadStatus.FAILED_UPLOAD
import com.wire.kalium.logic.data.message.Message.UploadStatus.UPLOAD_IN_PROGRESS
import org.commonmark.Extension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Document
import org.commonmark.parser.Parser

// TODO: Here we actually need to implement some logic that will distinguish MentionLabel with Body of the message,
//       waiting for the backend to implement mapping logic for the MessageBody
@Composable
internal fun MessageBody(
    messageBody: MessageBody,
    isAvailable: Boolean,
    onLongClick: (() -> Unit)? = null,
    onOpenProfile: (String) -> Unit,
    onLinkClick: (String) -> Unit
) {
    val (displayMentions, text) = mapToDisplayMentions(messageBody.message, LocalContext.current.resources)

    val nodeData = NodeData(
        modifier = Modifier.defaultMinSize(minHeight = dimensions().spacing20x),
        color = if (isAvailable) MaterialTheme.colorScheme.onBackground else MaterialTheme.wireColorScheme.secondaryText,
        style = MaterialTheme.wireTypography.body01,
        colorScheme = MaterialTheme.wireColorScheme,
        typography = MaterialTheme.wireTypography,
        mentions = displayMentions,
        onLongClick = onLongClick,
        onOpenProfile = onOpenProfile,
        onLinkClick = onLinkClick
    )

    val extensions: List<Extension> = listOf(
        StrikethroughExtension.builder().requireTwoTildes(true).build(),
        TablesExtension.create()
    )

    MarkdownDocument(Parser.builder().extensions(extensions).build().parse(text) as Document, nodeData)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageImage(
    asset: ImageAsset?,
    imgParams: ImageMessageParams,
    uploadStatus: Message.UploadStatus,
    downloadStatus: Message.DownloadStatus,
    onImageClick: Clickable,
    shouldFillMaxWidth: Boolean = false,
    isImportedMediaAsset: Boolean = false
) {
    Box(
        Modifier
            .padding(top = MaterialTheme.wireDimensions.spacing4x)
            .clip(shape = RoundedCornerShape(dimensions().messageAssetBorderRadius))
            .background(
                color = MaterialTheme.wireColorScheme.onPrimary, shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .wrapContentSize()
            .combinedClickable(
                enabled = onImageClick.enabled,
                onClick = onImageClick.onClick,
                onLongClick = onImageClick.onLongClick,
            )
    ) {
        when {
            // Trying to upload the asset
            uploadStatus == UPLOAD_IN_PROGRESS || downloadStatus == DOWNLOAD_IN_PROGRESS -> {
                ImageMessageInProgress(imgParams, downloadStatus == DOWNLOAD_IN_PROGRESS)
            }

            asset != null -> {
                if (isImportedMediaAsset) ImportedImageMessage(asset, shouldFillMaxWidth)
                else DisplayableImageMessage(asset, imgParams)
            }

            // Show error placeholder
            uploadStatus == FAILED_UPLOAD || downloadStatus == FAILED_DOWNLOAD -> {
                ImageMessageFailed(imgParams, downloadStatus == FAILED_DOWNLOAD)
            }
        }
    }
}

@Composable
internal fun MessageGenericAsset(
    assetName: String,
    assetExtension: String,
    assetSizeInBytes: Long,
    onAssetClick: Clickable,
    assetUploadStatus: Message.UploadStatus,
    assetDownloadStatus: Message.DownloadStatus,
    shouldFillMaxWidth: Boolean = true,
    isImportedMediaAsset: Boolean = false
) {
    MessageAsset(
        assetName,
        assetExtension,
        assetSizeInBytes,
        onAssetClick,
        assetUploadStatus,
        assetDownloadStatus,
        shouldFillMaxWidth,
        isImportedMediaAsset
    )
}

/**
 * Maps all mentions to DisplayMention in order to find them easier after converting
 * to markdown document as positions changes due to markdown characters.
 *
 * @param uiText: UIText - Message to be displayed as UIText
 * @param resources: Resources - To be able to get String out of UIText message
 * @return Pair<List<DisplayMention>, String>
 */
private fun mapToDisplayMentions(uiText: UIText, resources: Resources): Pair<List<DisplayMention>, String> {
    return if (uiText is UIText.DynamicString) {
        val stringBuilder: StringBuilder = StringBuilder(uiText.value)
        val mentions = uiText.mentions
            .filter { it.start >= 0 && it.length > 0 }
            .sortedBy { it.start }
            .reversed()
        val mentionList = mentions.mapNotNull { mention ->
            // secured crash for mentions caused by web when text without mentions contains mention data
            if (mention.start + mention.length <= uiText.value.length && uiText.value.elementAt(mention.start) == '@') {
                val mentionName = uiText.value.substring(mention.start, mention.start + mention.length)
                stringBuilder.insert(mention.start + mention.length, MENTION_MARK)
                stringBuilder.insert(mention.start, MENTION_MARK)
                DisplayMention(
                    mention.userId,
                    mention.length,
                    mention.isSelfMention,
                    mentionName
                )
            } else {
                null
            }
        }.reversed()
        Pair(mentionList, stringBuilder.toString())
    } else {
        Pair(listOf(), uiText.asString(resources))
    }
}
