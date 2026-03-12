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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.ArrowLeftIcon
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemTitle
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.call.CallQualityData
import com.wire.android.ui.common.R as commonR

@Composable
fun CallNetworkQualitySheetContent(
    callQualityData: CallQualityData,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackPressed)
    WireMenuModalSheetContent(
        modifier = modifier,
        header = MenuModalSheetHeader.Visible(
            title = stringResource(R.string.calling_details_network_quality_title),
            titleFillsRemainingSpace = false,
            leadingIcon = {
                IconButton(onClick = onBackPressed) {
                    ArrowLeftIcon(modifier = Modifier.size(dimensions().spacing16x))
                }
            },
            trailingIcon = {
                CallQualityIndicator(
                    callQuality = callQualityData.quality,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .padding(horizontal = dimensions().spacing16x)
                )
            },
            customVerticalPadding = dimensions().spacing0x,
        ),
        menuItems = listOf(
            { PeerValueItem(callQualityData.peer) },
            { ConnectionValueItem(callQualityData.connection) },
            { PacketLossValueItem(callQualityData.packetLoss) },
            { PingValueItem(callQualityData.ping) },
            { JitterValueItem(callQualityData.jitter) },
            { LearnMoreItem() },
        )
    )
}

@Composable
private fun LearnMoreItem() {
    val context = LocalContext.current
    val learnMoreUrl = stringResource(id = R.string.url_call_network_quality_learn_more)
    MenuBottomSheetItem(
        onItemClick = {
            CustomTabsHelper.launchUrl(context, learnMoreUrl)
        },
        title = stringResource(R.string.calling_details_network_quality_learn_more),
        trailing = {
            Icon(
                painter = painterResource(id = commonR.drawable.ic_open_in_new),
                contentDescription = null,
                tint = colorsScheme().onSurface,
                modifier = Modifier.size(dimensions().spacing14x)
            )
        },
    )
}

@Composable
private fun PeerValueItem(peer: CallQualityData.Peer) = QualityValueItem(
    title = stringResource(R.string.calling_details_network_quality_peer),
    value = when (peer) {
        CallQualityData.Peer.USER -> stringResource(R.string.calling_details_network_quality_peer_user)
        CallQualityData.Peer.SERVER -> stringResource(R.string.calling_details_network_quality_peer_server)
        CallQualityData.Peer.UNKNOWN -> ""
    }
)

@Composable
private fun ConnectionValueItem(connection: CallQualityData.Connection) = QualityValueItem(
    title = stringResource(R.string.calling_details_network_quality_connection),
    value = listOfNotNull(
        when (connection.candidate) {
            CallQualityData.Connection.Candidate.RELAY -> stringResource(R.string.calling_details_network_quality_connection_relay)
            else -> null
        },
        when (connection.protocol) {
            CallQualityData.Connection.Protocol.UDP -> connection.protocol.name
            CallQualityData.Connection.Protocol.TCP -> connection.protocol.name
            CallQualityData.Connection.Protocol.UNKNOWN -> ""
        }
    ).joinToString(separator = "/")
)

@Composable
private fun PacketLossValueItem(packetLoss: CallQualityData.PacketLoss) = QualityValueItem(
    title = stringResource(R.string.calling_details_network_quality_packet_loss),
    value = when {
        packetLoss.max >= 0 -> "${packetLoss.max.coerceIn(0, 100)}%"
        else -> ""
    },
    indicator = when {
        packetLoss.max > 10 -> CallQualityIndicatorValue.POOR
        packetLoss.max > 5 -> CallQualityIndicatorValue.FAIR
        else -> CallQualityIndicatorValue.GOOD
    }
)

@Composable
private fun PingValueItem(ping: Int) = QualityValueItem(
    title = stringResource(R.string.calling_details_network_quality_ping),
    value = when {
        ping >= 0 -> stringResource(R.string.calling_details_network_quality_value_milliseconds, ping)
        else -> ""
    },
    indicator = when {
        ping > 150 -> CallQualityIndicatorValue.POOR
        ping >= 50 -> CallQualityIndicatorValue.FAIR
        else -> CallQualityIndicatorValue.GOOD
    }
)

@Composable
private fun JitterValueItem(jitter: CallQualityData.Jitter) = QualityValueItem(
    title = stringResource(R.string.calling_details_network_quality_jitter),
    value = when {
        jitter.max >= 0 -> stringResource(R.string.calling_details_network_quality_value_milliseconds, jitter.max)
        else -> ""
    },
    indicator = when {
        jitter.max > 50 -> CallQualityIndicatorValue.POOR
        jitter.max >= 10 -> CallQualityIndicatorValue.FAIR
        else -> CallQualityIndicatorValue.GOOD
    }
)

@Composable
private fun QualityValueItem(title: String, value: String, indicator: CallQualityIndicatorValue = CallQualityIndicatorValue.GOOD) {
    MenuBottomSheetItem(
        title = title,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MenuItemTitle(
                    title = value,
                    color = colorsScheme().secondaryText
                )
                QualityValueIndicator(
                    qualityValueIndicator = indicator,
                    modifier = Modifier.padding(start = dimensions().spacing4x)
                )
            }
        },
        enabled = false // to disable ripple effect and make this item is not clickable, since it's only used to display information
    )
}

@Composable
private fun QualityValueIndicator(
    qualityValueIndicator: CallQualityIndicatorValue,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = qualityValueIndicator >= CallQualityIndicatorValue.FAIR,
        enter = fadeIn() + scaleIn() + expandIn(expandFrom = Alignment.Center),
        exit = fadeOut() + scaleOut() + shrinkOut(shrinkTowards = Alignment.Center),
    ) {
        val indicatorColor by animateColorAsState(
            targetValue = qualityValueIndicator.color
        )
        Box(
            modifier = modifier
                .size(dimensions().spacing8x)
                .drawBehind { drawCircle(color = indicatorColor, radius = size.minDimension / 2) }
        )
    }
}

@PreviewMultipleThemes
@Composable
fun CallNetworkQualitySheetContentPreview() = WireTheme {
    CallNetworkQualitySheetContent(
        callQualityData = CallQualityData(
            quality = CallQualityData.Quality.NORMAL,
            peer = CallQualityData.Peer.USER,
            connection = CallQualityData.Connection(
                protocol = CallQualityData.Connection.Protocol.UDP,
                candidate = CallQualityData.Connection.Candidate.RELAY,
            ),
            packetLoss = CallQualityData.PacketLoss(up = 5, down = 10),
            ping = 51,
            jitter = CallQualityData.Jitter(
                audio = CallQualityData.AudioJitter(up = 10, down = 201),
                video = CallQualityData.VideoJitter(up = 15, down = 25),
            ),
        ),
        onBackPressed = {},
    )
}
