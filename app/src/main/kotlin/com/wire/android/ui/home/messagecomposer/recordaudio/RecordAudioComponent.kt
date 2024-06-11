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
package com.wire.android.ui.home.messagecomposer.recordaudio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.sebaslogen.resaca.hilt.hiltViewModelScoped
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.extension.openAppInfoScreen
import com.wire.android.util.permission.rememberRecordAudioRequestFlow

@Composable
fun RecordAudioComponent(
    onAudioRecorded: (UriAsset) -> Unit,
    onCloseRecordAudio: () -> Unit,
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current,
    viewModel: RecordAudioViewModel = hiltViewModelScoped<RecordAudioViewModel>()
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    val recordAudioFlow = RecordAudioFlow(
        startRecording = { viewModel.startRecording() },
        onAudioPermissionPermanentlyDenied = viewModel::showPermissionsDeniedDialog
    )

    LaunchedEffect(Unit) {
        viewModel.getInfoMessage().collect {
            snackbarHostState.showSnackbar(it.asString(context.resources))
        }
    }

    // If `lifecycleOwner` changes, dispose and reset the effect
    DisposableEffect(lifecycleOwner) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP &&
                viewModel.state.buttonState != RecordAudioButtonState.ENABLED
            ) {
                viewModel.stopRecording()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            viewModel.stopRecording()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(colorsScheme().background)
            .then(modifier)
    ) {
        WireDivider(color = MaterialTheme.wireColorScheme.outline)
        RecordAudioButtonClose(
            onClick = { viewModel.showDiscardRecordingDialog(onCloseRecordAudio) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = dimensions().spacing0x, end = dimensions().spacing0x)
        )

        val buttonModifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = dimensions().spacing20x)

        when (viewModel.state.buttonState) {
            RecordAudioButtonState.ENABLED -> RecordAudioButtonEnabled(
                applyAudioFilterState = viewModel.state.shouldApplyEffects,
                applyAudioFilterClick = viewModel::setShouldApplyEffects,
                onClick = { recordAudioFlow.launch() },
                modifier = buttonModifier
            )

            RecordAudioButtonState.RECORDING -> RecordAudioButtonRecording(
                applyAudioFilterState = viewModel.state.shouldApplyEffects,
                onClick = viewModel::stopRecording,
                modifier = buttonModifier
            )

            RecordAudioButtonState.READY_TO_SEND -> RecordAudioButtonSend(
                applyAudioFilterState = viewModel.state.shouldApplyEffects,
                applyAudioFilterClick = viewModel::setApplyEffectsAndPlayAudio,
                audioState = viewModel.state.audioState,
                onClick = {
                    viewModel.sendRecording(onAudioRecorded = onAudioRecorded) {
                        onCloseRecordAudio()
                    }
                },
                modifier = buttonModifier,
                outputFile = viewModel.getPlayableAudioFile(),
                onPlayAudio = viewModel::onPlayAudio,
                onSliderPositionChange = viewModel::onSliderPositionChange
            )
        }
    }

    DiscardRecordedAudioDialog(
        dialogState = viewModel.state.discardDialogState,
        onDismiss = viewModel::onDismissDiscardDialog,
        onDiscard = { viewModel.discardRecording(onCloseRecordAudio) }
    )

    MicrophonePermissionsDeniedDialog(
        dialogState = viewModel.state.permissionsDeniedDialogState,
        onDismiss = viewModel::onDismissPermissionsDeniedDialog,
        onOpenSettings = {
            context.openAppInfoScreen()
        }
    )

    RecordedAudioMaxFileSizeReachedDialog(
        dialogState = viewModel.state.maxFileSizeReachedDialogState,
        onDismiss = viewModel::onDismissMaxFileSizeReachedDialog
    )
}

@Composable
private fun RecordAudioFlow(
    startRecording: () -> Unit,
    onAudioPermissionPermanentlyDenied: () -> Unit
) = rememberRecordAudioRequestFlow(
    onPermissionAllowed = {
        startRecording()
    },
    onPermissionDenied = { /** Nothing to do **/ },
    onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied
)
