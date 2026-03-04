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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.button.WireSwitch
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.LocalWireAccent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.call.CallQuality
import com.wire.android.ui.common.R as commonR

@Composable
fun CallDetailsSheetContent(
    callQuality: CallQuality,
    onOpenNetworkQuality: () -> Unit,
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
        TurnOffOtherVideosItem()
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
            text = "Calls are always end-to-end encrypted",
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
    callQuality: CallQuality,
    onOpenNetworkQuality: () -> Unit
) {
    MenuBottomSheetItem(
        title = "Network quality",
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x)
            ) {
                AnimatedContent(
                    targetState = callQuality,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        val enterTransition = slideInVertically { height -> direction * height } + fadeIn()
                        val exitTransition = slideOutVertically { height -> -direction * height } + fadeOut()
                        enterTransition togetherWith exitTransition
                    },
                    modifier = Modifier.weight(1f, fill = true)
                ) { callQuality ->
                    NetworkQualityIndicator(callQuality)
                }
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
private fun NetworkQualityIndicator(
    callQuality: CallQuality,
    textAlign: TextAlign = TextAlign.End,
) {
    CompositionLocalProvider(LocalWireAccent provides Accent.Blue) { // primary color used for medium quality should be always blue
        Text(
            textAlign = textAlign,
            style = typography().body03,
            color = when (callQuality) {
                CallQuality.UNKNOWN -> colorsScheme().secondaryText // shouldn't be visible, but just in case
                CallQuality.NORMAL -> colorsScheme().positive
                CallQuality.MEDIUM -> colorsScheme().primary
                else -> colorsScheme().warning
            },
            text = when (callQuality) {
                CallQuality.UNKNOWN -> ""
                CallQuality.NORMAL -> "Good"
                CallQuality.MEDIUM -> "Fair"
                else -> "Poor"
            },
        )
    }
}


@Composable
private fun TurnOffOtherVideosItem() {
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
                text = "Turn off other videos",
                style = typography().body01,
                color = colorsScheme().onSurface,
                modifier = Modifier.weight(1f, fill = true)
            )
            var checked by remember { mutableStateOf(false) } // TODO: get this value from the view model
            WireSwitch(
                checked = checked,
                onCheckedChange = {
                    checked = it // TODO: implement the logic of turning off other videos in the view model and call it here
                },
            )
        }
        Text(
            text = "This improves the audio quality when your network conditions are poor.",
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
        callQuality = CallQuality.NORMAL,
        onOpenNetworkQuality = {}
    )
}
