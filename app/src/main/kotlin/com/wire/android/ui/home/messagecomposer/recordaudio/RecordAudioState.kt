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

import com.wire.android.media.audiomessage.AudioState
import java.io.File

data class RecordAudioState(
    val buttonState: RecordAudioButtonState = RecordAudioButtonState.ENABLED,
    val discardDialogState: RecordAudioDialogState = RecordAudioDialogState.HIDDEN,
    val permissionsDeniedDialogState: RecordAudioDialogState = RecordAudioDialogState.HIDDEN,
    val outputFile: File? = null,
    val audioState: AudioState = AudioState.DEFAULT
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
    READY_TO_SEND
}

enum class RecordAudioDialogState {
    /**
     * Dialog is shown to user
     */
    SHOWN,

    /**
     * Dialog is hidden from user
     */
    HIDDEN
}
