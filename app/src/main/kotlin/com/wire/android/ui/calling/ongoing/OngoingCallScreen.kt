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
@file:Suppress("TooManyFunctions")
@file:OptIn(ExperimentalMaterial3Api::class)

package com.wire.android.ui.calling.ongoing

import android.content.pm.PackageManager
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.common.ObservePictureInPictureMode
import com.wire.android.ui.calling.common.ObserveRotation
import com.wire.android.ui.calling.common.SharedCallingViewActions
import com.wire.android.ui.calling.common.SharedCallingViewModel
import com.wire.android.ui.calling.controlbuttons.CameraButton
import com.wire.android.ui.calling.controlbuttons.HangUpOngoingButton
import com.wire.android.ui.calling.controlbuttons.InCallReactionsButton
import com.wire.android.ui.calling.controlbuttons.MicrophoneButton
import com.wire.android.ui.calling.controlbuttons.SpeakerButton
import com.wire.android.ui.calling.model.CallState
import com.wire.android.ui.calling.model.ConversationName
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.details.CallDetailsBottomSheet
import com.wire.android.ui.calling.ongoing.details.CallDetailsButton
import com.wire.android.ui.calling.ongoing.details.CallDetailsSheetState
import com.wire.android.ui.calling.ongoing.details.CallQualityState
import com.wire.android.ui.calling.ongoing.fullscreen.FullScreenTile
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.android.ui.calling.ongoing.incallreactions.InCallReactionsPanel
import com.wire.android.ui.calling.ongoing.incallreactions.InCallReactionsState
import com.wire.android.ui.calling.ongoing.incallreactions.PreviewInCallReactionState
import com.wire.android.ui.calling.ongoing.incallreactions.drawInCallReactions
import com.wire.android.ui.calling.ongoing.incallreactions.rememberInCallReactionsState
import com.wire.android.ui.calling.ongoing.participantslist.ParticipantList
import com.wire.android.ui.calling.ongoing.participantsview.FloatingSelfUserTile
import com.wire.android.ui.calling.ongoing.participantsview.VerticalCallingPager
import com.wire.android.ui.calling.ongoing.toast.InCallToast
import com.wire.android.ui.calling.ongoing.toast.InCallToastPanel
import com.wire.android.ui.common.ConversationVerificationIcons
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.banner.PreviewSecurityClassificationBannerState
import com.wire.android.ui.common.banner.SecurityClassificationBannerForConversation
import com.wire.android.ui.common.bottomsheet.SheetScrimState
import com.wire.android.ui.common.bottomsheet.WireBottomSheetScaffold
import com.wire.android.ui.common.bottomsheet.WireDragHandle
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.emoji.EmojiPickerBottomSheet
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.PreviewMultipleThemesForLandscape
import com.wire.android.util.ui.PreviewMultipleThemesForPortrait
import com.wire.android.util.ui.PreviewMultipleThemesForSquare
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@Suppress("ParameterWrapping", "CyclomaticComplexMethod")
@Composable
fun OngoingCallScreen(
    conversationId: ConversationId,
    ongoingCallViewModel: OngoingCallViewModel = hiltViewModel<OngoingCallViewModel, OngoingCallViewModel.Factory>(
        key = "ongoing_$conversationId",
        creationCallback = { factory -> factory.create(conversationId = conversationId) }
    ),
    sharedCallingViewModel: SharedCallingViewModel =
        hiltViewModel<SharedCallingViewModel, SharedCallingViewModel.Factory>(
            key = "shared_$conversationId",
            creationCallback = { factory -> factory.create(conversationId = conversationId) }
        )
) {
    val scope = rememberCoroutineScope()
    val permissionPermanentlyDeniedDialogState = rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()
    val inCallReactionsState = rememberInCallReactionsState()
    val callDetailsBottomSheetState = rememberWireModalSheetState<CallDetailsSheetState>()
    val activity = LocalActivity.current
    val isPiPAvailableOnThisDevice = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    val shouldUsePiPMode = BuildConfig.PICTURE_IN_PICTURE_ENABLED && isPiPAvailableOnThisDevice
    var inPictureInPictureMode by remember { mutableStateOf(shouldUsePiPMode && activity.isInPictureInPictureMode) }
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        confirmValueChange = { targetValue ->
            // do not allow to expand the sheet if there is nothing more to show in the expanded state
            !(targetValue == SheetValue.Expanded && ongoingCallViewModel.state.participants.isEmpty())
        }
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    if (shouldUsePiPMode) {
        ObservePictureInPictureMode { inPictureInPictureMode = it }
    }

    LaunchedEffect(ongoingCallViewModel.state.flowState) {
        when (ongoingCallViewModel.state.flowState) {
            OngoingCallState.FlowState.CallClosed -> {
                activity.finishAndRemoveTask()
            }

            OngoingCallState.FlowState.Default -> {
                /* do nothing */
            }
        }
    }

    LaunchedEffect(BuildConfig.CALL_REACTIONS_ENABLED) {
        if (BuildConfig.CALL_REACTIONS_ENABLED) {
            ongoingCallViewModel.inCallReactions.collectLatest { reaction ->
                inCallReactionsState.runAnimation(reaction)
            }
        }
    }

    HandleActions(sharedCallingViewModel.actions) { action ->
        when (action) {
            is SharedCallingViewActions.HungUpCall -> activity.finishAndRemoveTask()
        }
    }
    val onCollapse = remember {
        {
            if (shouldUsePiPMode) {
                (activity as OngoingCallActivity).enterPiPMode(
                    conversationId,
                    ongoingCallViewModel.currentUserId
                )
            } else {
                activity.moveTaskToBack(true)
            }
            Unit
        }
    }

    val onCameraPermissionPermanentlyDenied = remember {
        {
            permissionPermanentlyDeniedDialogState.show(
                PermissionPermanentlyDeniedDialogState.Visible(
                    title = R.string.app_permission_dialog_title,
                    description = R.string.camera_permission_dialog_description
                )
            )
        }
    }

    BackHandler {
        when {
            scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded -> scope.launch {
                scaffoldState.bottomSheetState.partialExpand()
            }

            ongoingCallViewModel.state.selectedParticipant != null -> ongoingCallViewModel.onSelectedParticipant(null)
            shouldUsePiPMode -> (activity as OngoingCallActivity).enterPiPMode(conversationId, ongoingCallViewModel.currentUserId)
            else -> activity.moveTaskToBack(true)
        }
    }

    OngoingCallContent(
        callState = sharedCallingViewModel.callState,
        inCallReactionsState = inCallReactionsState,
        scaffoldState = scaffoldState,
        toggleSpeaker = sharedCallingViewModel::toggleSpeaker,
        toggleMute = sharedCallingViewModel::toggleMute,
        hangUpCall = sharedCallingViewModel::hangUpCall,
        toggleVideo = sharedCallingViewModel::toggleVideo,
        flipCamera = sharedCallingViewModel::flipCamera,
        setVideoPreview = sharedCallingViewModel::setVideoPreview,
        clearVideoPreview = sharedCallingViewModel::clearVideoPreview,
        onCollapse = onCollapse,
        requestVideoStreams = ongoingCallViewModel::requestVideoStreams,
        onSelectedParticipant = ongoingCallViewModel::onSelectedParticipant,
        selectedParticipantForFullScreen = ongoingCallViewModel.state.selectedParticipant,
        onCameraPermissionPermanentlyDenied = onCameraPermissionPermanentlyDenied,
        onReactionClick = ongoingCallViewModel::onReactionClick,
        participants = ongoingCallViewModel.state.participants,
        inPictureInPictureMode = inPictureInPictureMode,
        recentReactions = ongoingCallViewModel.recentReactions,
        callQuality = ongoingCallViewModel.state.callQuality.quality,
        onOpenCallDetails = {
            callDetailsBottomSheetState.show(CallDetailsSheetState.Details)
        },
        othersVideosDisabled = ongoingCallViewModel.state.othersVideosDisabled,
        toasts = ongoingCallViewModel.toasts.values.toSet(),
        onToastClick = ongoingCallViewModel::dismissToast,
    )
    ObserveRotation(sharedCallingViewModel::setUIRotation)

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

    CallDetailsBottomSheet(
        sheetState = callDetailsBottomSheetState,
        callQualityState = ongoingCallViewModel.state.callQuality,
        othersVideosDisabled = ongoingCallViewModel.state.othersVideosDisabled,
        setOthersVideosDisabled = ongoingCallViewModel::setOthersVideosDisabled,
    )
    HandleQualityIntervals(
        sheetValue = callDetailsBottomSheetState.currentValue,
        setQualityInterval = ongoingCallViewModel::setQualityInterval
    )
}

