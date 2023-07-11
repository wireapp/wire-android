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

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.RecordAudioMessagePlayer
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.io.path.deleteIfExists
import kotlin.properties.Delegates

@Suppress("TooManyFunctions")
interface RecordAudioViewModel {
    fun getButtonState(): RecordAudioButtonState
    fun getDiscardDialogState(): RecordAudioDialogState
    fun getPermissionsDeniedDialogState(): RecordAudioDialogState
    fun getMaxFileSizeReachedDialogState(): RecordAudioDialogState
    fun getOutputFile(): File?
    fun getAudioState(): AudioState
    fun startRecording(context: Context)
    fun stopRecording()
    fun showDiscardRecordingDialog(onCloseRecordAudio: () -> Unit)
    fun onDismissDiscardDialog()
    fun showPermissionsDeniedDialog()
    fun onDismissPermissionsDeniedDialog()
    fun onDismissMaxFileSizeReachedDialog()
    fun discardRecording(onCloseRecordAudio: () -> Unit)
    fun sendRecording(onAudioRecorded: (UriAsset) -> Unit, onComplete: () -> Unit)
    fun onPlayAudio()
    fun onSliderPositionChange(position: Int)
}

@Suppress("TooManyFunctions")
@HiltViewModel
class RecordAudioViewModelImpl @Inject constructor(
    private val kaliumFileSystem: KaliumFileSystem,
    private val recordAudioMessagePlayer: RecordAudioMessagePlayer,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase
) : RecordAudioViewModel, ViewModel() {

    private var state: RecordAudioState by mutableStateOf(RecordAudioState())

    private var assetLimitInMegabyte by Delegates.notNull<Long>()

    private var mediaRecorder: MediaRecorder? = null

    override fun getButtonState(): RecordAudioButtonState = state.buttonState

    override fun getDiscardDialogState(): RecordAudioDialogState = state.discardDialogState

    override fun getPermissionsDeniedDialogState(): RecordAudioDialogState =
        state.permissionsDeniedDialogState

    override fun getMaxFileSizeReachedDialogState(): RecordAudioDialogState =
        state.maxFileSizeReachedDialogState

    override fun getOutputFile(): File? = state.outputFile

    override fun getAudioState(): AudioState = state.audioState

    init {
        observeAudioPlayerState()

        viewModelScope.launch {
            assetLimitInMegabyte = getAssetSizeLimit(isImage = false)
            observeAudioFileSize()
        }
    }

    private fun observeAudioPlayerState() {
        viewModelScope.launch {
            recordAudioMessagePlayer.observableAudioMessagesState.collect {
                state = state.copy(
                    audioState = it
                )
            }
        }
    }

    override fun startRecording(context: Context) {
        state = state.copy(
            buttonState = RecordAudioButtonState.RECORDING
        )

        setUpMediaRecorder(context = context)

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            appLogger.e("[RecordAudio] startRecording: IllegalStateException - ${e.message}")
        } catch (e: IOException) {
            e.printStackTrace()
            appLogger.e("[RecordAudio] startRecording: IOException - ${e.message}")
        }
    }

    private fun setUpMediaRecorder(context: Context) {
        if (mediaRecorder == null) {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            val outputFile = kaliumFileSystem
                .tempFilePath(TEMP_RECORDING_AUDIO_FILE)
                .toFile()
            state = state.copy(
                outputFile = outputFile
            )

            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder?.setOutputFile(outputFile)
            mediaRecorder?.setMaxFileSize(assetLimitInMegabyte)

            observeAudioFileSize()
        }
    }

    private fun observeAudioFileSize() {
        mediaRecorder?.setOnInfoListener { _, what, _ ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                stopRecording()
                state = state.copy(
                    maxFileSizeReachedDialogState = RecordAudioDialogState.MaxFileSizeReached(
                        maxSize = assetLimitInMegabyte.div(SIZE_OF_1MB)
                    )
                )
            }
        }
    }

    override fun stopRecording() {
        state = state.copy(
            buttonState = RecordAudioButtonState.READY_TO_SEND
        )
        mediaRecorder?.stop()
        mediaRecorder?.release()
    }

    override fun showDiscardRecordingDialog(onCloseRecordAudio: () -> Unit) {
        when (state.buttonState) {
            RecordAudioButtonState.ENABLED -> onCloseRecordAudio()
            RecordAudioButtonState.RECORDING,
            RecordAudioButtonState.READY_TO_SEND -> {
                state = state.copy(
                    discardDialogState = RecordAudioDialogState.Shown
                )
            }
        }
    }

    override fun onDismissDiscardDialog() {
        state = state.copy(
            discardDialogState = RecordAudioDialogState.Hidden
        )
    }

    override fun showPermissionsDeniedDialog() {
        state = state.copy(
            permissionsDeniedDialogState = RecordAudioDialogState.Shown
        )
    }

    override fun onDismissPermissionsDeniedDialog() {
        state = state.copy(
            permissionsDeniedDialogState = RecordAudioDialogState.Hidden
        )
    }

    override fun onDismissMaxFileSizeReachedDialog() {
        state = state.copy(
            maxFileSizeReachedDialogState = RecordAudioDialogState.Hidden
        )
    }

    override fun discardRecording(onCloseRecordAudio: () -> Unit) {
        viewModelScope.launch {
            state.outputFile?.toPath()?.deleteIfExists()
            recordAudioMessagePlayer.stop()
            recordAudioMessagePlayer.close()
            state = state.copy(
                buttonState = RecordAudioButtonState.ENABLED,
                discardDialogState = RecordAudioDialogState.Hidden,
                outputFile = null
            )
            onCloseRecordAudio()
        }
    }

    override fun sendRecording(
        onAudioRecorded: (UriAsset) -> Unit,
        onComplete: () -> Unit
    ) {
        state.outputFile?.let {
            onAudioRecorded(
                UriAsset(
                    uri = it.toUri(),
                    saveToDeviceIfInvalid = false
                )
            )
            onComplete()
        }
    }

    override fun onPlayAudio() {
        state.outputFile?.let { audioFile ->
            viewModelScope.launch {
                recordAudioMessagePlayer.playAudio(
                    audioFile = audioFile
                )
            }
        }
    }

    override fun onSliderPositionChange(position: Int) {
        viewModelScope.launch {
            recordAudioMessagePlayer.setPosition(
                position = position
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordAudioMessagePlayer.close()
    }

    private companion object {
        const val TEMP_RECORDING_AUDIO_FILE = "temp_recording.mp3"
        const val SIZE_OF_1MB = 1024 * 1024
    }
}
