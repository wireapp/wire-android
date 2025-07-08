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

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun FolderHeader(
    name: String,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
) {
    Text(
        text = name.uppercase(),
        modifier = modifier.padding(padding),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.wireTypography.title03,
        color = MaterialTheme.wireColorScheme.secondaryText,
    )
}

@Composable
fun CollapsingFolderHeader(
    name: String,
    expanded: Boolean,
    onClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    arrowWidth: Dp = dimensions().avatarDefaultSize,
    arrowHorizontalPadding: Dp = dimensions().avatarClickablePadding,
) {
    val arrowRotation: Float by animateFloatAsState(if (expanded) 180f else 90f, label = "CollapsingArrowRotationAnimation")
    val expandDescription = stringResource(
        id = if (expanded) R.string.content_description_collapse_label else R.string.content_description_expand_label
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClickLabel = expandDescription) { onClicked(!expanded) }
            .padding(horizontal = dimensions().spacing8x, vertical = dimensions().spacing16x)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_collapse),
            contentDescription = null,
            tint = MaterialTheme.wireColorScheme.secondaryText,
            modifier = Modifier
                .padding(horizontal = arrowHorizontalPadding)
                .width(arrowWidth)
                .rotate(arrowRotation)
        )
        Text(
            text = name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.wireTypography.title02,
            color = MaterialTheme.wireColorScheme.secondaryText,
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewFolderHeader() = WireTheme {
    FolderHeader(name = "Folder name", modifier = Modifier.fillMaxWidth())
}

@PreviewMultipleThemes
@Composable
private fun PreviewCollapsingFolderHeader_Expanded() = WireTheme {
    CollapsingFolderHeader(name = "Folder name", expanded = true, onClicked = {}, modifier = Modifier.fillMaxWidth())
}

@PreviewMultipleThemes
@Composable
private fun PreviewCollapsingFolderHeader_Collapsed() = WireTheme {
    CollapsingFolderHeader(name = "Folder name", expanded = false, onClicked = {}, modifier = Modifier.fillMaxWidth())
}
