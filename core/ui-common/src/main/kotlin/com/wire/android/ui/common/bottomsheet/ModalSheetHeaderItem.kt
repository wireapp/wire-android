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

package com.wire.android.ui.common.bottomsheet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.theme.wireTypography

@Composable
fun ModalSheetHeaderItem(header: MenuModalSheetHeader = MenuModalSheetHeader.Gone) {
    when (header) {
        MenuModalSheetHeader.Gone -> {
            Spacer(modifier = Modifier.height(dimensions().modalBottomSheetNoHeaderVerticalPadding))
        }

        is MenuModalSheetHeader.Visible -> {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.secondary) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        start = dimensions().modalBottomSheetHeaderHorizontalPadding,
                        end = dimensions().modalBottomSheetHeaderHorizontalPadding,
                        top = header.customVerticalPadding ?: dimensions().modalBottomSheetHeaderVerticalPadding,
                        bottom = header.customVerticalPadding ?: dimensions().modalBottomSheetHeaderVerticalPadding
                    )
                ) {
                    header.leadingIcon()
                    Spacer(modifier = Modifier.width(dimensions().spacing8x))
                    Text(
                        text = header.title,
                        style = MaterialTheme.wireTypography.title02
                    )
                }
                WireDivider()
            }
        }
    }
}

sealed class MenuModalSheetHeader {

    data class Visible(
        val title: String,
        val leadingIcon: @Composable () -> Unit = {},
        val customVerticalPadding: Dp? = null
    ) : MenuModalSheetHeader()

    object Gone : MenuModalSheetHeader()
}
