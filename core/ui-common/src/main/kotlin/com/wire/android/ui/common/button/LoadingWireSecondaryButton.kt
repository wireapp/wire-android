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

package com.wire.android.ui.common.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.shimmerPlaceholder
import com.wire.android.ui.theme.WireTheme

@Composable
fun LoadingWireSecondaryButton(
    modifier: Modifier = Modifier,
    withLeadingIcon: Boolean = false,
    withTrailingIcon: Boolean = false
) {
    Box(
        modifier = modifier
            .width(dimensions().spacing160x)
            .height(dimensions().spacing48x)
            .border(
                width = dimensions().spacing1x,
                color = colorsScheme().secondaryButtonEnabledOutline,
                shape = RoundedCornerShape(size = dimensions().buttonCornerSize)
            )
            .background(
                color = colorsScheme().secondaryButtonEnabled,
                shape = RoundedCornerShape(size = dimensions().buttonCornerSize)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.Center),
        ) {
            if (withLeadingIcon) {
                Spacer(
                    modifier = Modifier
                        .width(dimensions().spacing16x)
                        .height(dimensions().spacing16x)
                        .background(
                            color = colorsScheme().secondaryButtonEnabledOutline,
                            shape = RoundedCornerShape(size = dimensions().corner100x)
                        )
                        .shimmerPlaceholder(
                            visible = true,
                            color = colorsScheme().surfaceContainerHighest,
                            shape = RoundedCornerShape(size = dimensions().corner100x)
                        )
                )
            }
            Spacer(
                modifier = Modifier
                    .padding(start = dimensions().spacing8x, end = dimensions().spacing8x)
                    .width(dimensions().spacing32x)
                    .height(dimensions().spacing16x)
                    .background(
                        color = colorsScheme().secondaryButtonEnabledOutline,
                        shape = RoundedCornerShape(size = dimensions().corner100x)
                    )
                    .shimmerPlaceholder(
                        visible = true,
                        color = colorsScheme().surfaceContainerHighest,
                        shape = RoundedCornerShape(size = dimensions().corner100x)
                    )
            )
            if (withTrailingIcon) {
                Spacer(
                    modifier = Modifier
                        .width(dimensions().spacing16x)
                        .height(dimensions().spacing16x)
                        .background(
                            color = colorsScheme().secondaryButtonEnabledOutline,
                            shape = RoundedCornerShape(size = dimensions().corner100x)
                        )
                        .shimmerPlaceholder(
                            visible = true,
                            color = colorsScheme().surfaceContainerHighest,
                            shape = RoundedCornerShape(size = dimensions().corner100x)
                        ),
                )
            }
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewLoadingWireSecondaryButton() {
    WireTheme {
        LoadingWireSecondaryButton()
    }
}

@MultipleThemePreviews
@Composable
fun PreviewLoadingWireSecondaryButtonWithLeadingIcon() {
    WireTheme {
        LoadingWireSecondaryButton(withLeadingIcon = true)
    }
}

@MultipleThemePreviews
@Composable
fun PreviewLoadingWireSecondaryButtonWithBothIcons() {
    WireTheme {
        LoadingWireSecondaryButton(withLeadingIcon = true, withTrailingIcon = true)
    }
}

@MultipleThemePreviews
@Composable
fun PreviewLoadingWireSecondaryButtonWithTrailingIcon() {
    WireTheme {
        LoadingWireSecondaryButton(withTrailingIcon = true)
    }
}
