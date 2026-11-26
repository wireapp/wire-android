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
import com.wire.android.config.TestDispatcherProvider
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun givenSizeLimitReachedAndNobodyCollectingTheEvent_whenStopped_thenWavHeaderSizesMatchTheFileSize() = runBlocking {
        val recorder = createRecorder()
        recorder.setUp(assetLimitInMegabyte = 0)
        val collector = launch(start = CoroutineStart.UNDISPATCHED) { recorder.getMaxFileSizeReached().collect { } }
        recorder.startRecording()
        // Blocks this thread, so the collector cannot resume and the writer stays suspended in emit until stop() cancels it.
        Thread.sleep(RECORD_DURATION_MS)
        withTimeout(STOP_TIMEOUT_MS) { recorder.stop() }
        collector.cancel()

        assertHeaderMatchesFile(recorder.originalOutputPath!!.toFile())
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
        const val READ_DURATION_MS = 5L
        const val RECORD_DURATION_MS = 300L
        const val STOP_TIMEOUT_MS = 5_000L
        const val CONCURRENT_START_ITERATIONS = 30
        const val CONCURRENT_START_RECORD_MS = 20L
    }
}
