/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import android.content.pm.PackageManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.CallState
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
import com.wire.android.ui.calling.ongoing.participantsview.FloatingSelfUserTile
import com.wire.android.ui.calling.ongoing.participantsview.VerticalCallingPager
import com.wire.android.ui.common.ConversationVerificationIcons
import com.wire.android.ui.common.banner.SecurityClassificationBannerForConversation
import com.wire.android.ui.common.bottomsheet.WireBottomSheetScaffold
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import java.util.Locale

@Suppress("ParameterWrapping")
@Composable
fun OngoingCallScreen(
    conversationId: ConversationId,
    ongoingCallViewModel: OngoingCallViewModel = hiltViewModel<OngoingCallViewModel, OngoingCallViewModel.Factory>(
        creationCallback = { factory -> factory.create(conversationId = conversationId) }
    ),
    sharedCallingViewModel: SharedCallingViewModel = hiltViewModel<SharedCallingViewModel, SharedCallingViewModel.Factory>(
        creationCallback = { factory -> factory.create(conversationId = conversationId) }
    )
) {
    val permissionPermanentlyDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    val activity = LocalActivity.current
    val isPiPAvailableOnThisDevice =
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    val shouldUsePiPMode = BuildConfig.PICTURE_IN_PICTURE_ENABLED && isPiPAvailableOnThisDevice

    LaunchedEffect(ongoingCallViewModel.state.flowState) {
        when (ongoingCallViewModel.state.flowState) {
            OngoingCallState.FlowState.CallClosed -> {
                activity.finishAndRemoveTask()
            }

            OngoingCallState.FlowState.Default -> { /* do nothing */
            }
        }
    }

    OngoingCallContent(
        callState = sharedCallingViewModel.callState,
        shouldShowDoubleTapToast = ongoingCallViewModel.shouldShowDoubleTapToast,
        toggleSpeaker = sharedCallingViewModel::toggleSpeaker,
        toggleMute = sharedCallingViewModel::toggleMute,
        hangUpCall = { sharedCallingViewModel.hangUpCall { activity.finishAndRemoveTask() } },
        toggleVideo = sharedCallingViewModel::toggleVideo,
        flipCamera = sharedCallingViewModel::flipCamera,
        setVideoPreview = sharedCallingViewModel::setVideoPreview,
        clearVideoPreview = sharedCallingViewModel::clearVideoPreview,
        onCollapse = {
            if (shouldUsePiPMode) {
                (activity as OngoingCallActivity).enterPiPMode(
                    conversationId,
                    ongoingCallViewModel.currentUserId
                )
            } else {
                activity.moveTaskToBack(true)
            }
        },
        requestVideoStreams = ongoingCallViewModel::requestVideoStreams,
        hideDoubleTapToast = ongoingCallViewModel::hideDoubleTapToast,
        onCameraPermissionPermanentlyDenied = {
            permissionPermanentlyDeniedDialogState.show(
                PermissionPermanentlyDeniedDialogState.Visible(
                    title = R.string.app_permission_dialog_title,
                    description = R.string.camera_permission_dialog_description
                )
            )
        }
    )

    BackHandler {
        if (shouldUsePiPMode) {
            (activity as OngoingCallActivity).enterPiPMode(
                conversationId,
                ongoingCallViewModel.currentUserId
            )
        } else {
            activity.moveTaskToBack(true)
        }
    }

    /**
     * Enter PiP mode when the user leaves the app by pressing the home button.
     */
    val context = LocalContext.current
    DisposableEffect(context) {
        val onUserLeaveBehavior: () -> Unit = {
            if (shouldUsePiPMode) {
                (activity as OngoingCallActivity).enterPiPMode(
                    conversationId,
                    ongoingCallViewModel.currentUserId
                )
            }
        }
        (activity as OngoingCallActivity).addOnUserLeaveHintListener(
            onUserLeaveBehavior
        )
        onDispose {
            activity.removeOnUserLeaveHintListener(onUserLeaveBehavior)
        }
    }

    PermissionPermanentlyDeniedDialog(
        dialogState = permissionPermanentlyDeniedDialogState,
        hideDialog = permissionPermanentlyDeniedDialogState::dismiss
    )

    HandleSendingVideoFeed(
        callState = sharedCallingViewModel.callState,
        pauseSendingVideoFeed = ongoingCallViewModel::pauseSendingVideoFeed,
        startSendingVideoFeed = ongoingCallViewModel::startSendingVideoFeed,
        stopSendingVideoFeed = ongoingCallViewModel::stopSendingVideoFeed,
        clearVideoPreview = sharedCallingViewModel::clearVideoPreview,
    )
}

