/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.calling.ongoing

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.WakeUpScreenPopUpNavigationAnimation
import com.wire.android.ui.calling.CallingNavArgs
import com.wire.android.ui.calling.ConversationName
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.controlbuttons.CameraButton
import com.wire.android.ui.calling.controlbuttons.CameraFlipButton
import com.wire.android.ui.calling.controlbuttons.HangUpButton
import com.wire.android.ui.calling.controlbuttons.MicrophoneButton
import com.wire.android.ui.calling.controlbuttons.SpeakerButton
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.fullscreen.DoubleTapToast
import com.wire.android.ui.calling.ongoing.fullscreen.FullScreenTile
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.android.ui.calling.ongoing.participantsview.VerticalCallingPager
import com.wire.android.ui.common.banner.SecurityClassificationBannerForConversation
import com.wire.android.ui.common.bottomsheet.WireBottomSheetScaffold
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import java.util.Locale

@RootNavGraph
@Destination(
    navArgsDelegate = CallingNavArgs::class,
    style = WakeUpScreenPopUpNavigationAnimation::class
)
@Composable
fun OngoingCallScreen(
    navigator: Navigator,
    ongoingCallViewModel: OngoingCallViewModel = hiltViewModel(),
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
) {
    LaunchedEffect(ongoingCallViewModel.state.flowState) {
        when (ongoingCallViewModel.state.flowState) {
            OngoingCallState.FlowState.CallClosed -> navigator.navigateBack()
            OngoingCallState.FlowState.Default -> { /* do nothing */
            }
        }
    }
    with(sharedCallingViewModel.callState) {
        OngoingCallContent(
            conversationId = conversationId,
            conversationName = conversationName,
            participants = participants,
            isMuted = isMuted ?: true,
            isCameraOn = isCameraOn,
            isSpeakerOn = isSpeakerOn,
            isCbrEnabled = isCbrEnabled,
            isOnFrontCamera = isOnFrontCamera,
            shouldShowDoubleTapToast = ongoingCallViewModel.shouldShowDoubleTapToast,
            toggleSpeaker = sharedCallingViewModel::toggleSpeaker,
            toggleMute = sharedCallingViewModel::toggleMute,
            hangUpCall = { sharedCallingViewModel.hangUpCall(navigator::navigateBack) },
            toggleVideo = sharedCallingViewModel::toggleVideo,
            flipCamera = sharedCallingViewModel::flipCamera,
            setVideoPreview = sharedCallingViewModel::setVideoPreview,
            clearVideoPreview = sharedCallingViewModel::clearVideoPreview,
            navigateBack = navigator::navigateBack,
            requestVideoStreams = ongoingCallViewModel::requestVideoStreams,
            hideDoubleTapToast = ongoingCallViewModel::hideDoubleTapToast
        )
        BackHandler(enabled = isCameraOn, navigator::navigateBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OngoingCallContent(
    conversationId: ConversationId,
    conversationName: ConversationName?,
    participants: List<UICallParticipant>,
    isMuted: Boolean,
    isCameraOn: Boolean,
    isOnFrontCamera: Boolean,
    isSpeakerOn: Boolean,
    isCbrEnabled: Boolean,
    shouldShowDoubleTapToast: Boolean,
    toggleSpeaker: () -> Unit,
    toggleMute: () -> Unit,
    hangUpCall: () -> Unit,
    toggleVideo: () -> Unit,
    flipCamera: () -> Unit,
    setVideoPreview: (view: View) -> Unit,
    clearVideoPreview: () -> Unit,
    navigateBack: () -> Unit,
    hideDoubleTapToast: () -> Unit,
    requestVideoStreams: (participants: List<UICallParticipant>) -> Unit
) {

    val sheetInitialValue = SheetValue.PartiallyExpanded
    val sheetState = rememberStandardBottomSheetState(
        initialValue = sheetInitialValue
    )

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    var shouldOpenFullScreen by remember { mutableStateOf(false) }
    var selectedParticipantForFullScreen by remember { mutableStateOf(SelectedParticipant()) }

    WireBottomSheetScaffold(
        sheetDragHandle = null,
        topBar = {
            OngoingCallTopBar(
                conversationName = when (conversationName) {
                    is ConversationName.Known -> conversationName.name
                    is ConversationName.Unknown -> stringResource(id = conversationName.resourceId)
                    else -> ""
                },
                isCbrEnabled = isCbrEnabled,
                onCollapse = navigateBack
            )
        },
        sheetPeekHeight = dimensions().defaultSheetPeekHeight,
        scaffoldState = scaffoldState,
        sheetContent = {
            CallingControls(
                conversationId = conversationId,
                isMuted = isMuted,
                isCameraOn = isCameraOn,
                isOnFrontCamera = isOnFrontCamera,
                isSpeakerOn = isSpeakerOn,
                toggleSpeaker = toggleSpeaker,
                toggleMute = toggleMute,
                onHangUpCall = hangUpCall,
                onToggleVideo = toggleVideo,
                flipCamera = flipCamera
            )
        },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(
                    top = it.calculateTopPadding(),
                    bottom = dimensions().defaultSheetPeekHeight
                )
        ) {

            if (participants.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WireCircularProgressIndicator(
                        progressColor = MaterialTheme.wireColorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        size = dimensions().spacing32x
                    )
                    Text(
                        text = stringResource(id = R.string.calling_screen_connecting_until_call_established),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {

                    // if there is only one in the call, do not allow full screen
                    if (participants.size == 1) {
                        shouldOpenFullScreen = false
                    }

                    // if we are on full screen, and that user left the call, then we leave the full screen
                    if (participants.find { user -> user.id == selectedParticipantForFullScreen.userId } == null) {
                        shouldOpenFullScreen = false
                    }

                    if (shouldOpenFullScreen) {
                        hideDoubleTapToast()
                        FullScreenTile(
                            selectedParticipant = selectedParticipantForFullScreen,
                            height = this@BoxWithConstraints.maxHeight - dimensions().spacing4x
                        ) {
                            shouldOpenFullScreen = !shouldOpenFullScreen
                        }
                    } else {
                        VerticalCallingPager(
                            participants = participants,
                            isSelfUserCameraOn = isCameraOn,
                            isSelfUserMuted = isMuted,
                            contentHeight = this@BoxWithConstraints.maxHeight,
                            onSelfVideoPreviewCreated = setVideoPreview,
                            onSelfClearVideoPreview = clearVideoPreview,
                            requestVideoStreams = requestVideoStreams,
                            onDoubleTap = { selectedUserId, selectedClientId, isSelf ->
                                selectedParticipantForFullScreen = SelectedParticipant(
                                    userId = selectedUserId,
                                    clientId = selectedClientId,
                                    isSelfUser = isSelf
                                )
                                shouldOpenFullScreen = !shouldOpenFullScreen
                            }
                        )
                        DoubleTapToast(
                            modifier = Modifier.align(Alignment.TopCenter),
                            enabled = shouldShowDoubleTapToast,
                            text = stringResource(id = R.string.calling_ongoing_double_tap_for_full_screen)
                        ) {
                            hideDoubleTapToast()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OngoingCallTopBar(
    conversationName: String,
    isCbrEnabled: Boolean,
    onCollapse: () -> Unit
) {
    Column {
        WireCenterAlignedTopAppBar(
            onNavigationPressed = onCollapse,
            titleStyle = MaterialTheme.wireTypography.title02,
            maxLines = 1,
            title = conversationName,
            navigationIconType = NavigationIconType.Collapse,
            elevation = 0.dp,
            actions = {}
        )
        if (isCbrEnabled) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = -(5).dp),
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.calling_constant_bit_rate_indication).uppercase(
                    Locale.getDefault()
                ),
                color = colorsScheme().secondaryText,
                style = MaterialTheme.wireTypography.title03,
            )
        }
    }
}

@Composable
private fun CallingControls(
    conversationId: ConversationId,
    isMuted: Boolean,
    isCameraOn: Boolean,
    isSpeakerOn: Boolean,
    isOnFrontCamera: Boolean,
    toggleSpeaker: () -> Unit,
    toggleMute: () -> Unit,
    onHangUpCall: () -> Unit,
    onToggleVideo: () -> Unit,
    flipCamera: () -> Unit
) {
    Column(
        modifier = Modifier.height(dimensions().defaultSheetPeekHeight)
    ) {
        Spacer(modifier = Modifier.weight(1F))
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions().spacing56x)
        ) {
            MicrophoneButton(isMuted = isMuted) { toggleMute() }
            CameraButton(
                isCameraOn = isCameraOn,
                onCameraPermissionDenied = { },
                onCameraButtonClicked = onToggleVideo
            )

            SpeakerButton(
                isSpeakerOn = isSpeakerOn,
                onSpeakerButtonClicked = toggleSpeaker
            )

            if (isCameraOn) {
                CameraFlipButton(isOnFrontCamera, flipCamera)
            }

            HangUpButton(
                modifier = Modifier.size(MaterialTheme.wireDimensions.defaultCallingHangUpButtonSize),
                onHangUpButtonClicked = onHangUpCall
            )
        }
        Spacer(modifier = Modifier.weight(1F))
        SecurityClassificationBannerForConversation(conversationId)
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingCallTopBar() {
    OngoingCallTopBar("Default", true) { }
}