@Composable
private fun HandleQualityIntervals(
    sheetValue: WireSheetValue<CallDetailsSheetState>,
    setQualityInterval: (OngoingCallViewModel.QualityInterval) -> Unit
) {
    val interval = when (sheetValue) { // calculate the quality interval based on the current call details sheet content
        is WireSheetValue.Expanded if sheetValue.value == CallDetailsSheetState.Quality -> OngoingCallViewModel.QualityInterval.SHORT
        else -> OngoingCallViewModel.QualityInterval.NORMAL
    }
    LaunchedEffect(interval) { // update the quality interval each time it needs to be changed
        setQualityInterval(interval)
    }
    DisposableEffect(Unit) { // reset the quality interval to normal when this OngoingCallScreen composable is disposed
        onDispose {
            setQualityInterval(OngoingCallViewModel.QualityInterval.NORMAL)
        }
    }
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
    val currentCallState by rememberUpdatedState(callState)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            with(currentCallState) {
                when {
                    event == Lifecycle.Event.ON_PAUSE && callStatus == CallStatus.ESTABLISHED && isCameraOn -> pauseSendingVideoFeed()
                    event == Lifecycle.Event.ON_RESUME && callStatus == CallStatus.ESTABLISHED && isCameraOn -> startSendingVideoFeed()
                    event == Lifecycle.Event.ON_DESTROY -> clearVideoPreview()
                }
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
@Composable
private fun OngoingCallContent(
    callState: CallState,
    inCallReactionsState: InCallReactionsState,
    scaffoldState: BottomSheetScaffoldState,
    toggleSpeaker: () -> Unit,
    toggleMute: () -> Unit,
    hangUpCall: () -> Unit,
    toggleVideo: () -> Unit,
    flipCamera: () -> Unit,
    setVideoPreview: (view: View) -> Unit,
    clearVideoPreview: () -> Unit,
    onCollapse: () -> Unit,
    onOpenCallDetails: () -> Unit,
    onCameraPermissionPermanentlyDenied: () -> Unit,
    onReactionClick: (String) -> Unit,
    requestVideoStreams: (participants: List<UICallParticipant>) -> Unit,
    onSelectedParticipant: (selectedParticipant: SelectedParticipant?) -> Unit,
    selectedParticipantForFullScreen: SelectedParticipant?,
    participants: PersistentList<UICallParticipant>,
    recentReactions: Map<UserId, String>,
    inPictureInPictureMode: Boolean,
    callQuality: CallQualityState.Quality,
    othersVideosDisabled: Boolean,
    toasts: Set<InCallToast>,
    onToastClick: (toastKey: InCallToast.Key) -> Unit,
    inCallReactionsEnabled: Boolean = BuildConfig.CALL_REACTIONS_ENABLED,
    initialShowInCallReactionsPanel: Boolean = false, // for preview purposes
) {
    val scope = rememberCoroutineScope()
    var sheetPeekHeight by remember { mutableStateOf(0f) }
    var topBarHeight by remember { mutableStateOf(0f) }
    var showInCallReactionsPanel by remember { mutableStateOf(initialShowInCallReactionsPanel && inCallReactionsEnabled) }
    val emojiPickerState = rememberWireModalSheetState<Unit>(skipPartiallyExpanded = false)
    val isConnecting = participants.isEmpty()
    WireBottomSheetScaffold(
        topBar = {
            if (!inPictureInPictureMode) {
                OngoingCallTopBar(
                    conversationName = when (callState.conversationName) {
                        is ConversationName.Known -> callState.conversationName.name
                        is ConversationName.Unknown -> stringResource(id = callState.conversationName.resourceId)
                        else -> ""
                    },
                    isCbrEnabled = callState.isCbrEnabled,
                    onCollapse = onCollapse,
                    protocolInfo = callState.protocolInfo,
                    mlsVerificationStatus = callState.mlsVerificationStatus,
                    proteusVerificationStatus = callState.proteusVerificationStatus,
                    callQuality = callQuality,
                    onOpenCallDetails = onOpenCallDetails,
                    modifier = Modifier.onGloballyPositioned {
                        topBarHeight = it.size.height.toFloat()
                    }
                )
            }
        },
        sheetDragHandle = {
            val dragHandleContentDescription = when (scaffoldState.bottomSheetState.targetValue) {
                SheetValue.Expanded -> stringResource(id = R.string.content_description_calling_expanded_participants_list)
                else -> stringResource(id = R.string.content_description_calling_collapsed_participants_list)
            }
            val dragHandleClickContentDescription = when (scaffoldState.bottomSheetState.targetValue) {
                SheetValue.Expanded -> stringResource(id = R.string.content_description_calling_collapse)
                else -> stringResource(id = R.string.content_description_calling_expand)
            }
            WireDragHandle(
                progress = if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) 0f else 1f,
                modifier = Modifier.semantics {
                    this.contentDescription = dragHandleContentDescription
                    this.onClick(label = dragHandleClickContentDescription, action = null)
                }
            )
        },
        sheetPeekHeight = with(LocalDensity.current) { sheetPeekHeight.toDp() },
        scaffoldState = scaffoldState,
        sheetShadowElevation = dimensions().spacing0x,
        sheetMaxWidth = LocalConfiguration.current.screenWidthDp.dp,
        sheetScrim = SheetScrimState.Visible {
            scope.launch {
                scaffoldState.bottomSheetState.partialExpand()
            }
        },
        sheetContent = {
            if (!inPictureInPictureMode) {
                CallingControls(
                    conversationId = callState.conversationId,
                    isMuted = callState.isMuted ?: true,
                    isCameraOn = callState.isCameraOn,
                    isSpeakerOn = callState.isSpeakerOn,
                    isShowingCallReactions = showInCallReactionsPanel,
                    isConnecting = isConnecting,
                    inCallReactionsEnabled = inCallReactionsEnabled,
                    toggleSpeaker = toggleSpeaker,
                    toggleMute = toggleMute,
                    onHangUpCall = hangUpCall,
                    onToggleVideo = toggleVideo,
                    onCallReactionsClick = {
                        scope.launch {
                            scaffoldState.bottomSheetState.partialExpand()
                        }.invokeOnCompletion {
                            showInCallReactionsPanel = !showInCallReactionsPanel
                        }
                    },
                    onCameraPermissionPermanentlyDenied = onCameraPermissionPermanentlyDenied,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = dimensions().callingControlPanelMaxWidth)
                        .onGloballyPositioned {
                            sheetPeekHeight = it.positionInParent().y + it.size.height.toFloat()
                        }
                )
                BoxWithConstraints {
                    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    Column(
                        modifier = Modifier
                            .heightIn(max = with(LocalDensity.current) { (constraints.maxHeight - topBarHeight).toDp() })
                            .padding(top = max(dimensions().spacing8x, navBarHeight))
                            .background(colorsScheme().background)
                    ) {
                        val lazyListState = rememberLazyListState()
                        Surface(
                            shadowElevation = lazyListState.rememberTopBarElevationState().value,
                            color = MaterialTheme.wireColorScheme.background,
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(1f) // ensure the section header is above the participant items when scrolled
                        ) {
                            SectionHeader(name = stringResource(R.string.calling_details_participants_header, participants.size))
                        }
                        ParticipantList(
                            lazyListState = lazyListState,
                            participants = participants.sortedBy { it.name }.toPersistentList(),
                        )
                    }
                }
            }
        },
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxSize()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LaunchedEffect(this.maxHeight.value) {
                    inCallReactionsState.updateHeight(this@BoxWithConstraints.maxHeight.value)
                }

                if (isConnecting) {
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
                        modifier = Modifier
                            .fillMaxSize()
                            .drawInCallReactions(
                                state = inCallReactionsState,
                                enabled = !inPictureInPictureMode && inCallReactionsEnabled,
                            )
                    ) {
                        val uiCallParticipantToShowOnFullScreen by remember(participants, selectedParticipantForFullScreen) {
                            derivedStateOf {
                                participants
                                    .takeIf { it.size > 1 } // if there is only one in the call, do not allow full screen
                                    ?.find { participant ->
                                        participant.id == selectedParticipantForFullScreen?.userId
                                                && participant.clientId == selectedParticipantForFullScreen.clientId
                                    }
                            }
                        }

                        LaunchedEffect(selectedParticipantForFullScreen, uiCallParticipantToShowOnFullScreen) {
                            // if we have user for full screen selected, but that user left the call, then we leave the full screen
                            if (selectedParticipantForFullScreen != null && uiCallParticipantToShowOnFullScreen == null) {
                                onSelectedParticipant(null)
                            }
                        }

                        uiCallParticipantToShowOnFullScreen?.let { uiCallParticipantToShowOnFullScreen ->
                            FullScreenTile(
                                callState = callState,
                                selectedParticipant = uiCallParticipantToShowOnFullScreen,
                                height = this@BoxWithConstraints.maxHeight,
                                onDoubleTap = {
                                    onSelectedParticipant(null)
                                },
                                requestVideoStreams = requestVideoStreams,
                                setVideoPreview = setVideoPreview,
                                clearVideoPreview = clearVideoPreview,
                                isOnFrontCamera = callState.isOnFrontCamera,
                                flipCamera = flipCamera,
                                othersVideosDisabled = othersVideosDisabled,
                            )
                        } ?: run {
                            VerticalCallingPager(
                                participants = participants,
                                isSelfUserCameraOn = callState.isCameraOn,
                                isSelfUserMuted = callState.isMuted ?: true,
                                isInPictureInPictureMode = inPictureInPictureMode,
                                isOnFrontCamera = callState.isOnFrontCamera,
                                contentHeight = this@BoxWithConstraints.maxHeight,
                                contentWidth = this@BoxWithConstraints.maxWidth,
                                onSelfVideoPreviewCreated = setVideoPreview,
                                onSelfClearVideoPreview = clearVideoPreview,
                                requestVideoStreams = requestVideoStreams,
                                recentReactions = recentReactions,
                                onDoubleTap = onSelectedParticipant,
                                flipCamera = flipCamera,
                                othersVideosDisabled = othersVideosDisabled,
                            )
                        }

                        InCallToastPanel(
                            items = toasts,
                            onToastClick = onToastClick,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(dimensions().spacing4x)
                        )

                        if (BuildConfig.PICTURE_IN_PICTURE_ENABLED && participants.size > 1) {
                            val selfUser = participants.first { it.isSelfUser }
                            FloatingSelfUserTile(
                                modifier = Modifier.align(Alignment.TopEnd),
                                contentHeight = this@BoxWithConstraints.maxHeight,
                                contentWidth = this@BoxWithConstraints.maxWidth,
                                participant = selfUser,
                                isOnFrontCamera = callState.isOnFrontCamera,
                                onSelfUserVideoPreviewCreated = setVideoPreview,
                                onClearSelfUserVideoPreview = clearVideoPreview,
                                flipCamera = flipCamera,
                                othersVideosDisabled = othersVideosDisabled,
                            )
                        }
                    }
                }
            }
            if (!inPictureInPictureMode && showInCallReactionsPanel && inCallReactionsEnabled) {
                InCallReactionsPanel(
                    onReactionClick = onReactionClick,
                    onMoreClick = { emojiPickerState.show(Unit) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            EmojiPickerBottomSheet(
                sheetState = emojiPickerState,
                onEmojiSelected = { emoji, _ ->
                    emojiPickerState.hide()
                    onReactionClick(emoji)
                },
            )
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
    callQuality: CallQualityState.Quality,
    onCollapse: () -> Unit,
    onOpenCallDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
            actions = {
                if (BuildConfig.CALL_QUALITY_MENU_ENABLED) {
                    CallDetailsButton(callQuality = callQuality, onClick = onOpenCallDetails)
                }
            }
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
    isShowingCallReactions: Boolean,
    isConnecting: Boolean,
    inCallReactionsEnabled: Boolean,
    toggleSpeaker: () -> Unit,
    toggleMute: () -> Unit,
    onHangUpCall: () -> Unit,
    onToggleVideo: () -> Unit,
    onCallReactionsClick: () -> Unit,
    onCameraPermissionPermanentlyDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensions().spacing4x, bottom = dimensions().spacing8x)
        ) {
            MicrophoneButton(
                isMuted = isMuted,
                onMicrophoneButtonClicked = toggleMute
            )
            CameraButton(
                isCameraOn = isCameraOn,
                onPermissionPermanentlyDenied = onCameraPermissionPermanentlyDenied,
                onCameraButtonClicked = onToggleVideo
            )

            SpeakerButton(
                isSpeakerOn = isSpeakerOn,
                onSpeakerButtonClicked = toggleSpeaker
            )

            if (inCallReactionsEnabled) {
                InCallReactionsButton(
                    isSelected = isShowingCallReactions,
                    isEnabled = !isConnecting,
                    onInCallReactionsClick = onCallReactionsClick
                )
            }

            HangUpOngoingButton(
                onHangUpButtonClicked = onHangUpCall
            )
        }
        SecurityClassificationBannerForConversation(conversationId)
    }
}