@Composable
private fun HandleSendingVideoFeed(
    callState: CallState,
    pauseSendingVideoFeed: () -> Unit,
    startSendingVideoFeed: () -> Unit,
    stopSendingVideoFeed: () -> Unit,
    clearVideoPreview: () -> Unit,
) {
    // Pause the video feed when the lifecycle is paused and resume it when the lifecycle is resumed.
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && callState.callStatus == CallStatus.ESTABLISHED && callState.isCameraOn) {
                pauseSendingVideoFeed()
            }
            if (event == Lifecycle.Event.ON_RESUME && callState.callStatus == CallStatus.ESTABLISHED && callState.isCameraOn) {
                startSendingVideoFeed()
            }
            if (event == Lifecycle.Event.ON_DESTROY) {
                clearVideoPreview()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Start/stop sending video feed based on the camera state when the call is established.
    LaunchedEffect(callState.callStatus, callState.isCameraOn) {
        if (callState.callStatus == CallStatus.ESTABLISHED) {
            when (callState.isCameraOn) {
                true -> startSendingVideoFeed()
                false -> stopSendingVideoFeed()
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OngoingCallContent(
    callState: CallState,
    shouldShowDoubleTapToast: Boolean,
    toggleSpeaker: () -> Unit,
    toggleMute: () -> Unit,
    hangUpCall: () -> Unit,
    toggleVideo: () -> Unit,
    flipCamera: () -> Unit,
    setVideoPreview: (view: View) -> Unit,
    clearVideoPreview: () -> Unit,
    onCollapse: () -> Unit,
    hideDoubleTapToast: () -> Unit,
    onCameraPermissionPermanentlyDenied: () -> Unit,
    requestVideoStreams: (participants: List<UICallParticipant>) -> Unit
) = with(callState) {

    val activity = LocalActivity.current

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
        topBar = if (activity.isInPictureInPictureMode) {
            null
        } else {
            {
                OngoingCallTopBar(
                    conversationName = when (conversationName) {
                        is ConversationName.Known -> conversationName.name
                        is ConversationName.Unknown -> stringResource(id = conversationName.resourceId)
                        else -> ""
                    },
                    isCbrEnabled = isCbrEnabled,
                    onCollapse = onCollapse,
                    protocolInfo = protocolInfo,
                    mlsVerificationStatus = mlsVerificationStatus,
                    proteusVerificationStatus = proteusVerificationStatus
                )
            }
        },
        sheetPeekHeight = if (activity.isInPictureInPictureMode) 0.dp else dimensions().defaultSheetPeekHeight,
        scaffoldState = scaffoldState,
        sheetContent = {
            if (!activity.isInPictureInPictureMode) {
                CallingControls(
                    conversationId = conversationId,
                    isMuted = isMuted ?: true,
                    isCameraOn = isCameraOn,
                    isOnFrontCamera = isOnFrontCamera,
                    isSpeakerOn = isSpeakerOn,
                    toggleSpeaker = toggleSpeaker,
                    toggleMute = toggleMute,
                    onHangUpCall = hangUpCall,
                    onToggleVideo = toggleVideo,
                    flipCamera = flipCamera,
                    onCameraPermissionPermanentlyDenied = onCameraPermissionPermanentlyDenied
                )
            }
        },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(
                    top = it.calculateTopPadding(),
                    bottom = if (activity.isInPictureInPictureMode) 0.dp else dimensions().defaultSheetPeekHeight
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
                            callState = callState,
                            selectedParticipant = selectedParticipantForFullScreen,
                            height = this@BoxWithConstraints.maxHeight - dimensions().spacing4x,
                            closeFullScreen = {
                                shouldOpenFullScreen = !shouldOpenFullScreen
                            },
                            onBackButtonClicked = {
                                shouldOpenFullScreen = !shouldOpenFullScreen
                            },
                            setVideoPreview = setVideoPreview,
                            clearVideoPreview = clearVideoPreview,
                        )
                    } else {
                        VerticalCallingPager(
                            participants = participants,
                            isSelfUserCameraOn = isCameraOn,
                            isSelfUserMuted = isMuted ?: true,
                            contentHeight = this@BoxWithConstraints.maxHeight,
                            contentWidth = this@BoxWithConstraints.constraints.maxWidth,
                            onSelfVideoPreviewCreated = setVideoPreview,
                            onSelfClearVideoPreview = clearVideoPreview,
                            requestVideoStreams = requestVideoStreams,
                            onDoubleTap = { selectedParticipant ->
                                selectedParticipantForFullScreen = selectedParticipant
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
                    if (BuildConfig.PICTURE_IN_PICTURE_ENABLED && participants.size > 1) {
                        FloatingSelfUserTile(
                            modifier = Modifier.align(Alignment.TopEnd),
                            contentHeight = this@BoxWithConstraints.maxHeight,
                            contentWidth = this@BoxWithConstraints.constraints.maxWidth.toFloat(),
                            participant = participants.first(),
                            onSelfUserVideoPreviewCreated = setVideoPreview,
                            onClearSelfUserVideoPreview = clearVideoPreview
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
    protocolInfo: Conversation.ProtocolInfo?,
    mlsVerificationStatus: Conversation.VerificationStatus?,
    proteusVerificationStatus: Conversation.VerificationStatus?,
    onCollapse: () -> Unit
) {
    Column {
        WireCenterAlignedTopAppBar(
            onNavigationPressed = onCollapse,
            titleContent = {
                Row(
                    modifier = Modifier.padding(
                        start = dimensions().spacing6x,
                        end = dimensions().spacing6x
                    )
                ) {
                    Text(
                        text = conversationName,
                        style = MaterialTheme.wireTypography.title02,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ConversationVerificationIcons(
                        protocolInfo,
                        mlsVerificationStatus,
                        proteusVerificationStatus
                    )
                }
            },
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
    flipCamera: () -> Unit,
    onCameraPermissionPermanentlyDenied: () -> Unit
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
            MicrophoneButton(isMuted = isMuted, onMicrophoneButtonClicked = toggleMute)
            CameraButton(
                isCameraOn = isCameraOn,
                onPermissionPermanentlyDenied = onCameraPermissionPermanentlyDenied,
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
                modifier = Modifier.size(MaterialTheme.wireDimensions.defaultCallingControlsSize),
                onHangUpButtonClicked = onHangUpCall
            )
        }
        Spacer(modifier = Modifier.weight(1F))
        SecurityClassificationBannerForConversation(conversationId)
    }
}

@Composable
fun PreviewOngoingCallContent(participants: List<UICallParticipant>) {
    OngoingCallContent(
        callState = CallState(
            conversationId = ConversationId("conversationId", "domain"),
            conversationName = ConversationName.Known("Conversation Name"),
            participants = participants,
            isMuted = false,
            isCameraOn = false,
            isOnFrontCamera = false,
            isSpeakerOn = false,
            isCbrEnabled = false,
            protocolInfo = null,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        ),
        shouldShowDoubleTapToast = false,
        toggleSpeaker = {},
        toggleMute = {},
        hangUpCall = {},
        toggleVideo = {},
        flipCamera = {},
        setVideoPreview = {},
        clearVideoPreview = {},
        onCollapse = {},
        hideDoubleTapToast = {},
        onCameraPermissionPermanentlyDenied = {},
        requestVideoStreams = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingCallScreenConnecting() = WireTheme {
    PreviewOngoingCallContent(participants = emptyList())
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingCallScreen_2Participants() = WireTheme {
    PreviewOngoingCallContent(participants = buildPreviewParticipantsList(2))
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingCallScreen_8Participants() = WireTheme {
    PreviewOngoingCallContent(participants = buildPreviewParticipantsList(8))
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingCallTopBar() = WireTheme {
    OngoingCallTopBar("Default", true, null, null, null) { }
}

fun buildPreviewParticipantsList(count: Int = 10) = buildList {
    repeat(count) { index ->
        add(
            UICallParticipant(
                id = QualifiedID("id_$index", ""),
                clientId = "client_id_$index",
                name = "Participant $index",
                isSpeaking = index % 3 == 1,
                isMuted = index % 3 == 2,
                hasEstablishedAudio = index % 3 != 2,
                isCameraOn = false,
                isSharingScreen = false,
                avatar = null,
                membership = Membership.Admin,
                accentId = -1
            )
        )
    }
}
