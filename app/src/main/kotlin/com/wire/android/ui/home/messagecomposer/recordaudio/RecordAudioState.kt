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

import com.wire.android.media.audiomessage.AudioState
import java.io.File

data class RecordAudioState(
    val buttonState: RecordAudioButtonState = RecordAudioButtonState.ENABLED,
    val discardDialogState: RecordAudioDialogState = RecordAudioDialogState.Hidden,
    val permissionsDeniedDialogState: RecordAudioDialogState = RecordAudioDialogState.Hidden,
    val maxFileSizeReachedDialogState: RecordAudioDialogState = RecordAudioDialogState.Hidden,
    val originalOutputFile: File? = null,
    val effectsOutputFile: File? = null,
    val shouldApplyEffects: Boolean = false,
    val audioState: AudioState = AudioState.DEFAULT,
    val wavesMask: List<Int>? = null
)

enum class RecordAudioButtonState {
    /**
     * ENABLED: Button for starting an audio message will be shown.
     * Start Recording or Ask permissions
     */
    ENABLED,

    /**
     * RECORDING: Is shown when a recording is in place.
     */
    RECORDING,

    /**
     * READY_TO_SEND: When User finished recording its audio message.
     */
    READY_TO_SEND,

    /**
     * ENCODING: When recorded audio is encoding
     */
    ENCODING
}

sealed class RecordAudioDialogState {
    /**
     * Dialog is shown to user
     */
    object Shown : RecordAudioDialogState()

    /**
     * Dialog is hidden from user
     */
    object Hidden : RecordAudioDialogState()

    /**
     * Max File Size dialog is shown with dynamic max file size
     */
    data class MaxFileSizeReached(
        val maxSize: Long
    ) : RecordAudioDialogState()
}
