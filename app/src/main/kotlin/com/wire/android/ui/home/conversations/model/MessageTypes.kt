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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.DpSize
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.CompositeMessageViewModel
import com.wire.android.ui.home.conversations.CompositeMessageViewModelImpl
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.error
import com.wire.android.ui.home.conversations.messages.item.highlighted
import com.wire.android.ui.home.conversations.messages.item.isBubble
import com.wire.android.ui.home.conversations.messages.item.textColor
import com.wire.android.ui.home.conversations.mock.mockedPrivateAsset
import com.wire.android.ui.home.conversations.model.messagetypes.image.AsyncImageMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.DisplayableImageMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageFailed
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageInProgress
import com.wire.android.ui.home.conversations.model.messagetypes.image.VisualMediaParams
import com.wire.android.ui.home.conversations.model.messagetypes.image.size
import com.wire.android.ui.markdown.DisplayMention
import com.wire.android.ui.markdown.MarkdownConstants.MENTION_MARK
import com.wire.android.ui.markdown.MarkdownDocument
import com.wire.android.ui.markdown.MessageColors
import com.wire.android.ui.markdown.NodeActions
import com.wire.android.ui.markdown.NodeData
import com.wire.android.ui.markdown.toMarkdownDocument
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.AssetTransferStatus.DOWNLOAD_IN_PROGRESS
import com.wire.kalium.logic.data.asset.AssetTransferStatus.FAILED_DOWNLOAD
import com.wire.kalium.logic.data.asset.AssetTransferStatus.FAILED_UPLOAD
import com.wire.kalium.logic.data.asset.AssetTransferStatus.NOT_FOUND
import com.wire.kalium.logic.data.asset.AssetTransferStatus.UPLOAD_IN_PROGRESS
import com.wire.kalium.logic.data.asset.isSaved
import kotlinx.collections.immutable.PersistentList
import okio.Path

