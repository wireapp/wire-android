/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart.standalone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import com.wire.android.media.audiomessage.CellAudioMessageArgs
import com.wire.android.media.audiomessage.CellAudioMessageViewModel
import com.wire.android.media.audiomessage.toWavesMask
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.home.conversations.cellAudioMessageViewModel
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.model.messagetypes.audio.AudioMessageLayout
import com.wire.android.ui.home.conversations.model.messagetypes.audio.SuccessfulAudioMessageContent
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent.AssetMetadata
import com.wire.kalium.logic.data.message.durationMs
import com.wire.kalium.logic.util.fileExtension

@Composable
internal fun CellAudioAssetPreview(
    item: MultipartAttachmentUi,
    conversationId: ConversationId,
    messageStyle: MessageStyle,
) {
    val localPath = item.localPath ?: return

    val wavesMask = remember(item.metadata) {
        (item.metadata as? AssetMetadata.Audio)?.normalizedLoudness?.toWavesMask()
    }

    val durationMs = remember(item.metadata) {
        (item.metadata as? AssetMetadata.Audio)?.durationMs() ?: 0L
    }

    val extension = remember(item.fileName, item.mimeType) {
        item.fileName?.fileExtension() ?: item.mimeType.substringAfter("/")
    }

    val args = remember(conversationId, item.uuid, localPath) {
        CellAudioMessageArgs(
            conversationId = conversationId,
            attachmentId = item.uuid,
            localPath = localPath,
        )
    }

    val viewModel: CellAudioMessageViewModel = if (LocalInspectionMode.current) {
        object : CellAudioMessageViewModel {}
    } else {
        cellAudioMessageViewModel(args = args, wavesMask = wavesMask)
    }

    val sanitizedAudioState by remember(viewModel.state.audioState, durationMs) {
        derivedStateOf {
            viewModel.state.audioState.copy(
                totalTimeInMs = viewModel.state.audioState.sanitizeTotalTime(durationMs.toInt())
            )
        }
    }

    AudioMessageLayout(
        extension = extension,
        size = item.assetSize ?: 0L,
        messageStyle = messageStyle,
    ) {
        SuccessfulAudioMessageContent(
            audioState = sanitizedAudioState,
            wavesMask = wavesMask,
            audioSpeed = viewModel.state.audioSpeed,
            messageStyle = messageStyle,
            onPlayButtonClick = viewModel::playAudio,
            onSliderPositionChange = viewModel::changeAudioPosition,
            onAudioSpeedChange = { viewModel.changeAudioSpeed(viewModel.state.audioSpeed.toggle()) },
        )
    }
}
