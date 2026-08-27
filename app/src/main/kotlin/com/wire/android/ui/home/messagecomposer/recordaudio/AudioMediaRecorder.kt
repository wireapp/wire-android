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
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import com.wire.android.appLogger
import com.wire.android.di.metro.MetroSessionScope
import com.wire.android.media.audiomessage.AudioFocusHelper
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.fileDateTime
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase.AssetSizeLimits.ASSET_SIZE_DEFAULT_LIMIT_BYTES
import com.wire.kalium.util.DateTimeUtil
import com.wire.media.recording.AudioRecorder
import com.wire.media.recording.AudioRecorderEvent
import com.wire.media.recording.AudioRecordingFiles
import com.wire.media.recording.RecordingCapabilityResult
import com.wire.media.recording.RecordingInterruption
import com.wire.media.recording.RecordingSizePolicy
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import okio.buffer
import java.io.FileInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Inject
@ContributesBinding(MetroSessionScope::class, binding = binding<AudioRecorder>())
@Suppress("TooManyFunctions") // Native recording and codec lifecycle stay together at the Android boundary.
class AudioMediaRecorder(
    private val kaliumFileSystem: KaliumFileSystem,
    private val dispatcherProvider: DispatcherProvider,
    private val audioFocusHelper: AudioFocusHelper,
) : AudioRecorder {

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    }

    private var audioRecorder: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false
    private var assetLimitInBytes: Long = ASSET_SIZE_DEFAULT_LIMIT_BYTES

    private var originalOutputPath: Path? = null
    private var m4aOutputPath: Path? = null

    private val mutableEvents = MutableSharedFlow<AudioRecorderEvent>(extraBufferCapacity = 1)
    override val events: Flow<AudioRecorderEvent> = mutableEvents.asSharedFlow()

    init {
        audioFocusHelper.setListener(
            onPauseCurrentAudio = {
                mutableEvents.tryEmit(AudioRecorderEvent.Interrupted(RecordingInterruption.AUDIO_FOCUS))
            },
            onResumeCurrentAudio = {},
        )
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun start(maximumSizeBytes: Long): RecordingCapabilityResult<AudioRecordingFiles> =
        withContext(dispatcherProvider.default()) {
            if (!audioFocusHelper.requestExclusive()) {
                return@withContext RecordingCapabilityResult.Failure("audio_focus_denied")
            }

            try {
                setUp(maximumSizeBytes)
                if (startRecording()) {
                    val original = originalOutputPath
                    val encoded = m4aOutputPath
                    if (original != null && encoded != null) {
                        RecordingCapabilityResult.Success(AudioRecordingFiles(original, encoded))
                    } else {
                        cleanupFailedStart()
                        RecordingCapabilityResult.Failure("recording_paths_unavailable")
                    }
                } else {
                    cleanupFailedStart()
                    RecordingCapabilityResult.Failure("recording_start_failed")
                }
            } catch (cancelled: CancellationException) {
                cleanupFailedStart()
                throw cancelled
            } catch (error: Exception) {
                appLogger.e("[RecordAudio] start failed", error)
                cleanupFailedStart()
                RecordingCapabilityResult.Failure(error.message ?: "recording_start_failed")
            }
        }

    override suspend fun stop(): RecordingCapabilityResult<Unit> = withContext(dispatcherProvider.default()) {
        try {
            stopNativeRecording()
            RecordingCapabilityResult.Success(Unit)
        } catch (error: IllegalStateException) {
            appLogger.e("[RecordAudio] stop: ${error.message}", error)
            RecordingCapabilityResult.Failure(error.message)
        } finally {
            releaseNativeRecorder()
            audioFocusHelper.abandonExclusive()
        }
    }

    override suspend fun encode(source: Path, destination: Path): RecordingCapabilityResult<Path> = try {
        if (convertWavToM4a(source, destination)) {
            RecordingCapabilityResult.Success(destination)
        } else {
            deleteIfExists(destination)
            RecordingCapabilityResult.Failure("audio_encoding_failed")
        }
    } catch (cancelled: CancellationException) {
        deleteIfExists(destination)
        throw cancelled
    }

    override fun release() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        releaseNativeRecorder()
        audioFocusHelper.abandonExclusive()
    }

    @SuppressLint("MissingPermission")
    private fun setUp(maximumSizeBytes: Long) {
        assetLimitInBytes = maximumSizeBytes
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

            m4aOutputPath = kaliumFileSystem
                .tempFilePath(getRecordingM4aAudioFileName())
        }
    }

    private fun startRecording(): Boolean = try {
        audioRecorder?.startRecording()
        isRecording = true
        recordingJob = scope.launch { writeAudioDataToFile() }
        true
    } catch (error: IllegalStateException) {
        appLogger.e("[RecordAudio] startRecording: IllegalStateException - ${error.message}", error)
        false
    } catch (error: IOException) {
        appLogger.e("[RecordAudio] startRecording: IOException - ${error.message}", error)
        false
    }

    private fun writeWavHeader(bufferedSink: okio.BufferedSink, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val byteRate = sampleRate * channels * (bitsPerSample / BITS_PER_BYTE)
        val blockAlign = channels * (bitsPerSample / BITS_PER_BYTE)

        // We use buffer() to correctly write the string values.
        with(bufferedSink) {
            writeUtf8(CHUNK_ID_RIFF) // Chunk ID
            writeIntLe(PLACEHOLDER_SIZE) // Placeholder for Chunk Size (will be updated later)
            writeUtf8(FORMAT_WAVE) // Format
            writeUtf8(SUBCHUNK1_ID_FMT) // Subchunk1 ID
            writeIntLe(SUBCHUNK1_SIZE_PCM) // Subchunk1 Size (PCM)
            writeShortLe(AUDIO_FORMAT_PCM) // Audio Format (PCM)
            writeShortLe(channels) // Number of Channels
            writeIntLe(sampleRate) // Sample Rate
            writeIntLe(byteRate) // Byte Rate
            writeShortLe(blockAlign) // Block Align
            writeShortLe(bitsPerSample) // Bits Per Sample
            writeUtf8(SUBCHUNK2_ID_DATA) // Subchunk2 ID
            writeIntLe(PLACEHOLDER_SIZE) // Placeholder for Subchunk2 Size (will be updated later)
        }
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

        RandomAccessFile(file, "rw").use { randomAccessFile ->
            // Update Chunk Size
            randomAccessFile.seek(CHUNK_SIZE_OFFSET.toLong())
            randomAccessFile.write(chunkSizeBuffer.array())
            // Update Subchunk2 Size
            randomAccessFile.seek(SUBCHUNK2_SIZE_OFFSET.toLong())
            randomAccessFile.write(dataSizeBuffer.array())
        }

        appLogger.i("Updated WAV Header: Chunk Size = ${fileSize - CHUNK_ID_SIZE}, Data Size = $dataSize")
    }

    private suspend fun stopNativeRecording() {
        isRecording = false
        try {
            if (audioRecorder != null) {
                audioRecorder?.stop()
            }
        } finally {
            recordingJob?.cancelAndJoin()
            recordingJob = null
        }
    }

    private fun releaseNativeRecorder() {
        audioRecorder?.release()
        audioRecorder = null
    }

    @Suppress("NestedBlockDepth")
    private suspend fun writeAudioDataToFile() {
        withContext(dispatcherProvider.io()) {
            val data = ByteArray(BUFFER_SIZE)

            try {
                kaliumFileSystem.sink(originalOutputPath!!).use { sink ->
                    sink.buffer()
                        .use {
                            writeWavHeader(it, SAMPLING_RATE, AUDIO_CHANNELS, BITS_PER_SAMPLE)
                            while (isRecording && isActive) {
                                val read = audioRecorder?.read(data, 0, BUFFER_SIZE) ?: 0
                                if (read > 0) {
                                    it.write(data, 0, read)
                                }

                                // Check if the file size exceeds the limit
                                val currentSize = kaliumFileSystem.size(originalOutputPath!!) ?: 0L
                                if (RecordingSizePolicy.hasReachedLimit(currentSize, assetLimitInBytes)) {
                                    isRecording = false
                                    mutableEvents.emit(
                                        AudioRecorderEvent.MaxFileSizeReached(assetLimitInBytes)
                                    )
                                    break
                                }
                            }
                            it.flush()
                            updateWavHeader(originalOutputPath!!)
                        }
                }
            } catch (error: IOException) {
                appLogger.e("[RecordAudio] writeAudioDataToFile: IOException - ${error.message}", error)
            }
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "TooGenericExceptionCaught")
    private suspend fun convertWavToM4a(inputFilePath: Path, outputFilePath: Path): Boolean = withContext(dispatcherProvider.io()) {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var codecStarted = false
        var muxerStarted = false
        var success = true

        try {
            FileInputStream(inputFilePath.toFile()).use { fileInputStream ->
                outputFilePath.toFile().let { outputFile ->
                    ParcelFileDescriptor.open(
                        outputFile,
                        ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                    ).use { parcelFileDescriptor ->

                        val format = MediaFormat.createAudioFormat(
                            MediaFormat.MIMETYPE_AUDIO_AAC,
                            SAMPLING_RATE,
                            AUDIO_CHANNELS
                        )
                        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)

                        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                        val mediaCodec = codec!!
                        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                        mediaCodec.start()
                        codecStarted = true

                        val bufferInfo = MediaCodec.BufferInfo()
                        muxer = MediaMuxer(parcelFileDescriptor.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                        val mediaMuxer = muxer!!

                        var trackIndex = -1
                        var sawInputEOS = false
                        var sawOutputEOS = false

                        var retryCount = 0
                        var presentationTimeUs = 0L
                        val bytesPerSample = (BITS_PER_SAMPLE / BITS_PER_BYTE) * AUDIO_CHANNELS

                        while (!sawOutputEOS && retryCount < MAX_RETRY_COUNT) {
                            if (!sawInputEOS) {
                                val inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US)
                                if (inputBufferIndex >= 0) {
                                    val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                                    inputBuffer?.clear()

                                    val sampleSize = fileInputStream.channel.read(inputBuffer!!)
                                    if (sampleSize < 0) {
                                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                        sawInputEOS = true
                                    } else {
                                        val numSamples = sampleSize / bytesPerSample
                                        val bufferDurationUs = (numSamples * MICROSECONDS_PER_SECOND) / SAMPLING_RATE
                                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)

                                        presentationTimeUs += bufferDurationUs
                                    }
                                }
                            }

                            val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

                            when {
                                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                    val newFormat = mediaCodec.outputFormat
                                    trackIndex = mediaMuxer.addTrack(newFormat)
                                    mediaMuxer.start()
                                    muxerStarted = true
                                    retryCount = 0
                                }

                                outputBufferIndex >= 0 -> {
                                    val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)

                                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                        bufferInfo.size = 0
                                    }

                                    if (bufferInfo.size != 0 && outputBuffer != null) {
                                        outputBuffer.position(bufferInfo.offset)
                                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                                        if (trackIndex >= 0) {
                                            mediaMuxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                                        } else {
                                            appLogger.e("Track index is not set. Skipping writeSampleData.")
                                        }
                                    }

                                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                                    retryCount = 0

                                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                        sawOutputEOS = true
                                    }
                                }

                                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                                    retryCount++
                                    delay(RETRY_DELAY_IN_MILLIS)
                                }
                            }
                        }
                        if (retryCount >= MAX_RETRY_COUNT) {
                            appLogger.e("Reached maximum retries without receiving output from codec.")
                            success = false
                        }
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            appLogger.e("Could not convert wav to m4a: ${e.message}", throwable = e)
            success = false
        } finally {
            muxer?.let { safeMuxer ->
                if (muxerStarted) {
                    try {
                        safeMuxer.stop()
                    } catch (error: Exception) {
                        appLogger.e("Could not stop MediaMuxer: ${error.message}", throwable = error)
                        success = false
                    }
                }
                try {
                    safeMuxer.release()
                } catch (error: Exception) {
                    appLogger.e("Could not release MediaMuxer: ${error.message}", throwable = error)
                    success = false
                }
            }

            codec?.let { safeCodec ->
                if (codecStarted) {
                    try {
                        safeCodec.stop()
                    } catch (error: Exception) {
                        appLogger.e("Could not stop MediaCodec: ${error.message}", throwable = error)
                        success = false
                    }
                }
                try {
                    safeCodec.release()
                } catch (error: Exception) {
                    appLogger.e("Could not release MediaCodec: ${error.message}", throwable = error)
                    success = false
                }
            }
        }
        success
    }

    private fun cleanupFailedStart() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        releaseNativeRecorder()
        audioFocusHelper.abandonExclusive()
        originalOutputPath?.let(::deleteIfExists)
        m4aOutputPath?.let(::deleteIfExists)
        originalOutputPath = null
        m4aOutputPath = null
    }

    private fun deleteIfExists(path: Path) {
        if (kaliumFileSystem.exists(path)) kaliumFileSystem.delete(path)
    }

    companion object {
        fun getRecordingAudioFileName(): String = "wire-audio-${DateTimeUtil.currentInstant().fileDateTime()}.wav"
        fun getRecordingM4aAudioFileName(): String = "wire-audio-${DateTimeUtil.currentInstant().fileDateTime()}.m4a"

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
        const val MICROSECONDS_PER_SECOND = 1_000_000L
        const val MAX_RETRY_COUNT = 100
        const val RETRY_DELAY_IN_MILLIS = 100L
    }
}
