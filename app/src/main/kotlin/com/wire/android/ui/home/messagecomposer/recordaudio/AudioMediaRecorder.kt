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

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.wire.android.appLogger
import com.wire.android.util.audioFileDateTime
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.util.DateTimeUtil
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

@ViewModelScoped
class AudioMediaRecorder @Inject constructor(
    private val context: Context,
    private val kaliumFileSystem: KaliumFileSystem,
    private val dispatcherProvider: DispatcherProvider
) {

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    }

    private var mediaRecorder: MediaRecorder? = null

    var outputFile: File? = null

    private val _maxFileSizeReached = MutableSharedFlow<RecordAudioDialogState>()
    fun getMaxFileSizeReached(): Flow<RecordAudioDialogState> =
        _maxFileSizeReached.asSharedFlow()

    fun setUp(assetLimitInMegabyte: Long) {
        if (mediaRecorder == null) {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            outputFile = kaliumFileSystem
                .tempFilePath(getRecordingAudioFileName())
                .toFile()

            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setAudioSamplingRate(SAMPLING_RATE)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder?.setAudioChannels(AUDIO_CHANNELS)
            mediaRecorder?.setAudioEncodingBitRate(AUDIO_ENCONDING_BIT_RATE)
            mediaRecorder?.setMaxFileSize(assetLimitInMegabyte)
            mediaRecorder?.setOutputFile(outputFile)

            observeAudioFileSize(assetLimitInMegabyte)
        }
    }

    fun startRecording() {
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

    fun stop() {
        mediaRecorder?.stop()
    }

    fun release() {
        mediaRecorder?.release()
    }

    private fun observeAudioFileSize(assetLimitInMegabyte: Long) {
        mediaRecorder?.setOnInfoListener { _, what, _ ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                scope.launch {
                    _maxFileSizeReached.emit(
                        RecordAudioDialogState.MaxFileSizeReached(
                            maxSize = assetLimitInMegabyte.div(SIZE_OF_1MB)
                        )
                    )
                }
            }
        }
    }

    private companion object {
        fun getRecordingAudioFileName(): String =
            "wire-audio-${DateTimeUtil.currentInstant().audioFileDateTime()}.m4a"
        const val SIZE_OF_1MB = 1024 * 1024
        const val AUDIO_CHANNELS = 1
        const val SAMPLING_RATE = 44100
        const val AUDIO_ENCONDING_BIT_RATE = 96000
    }
}
