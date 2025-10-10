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

package com.wire.android.ui.common.rowitem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.wire.android.ui.common.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun SectionHeader(
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
fun CollapsingSectionHeader(
    name: String,
    expanded: Boolean,
    onClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
) {
    val arrowRotation: Float by animateFloatAsState(if (expanded) 180f else 90f, label = "CollapsingArrowRotationAnimation")
    val expandDescription = stringResource(
        id = if (expanded) R.string.content_description_collapse_label else R.string.content_description_expand_label
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
        modifier = modifier
            .clickable(onClickLabel = expandDescription) { onClicked(!expanded) }
            .padding(padding)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_collapse),
            contentDescription = null,
            tint = MaterialTheme.wireColorScheme.secondaryText,
            modifier = Modifier
                .width(dimensions().spacing12x)
                .rotate(arrowRotation)
        )
        SectionHeader(
            name = name,
            padding = PaddingValues.Zero
        )
    }
}

@Composable
fun BigSectionHeader(
    name: String,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
) {
    Text(
        text = name,
        modifier = modifier.padding(padding),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.wireTypography.body02,
        color = MaterialTheme.wireColorScheme.onSurface,
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewSectionHeader() = WireTheme {
    SectionHeader(name = "Section name", modifier = Modifier.fillMaxWidth())
}

@PreviewMultipleThemes
@Composable
private fun PreviewCollapsingSectionHeader_Expanded() = WireTheme {
    CollapsingSectionHeader(name = "Section name", expanded = true, onClicked = {}, modifier = Modifier.fillMaxWidth())
}

@PreviewMultipleThemes
@Composable
private fun PreviewCollapsingSectionHeader_Collapsed() = WireTheme {
    CollapsingSectionHeader(name = "Section name", expanded = false, onClicked = {}, modifier = Modifier.fillMaxWidth())
}

@PreviewMultipleThemes
@Composable
private fun PreviewBigSectionHeader() = WireTheme {
    BigSectionHeader(name = "Section name", modifier = Modifier.fillMaxWidth())
}
