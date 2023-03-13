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

package com.wire.android.ui.home.conversations.model

import com.wire.kalium.logic.data.asset.isAudioMimeType
import com.wire.kalium.logic.data.asset.isDisplayableImageMimeType
import okio.Path

/**
 * Represents an attachment part of a message to be sent
 */
data class AttachmentBundle(
    val mimeType: String,
    val dataPath: Path,
    val dataSize: Long,
    val fileName: String,
    val attachmentType: AttachmentType
)

enum class AttachmentType {
    // TODO: Add audio or video later on
    IMAGE, GENERIC_FILE, AUDIO;

    companion object {
        fun fromMimeTypeString(mimeType: String): AttachmentType =
            if (isDisplayableImageMimeType(mimeType)) IMAGE
            else if (isAudioMimeType(mimeType)) AUDIO
            else GENERIC_FILE
    }
}
