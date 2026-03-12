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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.LocalWireAccent
import com.wire.kalium.logic.data.call.CallQualityData

@Composable
internal fun CallQualityIndicator(
    callQuality: CallQualityData.Quality,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.End,
) {
    val callQualityIndicatorValue by remember(callQuality) {
        derivedStateOf {
            when (callQuality) { // mapped to a simpler enum to reduce recompositions and make the animations smoother
                CallQualityData.Quality.UNKNOWN -> CallQualityIndicatorValue.UNKNOWN
                CallQualityData.Quality.NORMAL -> CallQualityIndicatorValue.GOOD
                CallQualityData.Quality.MEDIUM -> CallQualityIndicatorValue.FAIR
                CallQualityData.Quality.POOR,
                CallQualityData.Quality.NETWORK_PROBLEM,
                CallQualityData.Quality.RECONNECTING -> CallQualityIndicatorValue.POOR
            }
        }
    }
    AnimatedContent(
        targetState = callQualityIndicatorValue,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            val enterTransition = slideInVertically { height -> direction * height } + fadeIn()
            val exitTransition = slideOutVertically { height -> -direction * height } + fadeOut()
            enterTransition togetherWith exitTransition
        },
        modifier = modifier
    ) { callQualityIndicatorValue ->
        CompositionLocalProvider(LocalWireAccent provides Accent.Blue) { // primary color used for medium quality should be always blue
            Text(
                textAlign = textAlign,
                style = typography().body03,
                color = when (callQualityIndicatorValue) {
                    CallQualityIndicatorValue.UNKNOWN -> Color.Unspecified
                    CallQualityIndicatorValue.GOOD -> colorsScheme().positive
                    CallQualityIndicatorValue.FAIR -> colorsScheme().primary
                    CallQualityIndicatorValue.POOR -> colorsScheme().error
                },
                text = when (callQualityIndicatorValue) {
                    CallQualityIndicatorValue.UNKNOWN -> ""
                    CallQualityIndicatorValue.GOOD -> stringResource(R.string.calling_details_network_quality_good)
                    CallQualityIndicatorValue.FAIR -> stringResource(R.string.calling_details_network_quality_fair)
                    CallQualityIndicatorValue.POOR -> stringResource(R.string.calling_details_network_quality_poor)
                },
            )
        }
    }
}

private enum class CallQualityIndicatorValue {
    UNKNOWN, GOOD, FAIR, POOR
}