// TODO: Here we actually need to implement some logic that will distinguish MentionLabel with Body of the message,
//       waiting for the backend to implement mapping logic for the MessageBody
@Composable
internal fun MessageBody(
    messageId: String,
    messageBody: MessageBody?,
    isAvailable: Boolean,
    onOpenProfile: (String) -> Unit,
    buttonList: PersistentList<MessageButton>?,
    onLinkClick: (String) -> Unit,
    searchQuery: String = "",
    clickable: Boolean = true,
    messageStyle: MessageStyle = MessageStyle.NORMAL
) {
    val (displayMentions, text) = messageBody?.message?.let {
        mapToDisplayMentions(it, LocalContext.current.resources)
    } ?: Pair(emptyList(), null)

    val color = when (messageStyle) {
        MessageStyle.BUBBLE_SELF -> MaterialTheme.wireColorScheme.onPrimary
        MessageStyle.BUBBLE_OTHER -> when {
            isAvailable -> MaterialTheme.colorScheme.onBackground
            else -> MaterialTheme.wireColorScheme.secondaryText
        }

        MessageStyle.NORMAL -> {
            when {
                isAvailable -> MaterialTheme.colorScheme.onBackground
                else -> MaterialTheme.wireColorScheme.secondaryText
            }
        }
    }

    val nodeData = NodeData(
        modifier = Modifier.defaultMinSize(minHeight = dimensions().spacing20x),
        color = color,
        style = MaterialTheme.wireTypography.body01,
        colorScheme = MaterialTheme.wireColorScheme,
        typography = MaterialTheme.wireTypography,
        searchQuery = searchQuery,
        mentions = displayMentions,
        actions = NodeActions(
            onOpenProfile = onOpenProfile,
            onLinkClick = onLinkClick
        ),
        messageStyle = messageStyle,
        messageColors = MessageColors(highlighted = messageStyle.highlighted())
    )

    val markdownDocument = remember(text) {
        text?.toMarkdownDocument()
    }

    markdownDocument?.also {
        MarkdownDocument(
            it,
            nodeData,
            clickable
        )
    }
    buttonList?.also {
        VerticalSpace.x4()
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
    modifier: Modifier = Modifier,
    viewModel: CompositeMessageViewModel =
        hiltViewModelScoped<CompositeMessageViewModelImpl, CompositeMessageViewModel, CompositeMessageArgs>(
            CompositeMessageArgs(messageId)
        )
) {
    Column(
        modifier = modifier
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
    asset: ImageAsset.Remote?,
    imgParams: VisualMediaParams,
    messageStyle: MessageStyle,
    transferStatus: AssetTransferStatus,
    onImageClick: Clickable,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .applyIf(!messageStyle.isBubble()) {
                padding(top = MaterialTheme.wireDimensions.spacing4x)
                    .clip(shape = RoundedCornerShape(dimensions().messageAssetBorderRadius))
                    .background(
                        color = MaterialTheme.wireColorScheme.onPrimary,
                        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
                    )
                    .border(
                        width = dimensions().spacing1x,
                        color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
                        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
                    )
            }
            .wrapContentSize()
            .combinedClickable(
                enabled = onImageClick.enabled,
                onClick = onImageClick.onClick,
                onLongClick = onImageClick.onLongClick,
            ),
        contentAlignment = Alignment.Center
    ) {
        val alignCenterModifier = Modifier.align(Alignment.Center)
        asset?.let {
            DisplayableImageMessage(
                imageData = it,
                size = imgParams.normalizedSize().size(),
                messageStyle = messageStyle,
                modifier = alignCenterModifier
            )
        }

        val shouldAddScrimBg = asset != null && transferStatus.isSaved()
        Box(
            Modifier
                .applyIf(shouldAddScrimBg) {
                    background(colorsScheme().scrim)
                },
            contentAlignment = Alignment.Center
        ) {
            when (transferStatus) {
                UPLOAD_IN_PROGRESS, DOWNLOAD_IN_PROGRESS -> {
                    ImageMessageInProgress(
                        size = imgParams.normalizedSize().size(),
                        isDownloading = transferStatus == DOWNLOAD_IN_PROGRESS,
                        color = colorsScheme().onScrim,
                    )
                }

                NOT_FOUND -> {
                    ImageMessageFailed(
                        size = imgParams.normalizedSize().size(),
                        isDownloadFailure = true,
                        errorColor = if (shouldAddScrimBg) {
                            colorsScheme().onScrim
                        } else {
                            messageStyle.error()
                        }
                    )
                }

                // Show error placeholder
                FAILED_UPLOAD, FAILED_DOWNLOAD -> {
                    ImageMessageFailed(
                        size = imgParams.normalizedSize().size(),
                        isDownloadFailure = transferStatus == FAILED_DOWNLOAD,
                        errorColor = if (shouldAddScrimBg) {
                            colorsScheme().onScrim
                        } else {
                            messageStyle.error()
                        }
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
fun MediaAssetImage(
    asset: ImageAsset.Remote?,
    size: DpSize,
    transferStatus: AssetTransferStatus?,
    messageStyle: MessageStyle,
    onImageClick: Clickable,
    modifier: Modifier = Modifier,
    assetPath: Path? = null
) {
    Box(
        modifier
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
                    size = size,
                    isDownloading = true,
                    showText = false,
                    color = messageStyle.textColor(),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            LocalInspectionMode.current -> { // preview
                DisplayableImageMessage(
                    imageData = mockedPrivateAsset(),
                    size = size,
                    messageStyle = messageStyle,
                )
            }

            assetPath != null -> {
                AsyncImageMessage(assetPath, size)
            }

            asset != null -> {
                DisplayableImageMessage(
                    imageData = asset,
                    size = size,
                    messageStyle = messageStyle,
                )
            }

            // Show error placeholder
            transferStatus == FAILED_DOWNLOAD -> {
                ImageMessageFailed(
                    size = size,
                    isDownloadFailure = true,
                    errorColor = colorsScheme().error
                )
            }

            transferStatus == NOT_FOUND -> {
                ImageMessageFailed(
                    size = size,
                    isDownloadFailure = true,
                    errorColor = colorsScheme().error
                )
            }
        }
    }
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
