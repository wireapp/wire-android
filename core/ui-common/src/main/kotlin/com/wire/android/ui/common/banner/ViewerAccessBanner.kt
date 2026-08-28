/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.common.banner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.ui.common.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun ViewerAccessBanner(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions().spacing48x)
            .background(
                color = colorsScheme().surfaceDim,
                shape = RoundedCornerShape(
                    topStart = dimensions().spacing8x,
                    topEnd = dimensions().spacing8x
                )
            )
            .padding(
                start = dimensions().spacing16x,
                end = dimensions().spacing8x,
                top = dimensions().spacing8x,
                bottom = dimensions().spacing8x,
            )
    ) {
        Text(
            modifier = Modifier
                .padding(end = dimensions().spacing8x)
                .weight(1f),
            text = stringResource(id = R.string.conversation_viewer_access_banner),
            style = MaterialTheme.wireTypography.subline01,
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = stringResource(id = R.string.content_description_close_access_info),
            modifier = Modifier
                .padding(end = dimensions().spacing16x)
                .size(dimensions().spacing16x)
                .clip(shape = CircleShape)
                .clickable(onClick = onCloseClick)
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewViewerAccessBanner() = WireTheme {
    Surface {
        ViewerAccessBanner(onCloseClick = {})
    }
}
