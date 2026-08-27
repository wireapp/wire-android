/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.media.recording

import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsupportedRecordingCapabilitiesTest {
    @Test
    fun `unsupported platform adapters never report fake success`() = runTest {
        val path = "/tmp/audio.wav".toPath()

        assertEquals(RecordingCapabilityResult.Unsupported, UnsupportedAudioRecorder.start(1))
        assertEquals(RecordingCapabilityResult.Unsupported, UnsupportedAudioRecorder.stop())
        assertEquals(RecordingCapabilityResult.Unsupported, UnsupportedAudioRecorder.encode(path, path))
        assertEquals(RecordingCapabilityResult.Unsupported, UnsupportedRecordingPreview.toggle(path))
        assertEquals(RecordingCapabilityResult.Unsupported, UnsupportedAudioEffectsProcessor.process(AudioEffectsRequest(path, path)))
    }
}