@Suppress("EmptyFunctionBlock")
@Composable
fun PreviewOngoingCallContent(
    participants: PersistentList<UICallParticipant>,
    inCallReactionsPanelVisible: Boolean = false,
    toasts: Set<InCallToast> = emptySet(),
    sheetValue: SheetValue = SheetValue.PartiallyExpanded,
) {
    OngoingCallContent(
        callState = CallState(
            conversationId = ConversationId("conversationId", "domain"),
            conversationName = ConversationName.Known("Conversation Name"),
            isMuted = false,
            isCameraOn = false,
            isOnFrontCamera = false,
            isSpeakerOn = false,
            isCbrEnabled = false,
            protocolInfo = null,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        ),
        inCallReactionsState = PreviewInCallReactionState,
        scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = rememberStandardBottomSheetState(initialValue = sheetValue)),
        toggleSpeaker = {},
        toggleMute = {},
        hangUpCall = {},
        toggleVideo = {},
        flipCamera = {},
        setVideoPreview = {},
        clearVideoPreview = {},
        onCollapse = {},
        onCameraPermissionPermanentlyDenied = {},
        onReactionClick = {},
        requestVideoStreams = {},
        participants = participants,
        inPictureInPictureMode = false,
        onSelectedParticipant = {},
        selectedParticipantForFullScreen = null,
        recentReactions = emptyMap(),
        inCallReactionsEnabled = true,
        initialShowInCallReactionsPanel = inCallReactionsPanelVisible,
        callQuality = CallQualityState.Quality.GOOD,
        onOpenCallDetails = {},
        othersVideosDisabled = true,
        toasts = toasts,
        onToastClick = {},
    )
}

