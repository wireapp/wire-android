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

import android.app.Application
import android.media.AudioRecord
import android.media.MediaCodec
import com.wire.android.config.TestDispatcherProvider
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.sink
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AudioMediaRecorderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val readersInFlight = AtomicInteger(0)
    private val maxReadersInFlight = AtomicInteger(0)

    @Before
    fun setUp() {
        mockkConstructor(AudioRecord::class)
        every { anyConstructed<AudioRecord>().startRecording() } returns Unit
        every { anyConstructed<AudioRecord>().read(any<ByteArray>(), any(), any()) } answers {
            val inFlight = readersInFlight.incrementAndGet()
            maxReadersInFlight.accumulateAndGet(inFlight) { a, b -> maxOf(a, b) }
            Thread.sleep(READ_DURATION_MS)
            readersInFlight.decrementAndGet()
            thirdArg<Int>()
        }
    }

    @After
    fun tearDown() {
        unmockkConstructor(AudioRecord::class)
    }

    @Test
    fun givenRecordingInProgress_whenStartRecordingIsCalledAgain_thenOnlyOneThreadReadsFromTheAudioRecord() = runBlocking {
        val recorder = createRecorder()
        recorder.setUp(assetLimitInMegabyte = 10)

        val first = recorder.startRecording()
        val second = recorder.startRecording()
        delay(RECORD_DURATION_MS)
        withTimeout(STOP_TIMEOUT_MS) { recorder.stop() }

        assertTrue(first)
        assertTrue(second)
        assertEquals(1, maxReadersInFlight.get())
    }

    @Test
    fun givenTwoThreadsStartingAtTheSameInstant_whenStartRecordingRuns_thenOnlyOneThreadReadsFromTheAudioRecord() = runBlocking {
        repeat(CONCURRENT_START_ITERATIONS) {
            val recorder = createRecorder()
            recorder.setUp(assetLimitInMegabyte = 10)
            val barrier = CyclicBarrier(2)
            val starters = List(2) {
                thread {
                    barrier.await()
                    recorder.startRecording()
                }
            }
            starters.forEach { it.join() }
            delay(CONCURRENT_START_RECORD_MS)
            withTimeout(STOP_TIMEOUT_MS) { recorder.stop() }
        }

        assertEquals(1, maxReadersInFlight.get())
    }

    @Test
    fun givenStoppedRecorder_whenStopIsCalledAgain_thenItReturns() = runBlocking {
        val recorder = createRecorder()
        recorder.setUp(assetLimitInMegabyte = 10)
        recorder.startRecording()
        delay(RECORD_DURATION_MS)

        withTimeout(STOP_TIMEOUT_MS) {
            recorder.stop()
            recorder.stop()
        }
    }

    @Test
    fun givenRecordedAudio_whenStopped_thenWavHeaderSizesMatchTheFileSize() = runBlocking {
        val recorder = createRecorder()
        recorder.setUp(assetLimitInMegabyte = 10)
        recorder.startRecording()
        delay(RECORD_DURATION_MS)
        withTimeout(STOP_TIMEOUT_MS) { recorder.stop() }

        assertHeaderMatchesFile(recorder.originalOutputPath!!.toFile())
    }

    @Test
    fun givenStartDuringStop_whenStoppedAgain_thenStopWaitsForTheNewWriter() = runBlocking {
        val firstReadEntered = CountDownLatch(1)
        val firstGate = CountDownLatch(1)
        val secondReadEntered = CountDownLatch(1)
        val secondGate = CountDownLatch(1)
        val reads = AtomicInteger()
        every { anyConstructed<AudioRecord>().read(any<ByteArray>(), any(), any()) } answers {
            when (reads.incrementAndGet()) {
                1 -> {
                    firstReadEntered.countDown()
                    firstGate.await()
                }
                2 -> {
                    secondReadEntered.countDown()
                    secondGate.await()
                }
                else -> Thread.sleep(READ_DURATION_MS)
            }
            thirdArg<Int>()
        }
        val recorder = createRecorder()
        recorder.setUp(assetLimitInMegabyte = 10)

        recorder.startRecording()
        firstReadEntered.await()
        val firstStop = launch(Dispatchers.IO) { recorder.stop() }
        delay(STOP_SETTLE_MS) // firstStop is now suspended in join on the blocked writer
        recorder.startRecording() // the old writer is cancelled but still running, so this starts a second one
        secondReadEntered.await()
        firstGate.countDown()
        withTimeout(STOP_TIMEOUT_MS) { firstStop.join() }

        val secondStop = launch(Dispatchers.IO) { recorder.stop() }
        delay(STOP_SETTLE_MS)
        val returnedBeforeWriterExited = secondStop.isCompleted
        secondGate.countDown()
        withTimeout(STOP_TIMEOUT_MS) { secondStop.join() }

        assertFalse(returnedBeforeWriterExited, "stop() returned without joining the writer started during the previous stop")
    }

    @Test
    fun givenSizeLimitReachedWhileTheEventCollectorIsBlocked_whenStopped_thenWavHeaderSizesMatchTheFileSize() = runBlocking {
        val recorder = createRecorder()
        recorder.setUp(assetLimitInMegabyte = 0)
        val limitFired = CompletableDeferred<RecordAudioDialogState>()
        // Proves the limit fired: it receives the event on another thread while the blocked collector holds the writer in emit.
        val probe = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
            recorder.getMaxFileSizeReached().collect { limitFired.complete(it) }
        }
        val blockedCollector = launch(start = CoroutineStart.UNDISPATCHED) { recorder.getMaxFileSizeReached().collect { } }
        recorder.startRecording()
        // Proves only that the header is written on the size-limit exit path: the writer leaves its loop on the limit,
        // and the blocked collector may resume once stop() suspends in join.
        limitFired.asCompletableFuture().get(STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        withTimeout(STOP_TIMEOUT_MS) { recorder.stop() }
        probe.cancel()
        blockedCollector.cancel()

        assertHeaderMatchesFile(recorder.originalOutputPath!!.toFile())
    }

    @Test
    fun givenInputFileThatCannotBeRead_whenConvertingToM4a_thenItReturnsFalse() = runBlocking {
        val recorder = createRecorder()
        recorder.setUp(assetLimitInMegabyte = 10)

        val result = recorder.convertWavToM4a(File(tempFolder.root, "missing.wav").absolutePath)

        assertFalse(result)
    }

    @Test
    fun givenConversionCancelled_whenConvertingToM4a_thenCancellationExceptionPropagates() {
        mockkStatic(MediaCodec::class)
        try {
            every { MediaCodec.createEncoderByType(any()) } throws CancellationException("cancelled")
            val wav = tempFolder.newFile("input.wav").apply { writeBytes(ByteArray(AudioMediaRecorder.HEADER_SIZE + PCM_BYTES)) }
            val recorder = createRecorder()
            recorder.setUp(assetLimitInMegabyte = 10)

            assertThrows<CancellationException> { runBlocking { recorder.convertWavToM4a(wav.absolutePath) } }
        } finally {
            unmockkStatic(MediaCodec::class)
        }
    }

    private fun assertHeaderMatchesFile(file: File) {
        val bytes = file.readBytes()
        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertTrue(bytes.size > AudioMediaRecorder.HEADER_SIZE)
        assertEquals(bytes.size - AudioMediaRecorder.CHUNK_ID_SIZE, header.getInt(AudioMediaRecorder.CHUNK_SIZE_OFFSET))
        assertEquals(bytes.size - AudioMediaRecorder.HEADER_SIZE, header.getInt(AudioMediaRecorder.SUBCHUNK2_SIZE_OFFSET))
    }

    private fun createRecorder(): AudioMediaRecorder {
        val fileSystem = mockk<KaliumFileSystem>()
        every { fileSystem.tempFilePath(any()) } answers { tempFolder.root.resolve(firstArg<String>()).toOkioPath() }
        every { fileSystem.sink(any<Path>(), any()) } answers { firstArg<Path>().toFile().sink() }
        return AudioMediaRecorder(fileSystem, TestDispatcherProvider(Dispatchers.IO))
    }

    private companion object {
        const val PCM_BYTES = 1024
        const val READ_DURATION_MS = 5L
        const val RECORD_DURATION_MS = 300L
        const val STOP_TIMEOUT_MS = 5_000L
        const val STOP_SETTLE_MS = 100L
        const val CONCURRENT_START_ITERATIONS = 30
        const val CONCURRENT_START_RECORD_MS = 20L
    }
}
