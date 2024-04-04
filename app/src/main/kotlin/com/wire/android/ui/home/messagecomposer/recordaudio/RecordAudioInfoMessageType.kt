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

import com.wire.android.R
import com.wire.android.model.SnackBarMessage
import com.wire.android.util.ui.UIText

sealed class RecordAudioInfoMessageType(override val uiText: UIText) : SnackBarMessage {

    // Unable to Record Audio due to being in a call
    data object UnableToRecordAudioCall : RecordAudioInfoMessageType(
        UIText.StringResource(
            R.string.record_audio_unable_due_to_ongoing_call
        )
    )

    // Unable to Record Audio due to error
    data object UnableToRecordAudioError : RecordAudioInfoMessageType(
        UIText.StringResource(
            R.string.record_audio_unable_due_to_error
        )
    )
}
