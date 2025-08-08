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
package com.wire.android.ui.home.conversations.details.options

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.shimmerPlaceholder
import com.wire.android.ui.theme.WireTheme

private const val ITEM_COUNT = 3
private const val MIN_ALPHA = 0.2f

@Composable
fun LoadingGroupConversation(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        for (index in 0 until ITEM_COUNT) {
            val progress = index.toFloat() / (ITEM_COUNT - 1)
            val alpha = 1f - progress * (1f - MIN_ALPHA)

            if (index == 0) {
                LoadingNameSection(modifier = Modifier.alpha(alpha))
            } else {
                Column(modifier = Modifier.alpha(alpha)) {
                    LoadingSectionTitle()
                    LoadingOptionItem()
                    LoadingOptionItem()
                }
            }
        }
    }
}

@Composable
private fun LoadingNameSection(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(color = colorsScheme().surface)
            .padding(
                start = dimensions().spacing8x,
                end = dimensions().spacing8x,
                top = dimensions().spacing16x,
                bottom = dimensions().spacing16x
            )
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = dimensions().spacing12x)
        ) {
            Spacer(
                Modifier
                    .height(dimensions().spacing14x)
                    .fillMaxWidth()
                    .background(color = colorsScheme().surfaceContainerHighest, shape = RoundedCornerShape(dimensions().corner16x))
                    .shimmerPlaceholder(
                        visible = true,
                        color = colorsScheme().surfaceContainerHighest,
                        shape = RoundedCornerShape(dimensions().corner16x)
                    )
            )
            Spacer(Modifier.height(dimensions().spacing4x))
            Spacer(
                Modifier
                    .height(dimensions().spacing20x)
                    .fillMaxWidth()
                    .background(color = colorsScheme().surfaceContainerHighest, shape = RoundedCornerShape(dimensions().corner16x))
                    .shimmerPlaceholder(
                        visible = true,
                        color = colorsScheme().surfaceContainerHighest,
                        shape = RoundedCornerShape(dimensions().corner16x)
                    )
            )
        }
        Spacer(
            Modifier
                .height(dimensions().spacing16x)
                .width(dimensions().spacing16x)
                .align(Alignment.CenterVertically)
                .background(color = colorsScheme().surfaceContainerHighest, shape = RoundedCornerShape(dimensions().corner16x))
                .shimmerPlaceholder(
                    visible = true,
                    color = colorsScheme().surfaceContainerHighest,
                    shape = RoundedCornerShape(dimensions().corner16x)
                )
        )
    }
}

@Composable
fun LoadingSectionTitle(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(color = colorsScheme().background),
    ) {
        Spacer(Modifier.height(dimensions().spacing4x))
        Spacer(
            Modifier
                .padding(start = dimensions().spacing8x, end = dimensions().spacing8x)
                .width(dimensions().spacing52x)
                .height(dimensions().spacing14x)
                .background(color = colorsScheme().primaryButtonDisabled, shape = RoundedCornerShape(dimensions().corner16x))
                .shimmerPlaceholder(
                    visible = true,
                    color = colorsScheme().primaryButtonDisabled,
                    shape = RoundedCornerShape(dimensions().corner16x)
                )
        )
        Spacer(Modifier.height(dimensions().spacing4x))
    }
}

@Composable
private fun LoadingOptionItem() {
    Column(
        modifier = Modifier
            .background(color = colorsScheme().surface)
            .padding(
                start = dimensions().spacing8x,
                end = dimensions().spacing8x,
                top = dimensions().spacing12x,
                bottom = dimensions().spacing12x
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
        ) {
            Spacer(
                Modifier
                    .weight(10f)
                    .height(dimensions().spacing20x)
                    .background(color = colorsScheme().surfaceContainerHighest, shape = RoundedCornerShape(dimensions().corner16x))
                    .shimmerPlaceholder(
                        visible = true,
                        color = colorsScheme().surfaceContainerHighest,
                        shape = RoundedCornerShape(dimensions().corner16x)
                    )
            )
            Spacer(
                Modifier
                    .weight(1f)
                    .height(dimensions().spacing20x)
                    .background(color = colorsScheme().surfaceContainerHighest, shape = RoundedCornerShape(dimensions().corner16x))
                    .shimmerPlaceholder(visible = true, color = colorsScheme().surfaceContainerHighest)
            )
            Spacer(
                Modifier
                    .weight(2f)
                    .height(dimensions().spacing24x)
                    .background(color = colorsScheme().surfaceContainerHighest, shape = RoundedCornerShape(dimensions().corner16x))
                    .shimmerPlaceholder(visible = true, color = colorsScheme().surfaceContainerHighest)
            )
        }
        Spacer(Modifier.height(dimensions().spacing8x))
        Spacer(
            Modifier
                .height(dimensions().spacing56x)
                .fillMaxWidth()
                .background(color = colorsScheme().surfaceContainerHighest, shape = RoundedCornerShape(dimensions().corner100x))
                .shimmerPlaceholder(
                    visible = true,
                    color = colorsScheme().surfaceContainerHighest,
                    shape = RoundedCornerShape(dimensions().corner100x)
                )
        )
    }
}

@Composable
@MultipleThemePreviews
fun PreviewLoadingGroupConversation() {
    WireTheme {
        LoadingGroupConversation()
    }
}
