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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.kalium.logic.data.call.CallQualityData.Connection
import com.wire.kalium.logic.data.call.CallQualityData.Jitter
import com.wire.kalium.logic.data.call.CallQualityData.PacketLoss
import com.wire.kalium.logic.data.call.CallQualityData.Peer

@Stable
data class CallQualityState(
    val quality: Quality = Quality.UNKNOWN,
    val peer: Peer = Peer.UNKNOWN,
    val connection: Connection = Connection(),
    val ping: Int = -1, // milliseconds of round-trip time
    val packetLoss: PacketLoss = PacketLoss(), // percentage of packets lost
    val jitter: Jitter = Jitter(), // milliseconds of variation in packet delay
) {
    enum class Quality {
        UNKNOWN, GOOD, FAIR, POOR, NO_INTERNET;

        val isLowQuality: Boolean get() = this >= POOR

        val color
            @Composable get() = when (this) {
                UNKNOWN -> colorsScheme().onBackground // this won't be visible as the text will be "-"
                GOOD -> colorsScheme().positive
                FAIR -> colorsScheme().primary
                POOR -> colorsScheme().warning
                NO_INTERNET -> colorsScheme().error
            }

        val text
            @Composable get() = when (this) {
                UNKNOWN -> "-"
                GOOD -> stringResource(R.string.calling_details_network_quality_good)
                FAIR -> stringResource(R.string.calling_details_network_quality_fair)
                POOR -> stringResource(R.string.calling_details_network_quality_poor)
                NO_INTERNET -> stringResource(R.string.calling_details_network_quality_no_internet)
            }
    }
}
