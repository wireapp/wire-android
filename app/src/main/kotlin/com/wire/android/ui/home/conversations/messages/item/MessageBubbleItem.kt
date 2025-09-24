/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireColorScheme

@Suppress("CyclomaticComplexMethod")
@Composable
fun MessageBubbleItem(
    message: UIMessage.Regular,
    source: MessageSource,
    messageStatus: MessageStatus,
    modifier: Modifier = Modifier,
    accent: Accent = Accent.Unknown,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showAuthor: Boolean = true,
    useSmallBottomPadding: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    footer: (@Composable (inner: PaddingValues) -> Unit)? = null,
    header: (@Composable (inner: PaddingValues) -> Unit)? = null,
    error: (@Composable () -> Unit)? = null,
    content: @Composable (inner: PaddingValues) -> Unit
) {
    val isSelfMessage = source == MessageSource.Self

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                end = dimensions().messageItemHorizontalPadding,
                top = if (showAuthor) dimensions().spacing6x else dimensions().spacing1x,
                bottom = if (useSmallBottomPadding) dimensions().spacing1x else dimensions().messageItemBottomPadding
            ),
        verticalArrangement = Arrangement.spacedBy(-dimensions().spacing8x, alignment = Alignment.Bottom),
    ) {
        val paddingValue = dimensions().spacing10x
        val leadingPadding = if (leading != null) {
            dimensions().spacing48x
        } else {
            dimensions().spacing0x
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = if (isSelfMessage) {
                Arrangement.End
            } else {
                Arrangement.Start
            },
            verticalAlignment = Alignment.Bottom
        ) {
            if (leading != null) {
                Box(Modifier.size(leadingPadding), contentAlignment = Alignment.BottomStart) {
                    leading()
                }
            } else {
                HorizontalSpace.x12()
            }
            Column(
                modifier = Modifier.applyIf(!message.decryptionFailed) {
                    widthIn(max = dimensions().bubbleMaxWidth)
                },
                horizontalAlignment = if (isSelfMessage) {
                    Alignment.End
                } else {
                    Alignment.Start
                }
            ) {
                val shape = RoundedCornerShape(dimensions().corner16x)

                val bubbleColorOther: Color = if (messageStatus.isDeleted) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.background
                }
                val bubbleColor = when {
                    messageStatus.isDeleted || message.decryptionFailed -> {
                        MaterialTheme.colorScheme.surface
                    }

                    isSelfMessage -> {
                        MaterialTheme.wireColorScheme.wireAccentColors.getOrDefault(
                            accent,
                            MaterialTheme.wireColorScheme.primary
                        )
                    }

                    else -> {
                        bubbleColorOther
                    }
                }

                val borderColor = when {
                    message.decryptionFailed -> MaterialTheme.wireColorScheme.outline
                    messageStatus.isDeleted -> if (isSelfMessage) {
                        MaterialTheme.wireColorScheme.wireAccentColors.getOrDefault(
                            accent,
                            MaterialTheme.wireColorScheme.primary
                        )
                    } else {
                        colorsScheme().outline
                    }

                    else -> {
                        null
                    }
                }

                val bubbleWidthMod = when {
                    !message.decryptionFailed -> {
                        message.assetParams?.normalizedSize()?.width
                            ?.let { Modifier.widthIn(max = it) }
                            ?: Modifier
                    }

                    else -> {
                        Modifier
                    }
                }

                Surface(
                    color = bubbleColor,
                    shape = shape,
                    border = borderColor?.let { BorderStroke(dimensions().spacing1x, it) },
                    modifier = bubbleWidthMod
                        .clip(shape)
                        .interceptCombinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = onClick,
                            onLongPress = onLongClick
                        )
                ) {
                    Column {
                        val contentModifier = if (message.hasAssetParams) {
                            Modifier.padding(
                                bottom = if (useSmallBottomPadding) dimensions().spacing0x else paddingValue
                            )
                        } else {
                            Modifier.padding(all = paddingValue)
                        }
                        val contentPadding = if (message.hasAssetParams) {
                            PaddingValues(horizontal = paddingValue)
                        } else {
                            PaddingValues()
                        }

                        Column(contentModifier) {
                            if (header != null) {
                                val headerPadding = if (message.hasAssetParams) {
                                    PaddingValues(start = paddingValue, end = paddingValue, top = paddingValue)
                                } else {
                                    PaddingValues()
                                }
                                header(headerPadding)
                            }
                            content(contentPadding)
                        }
                    }
                }

                if (error != null) {
                    Column(modifier = Modifier.padding(top = dimensions().spacing8x)) {
                        error()
                    }
                }
            }
        }
        if (footer != null) {
            val footerPadding = PaddingValues(start = paddingValue + leadingPadding, end = paddingValue)
            footer(footerPadding)
        }
    }
}
