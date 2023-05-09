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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.calling.ConversationName
import com.wire.android.ui.calling.SharedCallingViewModel
import com.wire.android.ui.calling.controlbuttons.CameraButton
import com.wire.android.ui.calling.controlbuttons.CameraFlipButton
import com.wire.android.ui.calling.controlbuttons.HangUpButton
import com.wire.android.ui.calling.controlbuttons.MicrophoneButton
import com.wire.android.ui.calling.controlbuttons.SpeakerButton
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.fullscreen.FullScreenTile
import com.wire.android.ui.calling.ongoing.participantsview.VerticalCallingPager
import com.wire.android.ui.common.SecurityClassificationBanner
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import java.util.Locale

@Composable
fun OngoingCallScreen(
    ongoingCallViewModel: OngoingCallViewModel = hiltViewModel(),
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel(),
) {

    with(sharedCallingViewModel.callState) {
        OngoingCallContent(
            conversationName = conversationName,
            participants = participants,
            isMuted = isMuted ?: true,
            isCameraOn = isCameraOn,
            isSpeakerOn = isSpeakerOn,
            isCbrEnabled = isCbrEnabled,
            isOnFrontCamera = isOnFrontCamera,
            classificationType = securityClassificationType,
            toggleSpeaker = sharedCallingViewModel::toggleSpeaker,
            toggleMute = sharedCallingViewModel::toggleMute,
            hangUpCall = sharedCallingViewModel::hangUpCall,
            toggleVideo = sharedCallingViewModel::toggleVideo,
            flipCamera = sharedCallingViewModel::flipCamera,
            setVideoPreview = sharedCallingViewModel::setVideoPreview,
            clearVideoPreview = sharedCallingViewModel::clearVideoPreview,
            navigateBack = sharedCallingViewModel::navigateBack,
            requestVideoStreams = ongoingCallViewModel::requestVideoStreams
        )
        isCameraOn?.let {
            BackHandler(enabled = it, sharedCallingViewModel::navigateBack)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OngoingCallContent(
    conversationName: ConversationName?,
    participants: List<UICallParticipant>,
    isMuted: Boolean,
    isCameraOn: Boolean,
    isOnFrontCamera: Boolean,
    isSpeakerOn: Boolean,
    isCbrEnabled: Boolean,
    classificationType: SecurityClassificationType,
    toggleSpeaker: () -> Unit,
    toggleMute: () -> Unit,
    hangUpCall: () -> Unit,
    toggleVideo: () -> Unit,
    flipCamera: () -> Unit,
    setVideoPreview: (view: View) -> Unit,
    clearVideoPreview: () -> Unit,
    navigateBack: () -> Unit,
    requestVideoStreams: (participants: List<UICallParticipant>) -> Unit
) {

    val sheetInitialValue =
        if (classificationType == SecurityClassificationType.NONE) BottomSheetValue.Collapsed else BottomSheetValue.Expanded
    val sheetState = rememberBottomSheetState(
        initialValue = sheetInitialValue
    ).also {
        LaunchedEffect(Unit) {
            // Same issue with expanded on other sheets, we need to use animateTo to fully expand programmatically.
            it.animateTo(sheetInitialValue)
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    var shouldFullScreen by remember { mutableStateOf(false) }
    var selectedUserIdForFullScreen by remember { mutableStateOf(UserId(String.EMPTY, String.EMPTY)) }
    var isSelectedUserSelfUser by remember { mutableStateOf(false) }
    var selectedClientIdForFullScreen by remember { mutableStateOf(String.EMPTY) }

    BottomSheetScaffold(
        sheetBackgroundColor = colorsScheme().background,
        backgroundColor = colorsScheme().background,
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
        sheetShape = RoundedCornerShape(topStart = dimensions().corner16x, topEnd = dimensions().corner16x),
        sheetPeekHeight = dimensions().defaultSheetPeekHeight,
        scaffoldState = scaffoldState,
        sheetContent = {
            CallingControls(
                isMuted = isMuted,
                isCameraOn = isCameraOn,
                isOnFrontCamera = isOnFrontCamera,
                isSpeakerOn = isSpeakerOn,
                classificationType = classificationType,
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
                .padding(it)
                .padding(bottom = dimensions().defaultSheetPeekHeight)
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
                        text = stringResource(id = R.string.connectivity_status_bar_connecting),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {

                    // if there is only one in the call, do not allow full screen
                    if (participants.size == 1) {
                        shouldFullScreen = false
                    }

                    // if we are on full screen, and that user left the call, then we leave the full screen
                    if (participants.find { user -> user.id == selectedUserIdForFullScreen } == null) {
                        shouldFullScreen = false
                    }

                    if (shouldFullScreen) {
                        FullScreenTile(
                            userId = selectedUserIdForFullScreen,
                            clientId = selectedClientIdForFullScreen,
                            isSelfUser = isSelectedUserSelfUser,
                            height = this@BoxWithConstraints.maxHeight - dimensions().spacing4x
                        ) {
                            shouldFullScreen = !shouldFullScreen
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
                                selectedUserIdForFullScreen = selectedUserId
                                selectedClientIdForFullScreen = selectedClientId
                                isSelectedUserSelfUser = isSelf
                                shouldFullScreen = !shouldFullScreen
                            }
                        )
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
                text = stringResource(id = R.string.calling_constant_bit_rate_indication).uppercase(Locale.getDefault()),
                color = colorsScheme().secondaryText,
                style = MaterialTheme.wireTypography.title03,
            )
        }
    }
}

@Composable
private fun CallingControls(
    isMuted: Boolean,
    isCameraOn: Boolean,
    isSpeakerOn: Boolean,
    isOnFrontCamera: Boolean,
    classificationType: SecurityClassificationType,
    toggleSpeaker: () -> Unit,
    toggleMute: () -> Unit,
    onHangUpCall: () -> Unit,
    onToggleVideo: () -> Unit,
    flipCamera: () -> Unit,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensions().spacing16x)
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
        SecurityClassificationBanner(classificationType)
    }
}

@Composable
@Preview
fun PreviewOngoingCallTopBar() {
    OngoingCallTopBar("Default", true) { }
}
