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
package com.wire.android.ui.calling.ongoing.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.button.WireSwitch
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.call.CallQualityData
import com.wire.android.ui.common.R as commonR

@Composable
fun CallDetailsSheetContent(
    callQuality: CallQualityData.Quality,
    onOpenNetworkQuality: () -> Unit,
    othersVideosDisabled: Boolean,
    onOthersVideosDisabledChanged: (othersVideosDisabled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorsScheme().surface)
    ) {
        EncryptedCallItem()
        NetworkQualityItem(
            callQuality = callQuality,
            onOpenNetworkQuality = onOpenNetworkQuality
        )
        WireDivider()
        TurnOffOtherVideosItem(
            othersVideosDisabled = othersVideosDisabled,
            onOthersVideosDisabledChanged = onOthersVideosDisabledChanged,
        )
    }
}

@Composable
private fun EncryptedCallItem() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x),
        modifier = Modifier
            .fillMaxWidth()
            .background(colorsScheme().positiveVariant)
            .padding(dimensions().spacing16x)
    ) {
        Text(
            text = stringResource(R.string.calling_details_security_banner_label),
            style = typography().body02,
            color = colorsScheme().onPositiveVariant,
            modifier = Modifier.weight(1f, fill = true)
        )
        Icon(
            painter = painterResource(id = commonR.drawable.ic_shield_holo),
            contentDescription = null,
            tint = colorsScheme().onPositiveVariant,
            modifier = Modifier.size(dimensions().spacing16x)
        )
    }
}

@Composable
private fun NetworkQualityItem(
    callQuality: CallQualityData.Quality,
    onOpenNetworkQuality: () -> Unit
) {
    MenuBottomSheetItem(
        title = stringResource(R.string.calling_details_network_quality_title),
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x)
            ) {
                CallQualityIndicator(
                    callQuality = callQuality,
                    modifier = Modifier.weight(1f, fill = true)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "",
                    tint = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
                    modifier = Modifier.size(dimensions().spacing16x)
                )
            }
        },
        onItemClick = onOpenNetworkQuality
    )
}

@Composable
private fun TurnOffOtherVideosItem(
    othersVideosDisabled: Boolean,
    onOthersVideosDisabledChanged: (othersVideosDisabled: Boolean) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensions().spacing16x)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.calling_details_turn_off_other_videos_title),
                style = typography().body01,
                color = colorsScheme().onSurface,
                modifier = Modifier.weight(1f, fill = true)
            )
            WireSwitch(
                checked = othersVideosDisabled,
                onCheckedChange = onOthersVideosDisabledChanged,
            )
        }
        Text(
            text = stringResource(R.string.calling_details_turn_off_other_videos_description),
            style = typography().label04,
            color = colorsScheme().secondaryText,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@PreviewMultipleThemes
@Composable
fun CallDetailsSheetContentPreview() = WireTheme {
    CallDetailsSheetContent(
        callQuality = CallQualityData.Quality.NORMAL,
        onOpenNetworkQuality = {},
        othersVideosDisabled = true,
        onOthersVideosDisabledChanged = {},
    )
}
