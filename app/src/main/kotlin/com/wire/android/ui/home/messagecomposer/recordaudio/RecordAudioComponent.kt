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
 */
package com.wire.android.ui.home.messagecomposer.recordaudio

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.sebaslogen.resaca.hilt.hiltViewModelScoped
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.permission.rememberRecordAudioRequestFlow
import com.wire.android.util.ui.KeyboardHeight

@Composable
fun RecordAudioComponent(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    snackbarHostState: SnackbarHostState,
    onAudioRecorded: (UriAsset) -> Unit,
    onCloseRecordAudio: () -> Unit
) {
    val viewModel: RecordAudioViewModel = hiltViewModelScoped<RecordAudioViewModel>()
    val context = LocalContext.current

    val recordAudioFlow = RecordAudioFlow(
        startRecording = { viewModel.startRecording() },
        showPermissionsDeniedDialog = viewModel::showPermissionsDeniedDialog
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
                viewModel.getButtonState() != RecordAudioButtonState.ENABLED
            ) {
                viewModel.stopRecording()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(containerHeight)
            .background(colorsScheme().background)
    ) {
        Divider(color = MaterialTheme.wireColorScheme.outline)
        RecordAudioButtonClose(
            onClick = { viewModel.showDiscardRecordingDialog(onCloseRecordAudio) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = dimensions().spacing0x, end = dimensions().spacing0x)
        )

        val buttonModifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = dimensions().spacing80x)

        when (viewModel.getButtonState()) {
            RecordAudioButtonState.ENABLED -> RecordAudioButtonEnabled(
                onClick = { recordAudioFlow.launch() },
                modifier = buttonModifier
            )

            RecordAudioButtonState.RECORDING -> RecordAudioButtonRecording(
                onClick = viewModel::stopRecording,
                modifier = buttonModifier
            )

            RecordAudioButtonState.READY_TO_SEND -> RecordAudioButtonSend(
                audioState = viewModel.getAudioState(),
                onClick = {
                    viewModel.sendRecording(
                        onAudioRecorded = onAudioRecorded,
                    ) {
                        onCloseRecordAudio()
                    }
                },
                modifier = buttonModifier,
                outputFile = viewModel.getOutputFile(),
                onPlayAudio = viewModel::onPlayAudio,
                onSliderPositionChange = viewModel::onSliderPositionChange
            )
        }
    }

    DiscardRecordedAudioDialog(
        dialogState = viewModel.getDiscardDialogState(),
        onDismiss = viewModel::onDismissDiscardDialog,
        onDiscard = { viewModel.discardRecording(onCloseRecordAudio) }
    )

    MicrophonePermissionsDeniedDialog(
        dialogState = viewModel.getPermissionsDeniedDialogState(),
        onDismiss = viewModel::onDismissPermissionsDeniedDialog,
        onOpenSettings = {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    )

    RecordedAudioMaxFileSizeReachedDialog(
        dialogState = viewModel.getMaxFileSizeReachedDialogState(),
        onDismiss = viewModel::onDismissMaxFileSizeReachedDialog
    )
}

@Composable
private fun RecordAudioFlow(
    startRecording: () -> Unit,
    showPermissionsDeniedDialog: () -> Unit
) = rememberRecordAudioRequestFlow(
    onPermissionAllowed = {
        startRecording()
    },
    onPermissionDenied = {
        showPermissionsDeniedDialog()
    }
)

/**
 * This height was based on the size of Input Text + Additional Options (Text Format, Ping, etc)
 */
private val composeTextHeight = 128.dp

/**
 * To keep the height of the container true to the previous shown content, we acquire the height of
 * the keyboard + text input and additional options.
 */
private val containerHeight = KeyboardHeight.DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET + composeTextHeight
