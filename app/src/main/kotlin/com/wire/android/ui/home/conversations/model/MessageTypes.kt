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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.CompositeMessageViewModel
import com.wire.android.ui.home.conversations.CompositeMessageViewModelImpl
import com.wire.android.ui.home.conversations.model.messagetypes.asset.MessageAsset
import com.wire.android.ui.home.conversations.model.messagetypes.image.AsyncImageMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.DisplayableImageMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageFailed
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageInProgress
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImportedImageMessage
import com.wire.android.ui.markdown.DisplayMention
import com.wire.android.ui.markdown.MarkdownConstants.MENTION_MARK
import com.wire.android.ui.markdown.MarkdownDocument
import com.wire.android.ui.markdown.NodeData
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.asset.AssetTransferStatus.DOWNLOAD_IN_PROGRESS
import com.wire.kalium.logic.data.asset.AssetTransferStatus.FAILED_DOWNLOAD
import com.wire.kalium.logic.data.asset.AssetTransferStatus.NOT_FOUND
import com.wire.kalium.logic.data.asset.AssetTransferStatus.FAILED_UPLOAD
import com.wire.kalium.logic.data.asset.AssetTransferStatus.UPLOAD_IN_PROGRESS
import okio.Path
import org.commonmark.Extension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Document
import org.commonmark.parser.Parser

// TODO: Here we actually need to implement some logic that will distinguish MentionLabel with Body of the message,
//       waiting for the backend to implement mapping logic for the MessageBody
@Composable
internal fun MessageBody(
    messageId: String,
    messageBody: MessageBody?,
    isAvailable: Boolean,
    searchQuery: String = "",
    onLongClick: (() -> Unit)? = null,
    onOpenProfile: (String) -> Unit,
    buttonList: List<MessageButton>?,
    onLinkClick: (String) -> Unit,
    clickable: Boolean = true
) {
    val (displayMentions, text) = messageBody?.message?.let {
        mapToDisplayMentions(it, LocalContext.current.resources)
    } ?: Pair(emptyList(), null)

    val nodeData = NodeData(
        modifier = Modifier.defaultMinSize(minHeight = dimensions().spacing20x),
        color = if (isAvailable) MaterialTheme.colorScheme.onBackground else MaterialTheme.wireColorScheme.secondaryText,
        style = MaterialTheme.wireTypography.body01,
        colorScheme = MaterialTheme.wireColorScheme,
        typography = MaterialTheme.wireTypography,
        searchQuery = searchQuery,
        mentions = displayMentions,
        onLongClick = onLongClick,
        onOpenProfile = onOpenProfile,
        onLinkClick = onLinkClick
    )

    val extensions: List<Extension> = listOf(
        StrikethroughExtension.builder().requireTwoTildes(true).build(),
        TablesExtension.create()
    )
    text?.also {
        MarkdownDocument(
            Parser.builder().extensions(extensions).build().parse(it) as Document,
            nodeData,
            clickable
        )
    }
    buttonList?.also {
        MessageButtonsContent(
            messageId = messageId,
            buttonList = it,
        )
    }
}

@Composable
fun MessageButtonsContent(
    messageId: String,
    buttonList: List<MessageButton>,
    viewModel: CompositeMessageViewModel =
        hiltViewModelScoped<CompositeMessageViewModelImpl, CompositeMessageViewModel, CompositeMessageArgs>(
            CompositeMessageArgs(messageId)
        )
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
    ) {
        for (index in buttonList.indices) {
            val button = buttonList[index]
            val onCLick = remember(button.isSelected) {
                if (!button.isSelected) {
                    { viewModel.sendButtonActionMessage(button.id) }
                } else {
                    { }
                }
            }

            val isPending = viewModel.pendingButtonId == button.id

            val state = if (button.isSelected) WireButtonState.Selected
            else if (viewModel.pendingButtonId != null) WireButtonState.Disabled
            else WireButtonState.Default

            WireSecondaryButton(
                loading = isPending,
                text = button.text,
                onClick = onCLick,
                state = state
            )
            if (index != buttonList.lastIndex) {
                Spacer(modifier = Modifier.padding(top = dimensions().spacing8x))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageImage(
    asset: ImageAsset?,
    imgParams: ImageMessageParams,
    transferStatus: AssetTransferStatus,
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
                width = dimensions().spacing1x,
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
        // TODO Kubaz make progress in box, but then remember to not load image with isIncompleteImage
        when {
            // Trying to upload the asset
            transferStatus == UPLOAD_IN_PROGRESS || transferStatus == DOWNLOAD_IN_PROGRESS -> {
                ImageMessageInProgress(
                    imgParams.normalizedWidth, imgParams.normalizedHeight,
                    transferStatus == DOWNLOAD_IN_PROGRESS
                )
            }

            transferStatus == NOT_FOUND -> {
                ImageMessageFailed(
                    imgParams.normalizedWidth, imgParams.normalizedHeight,
                    true
                )
            }

            asset != null -> {
                if (isImportedMediaAsset) ImportedImageMessage(asset, shouldFillMaxWidth)
                else DisplayableImageMessage(asset, imgParams.normalizedWidth, imgParams.normalizedHeight)
            }

            // Show error placeholder
            transferStatus == FAILED_UPLOAD || transferStatus == FAILED_DOWNLOAD -> {
                ImageMessageFailed(
                    imgParams.normalizedWidth, imgParams.normalizedHeight,
                    transferStatus == FAILED_DOWNLOAD
                )
            }
        }
    }
}

@Composable
fun MediaAssetImage(
    asset: ImageAsset?,
    width: Dp,
    height: Dp,
    transferStatus: AssetTransferStatus?,
    assetPath: Path? = null,
    onImageClick: Clickable
) {
    Box(
        Modifier
            .padding(top = MaterialTheme.wireDimensions.spacing2x)
            .clip(shape = RoundedCornerShape(dimensions().messageAssetBorderRadius))
            .background(
                color = MaterialTheme.wireColorScheme.onPrimary, shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .border(
                width = dimensions().spacing1x,
                color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .wrapContentSize()
            .clickable(onImageClick)
    ) {
        when {
            // Trying to upload the asset
            transferStatus == DOWNLOAD_IN_PROGRESS -> {
                ImageMessageInProgress(
                    width = width,
                    height = height,
                    isDownloading = true,
                    showText = false
                )
            }

            assetPath != null -> {
                AsyncImageMessage(assetPath, width, height)
            }

            asset != null -> {
                DisplayableImageMessage(asset, width, height)
            }

            // Show error placeholder
            transferStatus == FAILED_DOWNLOAD -> {
                ImageMessageFailed(
                    width = width,
                    height = height,
                    isDownloadFailure = true
                )
            }

            transferStatus == NOT_FOUND -> {
                ImageMessageFailed(
                    width = width,
                    height = height,
                    isDownloadFailure = true
                )
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
    assetTransferStatus: AssetTransferStatus,
    shouldFillMaxWidth: Boolean = true,
    isImportedMediaAsset: Boolean = false
) {
    MessageAsset(
        assetName,
        assetExtension,
        assetSizeInBytes,
        onAssetClick,
        assetTransferStatus,
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
fun mapToDisplayMentions(uiText: UIText, resources: Resources): Pair<List<DisplayMention>, String> {
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
