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
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import com.wire.android.appLogger
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.fileDateTime
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCaseImpl.Companion.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.kalium.util.DateTimeUtil
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import okio.buffer
import java.io.File
import java.io.FileInputStream
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
    var mp4OutputPath: Path? = null

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

            mp4OutputPath = kaliumFileSystem
                .tempFilePath(getRecordingMP4AudioFileName())
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
        val byteRate = sampleRate * channels * (bitsPerSample / BITS_PER_BYTE)
        val blockAlign = channels * (bitsPerSample / BITS_PER_BYTE)

        // We use buffer() to correctly write the string values.
        bufferedSink.writeUtf8(CHUNK_ID_RIFF) // Chunk ID
        bufferedSink.writeIntLe(PLACEHOLDER_SIZE) // Placeholder for Chunk Size (will be updated later)
        bufferedSink.writeUtf8(FORMAT_WAVE) // Format
        bufferedSink.writeUtf8(SUBCHUNK1_ID_FMT) // Subchunk1 ID
        bufferedSink.writeIntLe(SUBCHUNK1_SIZE_PCM) // Subchunk1 Size (PCM)
        bufferedSink.writeShortLe(AUDIO_FORMAT_PCM) // Audio Format (PCM)
        bufferedSink.writeShortLe(channels) // Number of Channels
        bufferedSink.writeIntLe(sampleRate) // Sample Rate
        bufferedSink.writeIntLe(byteRate) // Byte Rate
        bufferedSink.writeShortLe(blockAlign) // Block Align
        bufferedSink.writeShortLe(bitsPerSample) // Bits Per Sample
        bufferedSink.writeUtf8(SUBCHUNK2_ID_DATA) // Subchunk2 ID
        bufferedSink.writeIntLe(PLACEHOLDER_SIZE) // Placeholder for Subchunk2 Size (will be updated later)
    }

    private fun updateWavHeader(filePath: Path) {
        val file = filePath.toFile()
        val fileSize = file.length().toInt()
        val dataSize = fileSize - HEADER_SIZE

        val chunkSizeBuffer = ByteBuffer.allocate(INT_SIZE)
        chunkSizeBuffer.order(ByteOrder.LITTLE_ENDIAN)
        chunkSizeBuffer.putInt(fileSize - CHUNK_ID_SIZE)

        val dataSizeBuffer = ByteBuffer.allocate(INT_SIZE)
        dataSizeBuffer.order(ByteOrder.LITTLE_ENDIAN)
        dataSizeBuffer.putInt(dataSize)

        val randomAccessFile = java.io.RandomAccessFile(file, "rw")

        // Update Chunk Size
        randomAccessFile.seek(CHUNK_SIZE_OFFSET.toLong())
        randomAccessFile.write(chunkSizeBuffer.array())

        // Update Subchunk2 Size
        randomAccessFile.seek(SUBCHUNK2_SIZE_OFFSET.toLong())
        randomAccessFile.write(dataSizeBuffer.array())

        randomAccessFile.close()

        appLogger.i("Updated WAV Header: Chunk Size = ${fileSize - CHUNK_ID_SIZE}, Data Size = $dataSize")
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

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    suspend fun convertWavToMp4(inputFilePath: String): Boolean = withContext(Dispatchers.IO) {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var fileInputStream: FileInputStream? = null

        try {
            val inputFile = File(inputFilePath)
            fileInputStream = FileInputStream(inputFile)

            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(inputFilePath)

            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                SAMPLING_RATE,
                AUDIO_CHANNELS
            )
            format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            muxer = MediaMuxer(mp4OutputPath.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var sawInputEOS = false
            var sawOutputEOS = false

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()

                        val sampleSize = fileInputStream.channel.read(inputBuffer!!)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            val presentationTimeUs = System.nanoTime() / NANOSECONDS_IN_MICROSECOND
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
                        }
                    }
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        continue
                    }

                    if (bufferInfo.size != 0) {
                        outputBuffer?.position(bufferInfo.offset)
                        outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)

                        if (trackIndex == -1) {
                            val newFormat = codec.outputFormat
                            trackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                        }

                        muxer.writeSampleData(trackIndex, outputBuffer!!, bufferInfo)
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                }
            }
            true
        } catch (e: IllegalStateException) {
            appLogger.e("Could not convert wav to mp4: ${e.message}", throwable = e)
            false
        } catch (e: IOException) {
            appLogger.e("Could not convert wav to mp4: ${e.message}", throwable = e)
            false
        } finally {
            try {
                fileInputStream?.close()
            } catch (e: IOException) {
                appLogger.e("Could not close FileInputStream: ${e.message}", throwable = e)
            }

            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: IllegalStateException) {
                appLogger.e("Could not stop or release MediaMuxer: ${e.message}", throwable = e)
            }

            try {
                codec?.stop()
                codec?.release()
            } catch (e: IllegalStateException) {
                appLogger.e("Could not stop or release MediaCodec: ${e.message}", throwable = e)
            }
        }
    }

    companion object {
        fun getRecordingAudioFileName(): String = "wire-audio-${DateTimeUtil.currentInstant().fileDateTime()}.wav"
        fun getRecordingMP4AudioFileName(): String = "wire-audio-${DateTimeUtil.currentInstant().fileDateTime()}.mp4"

        const val SIZE_OF_1MB = 1024 * 1024
        const val AUDIO_CHANNELS = 1 // Mono
        const val SAMPLING_RATE = 16000
        const val BUFFER_SIZE = 1024
        const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val BITS_PER_SAMPLE = 16
        const val BITS_PER_BYTE = 8
        const val HEADER_SIZE = 44
        const val CHUNK_ID_SIZE = 8
        const val INT_SIZE = 4
        const val PLACEHOLDER_SIZE = 0
        const val CHUNK_SIZE_OFFSET = 4
        const val SUBCHUNK2_SIZE_OFFSET = 40
        const val AUDIO_FORMAT_PCM = 1
        const val SUBCHUNK1_SIZE_PCM = 16

        const val CHUNK_ID_RIFF = "RIFF"
        const val FORMAT_WAVE = "WAVE"
        const val SUBCHUNK1_ID_FMT = "fmt "
        const val SUBCHUNK2_ID_DATA = "data"

        private const val BIT_RATE = 64000
        private const val TIMEOUT_US: Long = 10000
        const val NANOSECONDS_IN_MICROSECOND = 1000
    }
}
