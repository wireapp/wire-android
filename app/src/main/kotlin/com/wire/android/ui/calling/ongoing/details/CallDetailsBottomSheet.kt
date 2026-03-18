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
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.call.CallQualityData
import kotlinx.serialization.Serializable

@Composable
fun CallDetailsBottomSheet(
    sheetState: WireModalSheetState<CallDetailsSheetState>,
    callQualityData: CallQualityData,
) {
    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = { callDetailsSheetState ->
            AnimatedContent(
                targetState = callDetailsSheetState,
                transitionSpec = {
                    when (this.targetState) {
                        CallDetailsSheetState.Details -> // navigating back to details from quality
                            TransitionAnimationType.SLIDE.popEnterTransition.togetherWith(TransitionAnimationType.SLIDE.popExitTransition)

                        CallDetailsSheetState.Quality -> // navigating forward to quality from details
                            TransitionAnimationType.SLIDE.enterTransition.togetherWith(TransitionAnimationType.SLIDE.exitTransition)
                    }
                },
            ) { callDetailsSheetState ->
                when (callDetailsSheetState) {
                    CallDetailsSheetState.Details -> CallDetailsSheetContent(
                        callQuality = callQualityData.quality,
                        onOpenNetworkQuality = {
                            sheetState.show(CallDetailsSheetState.Quality)
                        },
                    )

                    CallDetailsSheetState.Quality -> CallNetworkQualitySheetContent(
                        callQualityData = callQualityData,
                        onBackPressed = {
                            sheetState.show(CallDetailsSheetState.Details)
                        }
                    )
                }
            }
        }
    )
}

@Serializable
enum class CallDetailsSheetState {
    Details, Quality
}

@Composable
private fun CallDetailsBottomSheetPreview(callDetailsSheetState: CallDetailsSheetState) = WireTheme {
    CallDetailsBottomSheet(
        sheetState = rememberWireModalSheetState(WireSheetValue.Expanded(callDetailsSheetState)),
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
    )
}

@PreviewMultipleThemes
@Composable
fun CallDetailsBottomSheet_DetailsPagePreview() = CallDetailsBottomSheetPreview(CallDetailsSheetState.Details)

@PreviewMultipleThemes
@Composable
fun CallDetailsBottomSheet_QualityPagePreview() = CallDetailsBottomSheetPreview(CallDetailsSheetState.Quality)
