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

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.wire.android.appLogger
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.fileDateTime
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCaseImpl.Companion.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.kalium.util.DateTimeUtil
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okio.Path
import okio.buffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

@ViewModelScoped
class AudioMediaRecorder @Inject constructor(
    private val kaliumFileSystem: KaliumFileSystem,
    private val dispatcherProvider: DispatcherProvider
) {

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    }

    private var audioRecorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private var assetLimitInMB: Long = ASSET_SIZE_DEFAULT_LIMIT_BYTES

    var originalOutputPath: Path? = null
    var effectsOutputPath: Path? = null

    private val _maxFileSizeReached = MutableSharedFlow<RecordAudioDialogState>()
    fun getMaxFileSizeReached(): Flow<RecordAudioDialogState> =
        _maxFileSizeReached.asSharedFlow()

    @SuppressLint("MissingPermission")
    fun setUp(assetLimitInMegabyte: Long) {
        assetLimitInMB = assetLimitInMegabyte
        if (audioRecorder == null) {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLING_RATE,
                AUDIO_CHANNELS,
                AUDIO_ENCODING
            )

            audioRecorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLING_RATE,
                AUDIO_CHANNELS,
                AUDIO_ENCODING,
                bufferSize
            )

            originalOutputPath = kaliumFileSystem
                .tempFilePath(getRecordingAudioFileName())

            effectsOutputPath = kaliumFileSystem
                .tempFilePath(getRecordingAudioEffectsFileName())
        }
    }

    fun startRecording(): Boolean = try {
        audioRecorder?.startRecording()
        isRecording = true
        recordingThread = Thread { writeAudioDataToFile() }
        recordingThread?.start()
        true
    } catch (e: IllegalStateException) {
        e.printStackTrace()
        appLogger.e("[RecordAudio] startRecording: IllegalStateException - ${e.message}")
        false
    } catch (e: IOException) {
        e.printStackTrace()
        appLogger.e("[RecordAudio] startRecording: IOException - ${e.message}")
        false
    }

    private fun writeWavHeader(bufferedSink: okio.BufferedSink, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)

        // Używamy buffer() aby poprawnie zapisać wartości ciągów znaków
        bufferedSink.writeUtf8("RIFF") // Chunk ID
        bufferedSink.writeIntLe(0) // Placeholder for Chunk Size (will be updated later)
        bufferedSink.writeUtf8("WAVE") // Format
        bufferedSink.writeUtf8("fmt ") // Subchunk1 ID
        bufferedSink.writeIntLe(16) // Subchunk1 Size (PCM)
        bufferedSink.writeShortLe(1) // Audio Format (PCM)
        bufferedSink.writeShortLe(channels) // Number of Channels
        bufferedSink.writeIntLe(sampleRate) // Sample Rate
        bufferedSink.writeIntLe(byteRate) // Byte Rate
        bufferedSink.writeShortLe(blockAlign) // Block Align
        bufferedSink.writeShortLe(bitsPerSample) // Bits Per Sample
        bufferedSink.writeUtf8("data") // Subchunk2 ID
        bufferedSink.writeIntLe(0) // Placeholder for Subchunk2 Size (will be updated later)
    }

    private fun updateWavHeader(filePath: Path) {
        val file = filePath.toFile()
        val fileSize = file.length().toInt()
        val dataSize = fileSize - 44

        val chunkSizeBuffer = ByteBuffer.allocate(4)
        chunkSizeBuffer.order(ByteOrder.LITTLE_ENDIAN)
        chunkSizeBuffer.putInt(fileSize - 8)

        val dataSizeBuffer = ByteBuffer.allocate(4)
        dataSizeBuffer.order(ByteOrder.LITTLE_ENDIAN)
        dataSizeBuffer.putInt(dataSize)

        val randomAccessFile = java.io.RandomAccessFile(file, "rw")

        // Update Chunk Size
        randomAccessFile.seek(4)
        randomAccessFile.write(chunkSizeBuffer.array())

        // Update Subchunk2 Size
        randomAccessFile.seek(40)
        randomAccessFile.write(dataSizeBuffer.array())

        randomAccessFile.close()

        appLogger.i("Updated WAV Header: Chunk Size = ${fileSize - 8}, Data Size = $dataSize")
    }

    fun stop() {
        isRecording = false
        audioRecorder?.stop()
        recordingThread?.join()
    }

    fun release() {
        audioRecorder?.release()
        audioRecorder = null
    }

    private fun writeAudioDataToFile() {
        val data = ByteArray(BUFFER_SIZE)
        var sink: okio.Sink? = null

        try {
            sink = kaliumFileSystem.sink(originalOutputPath!!)
            val bufferedSink = sink.buffer()

            // Write WAV header
            writeWavHeader(bufferedSink, SAMPLING_RATE, AUDIO_CHANNELS, BITS_PER_SAMPLE)

            while (isRecording) {
                val read = audioRecorder?.read(data, 0, BUFFER_SIZE) ?: 0
                if (read > 0) {
                    bufferedSink.write(data, 0, read)
                }

                // Check if the file size exceeds the limit
                val currentSize = originalOutputPath!!.toFile().length()
                if (currentSize > (assetLimitInMB * SIZE_OF_1MB)) {
                    isRecording = false
                    scope.launch {
                        _maxFileSizeReached.emit(
                            RecordAudioDialogState.MaxFileSizeReached(
                                maxSize = assetLimitInMB / SIZE_OF_1MB
                            )
                        )
                    }
                    break
                }
            }

            // Close buffer to ensure all data is written
            bufferedSink.close()

            // Update WAV header with final file size
            updateWavHeader(originalOutputPath!!)
        } catch (e: IOException) {
            e.printStackTrace()
            appLogger.e("[RecordAudio] writeAudioDataToFile: IOException - ${e.message}")
        } finally {
            sink?.close()
        }
    }

    companion object {
        fun getRecordingAudioFileName(): String = "wire-audio-${DateTimeUtil.currentInstant().fileDateTime()}.wav"
        fun getRecordingAudioEffectsFileName(): String = "wire-audio-${DateTimeUtil.currentInstant().fileDateTime()}-filter.wav"

        const val SIZE_OF_1MB = 1024 * 1024
        const val AUDIO_CHANNELS = 1 // Mono
        const val SAMPLING_RATE = 44100
        const val BUFFER_SIZE = 1024
        const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val BITS_PER_SAMPLE = 16
    }
}
