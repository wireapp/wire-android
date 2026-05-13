/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
import android.net.Uri
import com.wire.android.util.SUPPORTED_AUDIO_MIME_TYPE
import com.wire.android.util.fromNioPathToContentUri
import com.wire.android.util.getAudioLengthInMs
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okio.Path
import java.io.File
import javax.inject.Inject

interface RecordAudioFileGateway {
    suspend fun generateAudioFileWithEffects(
        originalFilePath: String,
        effectsFilePath: String
    )

    fun audioLengthInMs(audioPath: Path): Long
    fun contentUri(audioFile: File): Uri
}

class AndroidRecordAudioFileGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generateAudioFileWithEffects: GenerateAudioFileWithEffectsUseCase,
) : RecordAudioFileGateway {

    override suspend fun generateAudioFileWithEffects(
        originalFilePath: String,
        effectsFilePath: String
    ) {
        generateAudioFileWithEffects(
            context = context,
            originalFilePath = originalFilePath,
            effectsFilePath = effectsFilePath
        )
    }

    override fun audioLengthInMs(audioPath: Path): Long =
        getAudioLengthInMs(
            dataPath = audioPath,
            mimeType = SUPPORTED_AUDIO_MIME_TYPE
        )

    override fun contentUri(audioFile: File): Uri =
        context.fromNioPathToContentUri(nioPath = audioFile.toPath())
}

@Module
@InstallIn(ViewModelComponent::class)
interface RecordAudioFileGatewayModule {
    @Binds
    fun bindRecordAudioFileGateway(gateway: AndroidRecordAudioFileGateway): RecordAudioFileGateway
}