@PreviewMultipleThemesForPortrait
@PreviewMultipleThemesForLandscape
@PreviewMultipleThemesForSquare
@Composable
fun PreviewOngoingCallScreen_2Participants() = WireTheme {
    PreviewOngoingCallContent(participants = buildPreviewParticipantsList(2))
}

@PreviewMultipleThemesForPortrait
@PreviewMultipleThemesForLandscape
@Composable
fun PreviewOngoingCallScreen_8Participants() = WireTheme {
    PreviewOngoingCallContent(participants = buildPreviewParticipantsList(8))
}

@PreviewMultipleThemesForSquare
@Composable
fun PreviewOngoingCallScreen_9Participants() = WireTheme {
    PreviewOngoingCallContent(participants = buildPreviewParticipantsList(9))
}

@PreviewMultipleThemesForPortrait
@PreviewMultipleThemesForLandscape
@PreviewMultipleThemesForSquare
@Composable
fun PreviewOngoingCallScreen_WithInCallReactionsPanel() = WireTheme {
    PreviewOngoingCallContent(participants = buildPreviewParticipantsList(2), inCallReactionsPanelVisible = true)
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingCallScreenConnecting() = WireTheme {
    PreviewOngoingCallContent(participants = persistentListOf())
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingCallScreen_WithSecurityBanner() = WireTheme {
    PreviewSecurityClassificationBannerState(SecurityClassificationType.NOT_CLASSIFIED) {
        PreviewOngoingCallContent(participants = buildPreviewParticipantsList(2))
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingCallScreen_WithParticipantsListExpanded() = WireTheme {
    PreviewOngoingCallContent(participants = buildPreviewParticipantsList(3), sheetValue = SheetValue.Expanded)
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingCallScreen_WithToasts() = WireTheme {
    PreviewOngoingCallContent(
        participants = buildPreviewParticipantsList(2),
        toasts = setOf(
            InCallToast.Fullscreen(0L, InCallToast.Fullscreen.Type.DoubleTapToOpen),
            InCallToast.ModerationAction(1L, "1", "Alice", InCallToast.ModerationAction.Type.Muted)
        ),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingCallTopBar() = WireTheme {
    OngoingCallTopBar("Default", true, null, null, null, CallQualityState.Quality.GOOD, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewOngoingCallTopBarWithPoorQuality() = WireTheme {
    OngoingCallTopBar("Default", true, null, null, null, CallQualityState.Quality.POOR, {}, {})
}

fun buildPreviewParticipantsList(count: Int = 10) = buildList {
    repeat(count) { index ->
        add(
            UICallParticipant(
                id = QualifiedID("id_$index", ""),
                clientId = "client_id_$index",
                isSelfUser = false,
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
}.toPersistentList()
