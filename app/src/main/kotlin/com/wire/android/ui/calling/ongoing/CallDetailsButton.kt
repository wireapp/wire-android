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
package com.wire.android.ui.calling.ongoing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.OverlapDirection
import com.wire.android.ui.common.OverlappingCirclesRow
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.call.CallQuality

@Composable
fun CallDetailsButton(callQuality: CallQuality, modifier: Modifier = Modifier) {
    IconButton(
        modifier = modifier,
        onClick = { /* TODO */ },
        content = {
            OverlappingCirclesRow(
                overlapSize = dimensions().spacing4x,
                overlapCutoutSize = dimensions().spacing1x,
                overlapDirection = OverlapDirection.StartOnTop,
                items = listOf(
                    { LowNetworkItem(callQuality = callQuality) },
                    { InfoItem() }
                )
            )
        },
    )
}

@Composable
private fun LowNetworkItem(callQuality: CallQuality) {
    AnimatedVisibility(
        visible = callQuality.isLowQuality,
        enter = fadeIn() + scaleIn() + expandIn(expandFrom = Alignment.Center),
        exit = fadeOut() + scaleOut() + shrinkOut(shrinkTowards = Alignment.Center)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(dimensions().wireIconButtonSize)
                .clip(CircleShape)
                .background(color = colorsScheme().warning, shape = CircleShape)
                .padding(horizontal = dimensions().spacing2x, vertical = dimensions().spacing4x),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_network),
                contentDescription = stringResource(R.string.content_description_call_low_network_quality),
                tint = colorsScheme().onWarning,
            )
        }
    }
}

@Composable
private fun InfoItem() {
    Icon(
        painter = painterResource(id = R.drawable.ic_info),
        contentDescription = stringResource(R.string.content_description_call_open_calling_details),
        tint = colorsScheme().onBackground,
        modifier = Modifier.size(dimensions().wireIconButtonSize)
    )
}

@PreviewMultipleThemes
@Composable
fun CallDetailsButtonPreview() = WireTheme {
    CallDetailsButton(callQuality = CallQuality.NORMAL)
}

@PreviewMultipleThemes
@Composable
fun CallDetailsButtonWithLowNetworkPreview() = WireTheme {
    CallDetailsButton(callQuality = CallQuality.POOR)
}
