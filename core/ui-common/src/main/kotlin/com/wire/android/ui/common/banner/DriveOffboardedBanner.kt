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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.ui.common.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireColorPalette
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.PreviewMultipleThemes

/** Warning shown at the top of conversations of a team whose Drive was offboarded: files are read-only until deleted. */
@Composable
fun DriveOffboardedBanner(modifier: Modifier = Modifier) {
    val backgroundColor = colorsScheme().warning.copy(alpha = WARNING_BACKGROUND_ALPHA)
    val iconColor = colorsScheme().warning
    val textColor = colorsScheme().onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor)
            .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_info),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(dimensions().spacing16x)
        )
        Text(
            text = stringResource(id = R.string.conversation_drive_offboarded_banner),
            style = MaterialTheme.wireTypography.label01,
            color = textColor,
            modifier = Modifier
                .padding(start = dimensions().spacing8x)
                .weight(1f)
        )
    }
}

private const val WARNING_BACKGROUND_ALPHA = 0.16f

@PreviewMultipleThemes
@Composable
private fun PreviewDriveOffboardedBanner() = WireTheme {
    Surface {
        DriveOffboardedBanner()
    }
}
