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
package com.wire.android.mediaplayer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class MediaPlayerMetroViewModelBindingsTest {

    @Test
    fun givenRuntimeArguments_whenCreatingVideoPlayerViewModel_thenFocusedFactoryReceivesExactArguments() {
        val expectedViewModel = mockk<VideoPlayerViewModel>()
        val factory = mockk<VideoPlayerViewModel.Factory> {
            every { create("local-path", "content-url", "file-name") } returns expectedViewModel
        }
        val adapter = MediaPlayerMetroViewModelBindings.mediaPlayerManualViewModelFactory(factory) as MediaPlayerManualViewModelFactory

        val actualViewModel = adapter.videoPlayerViewModel("local-path", "content-url", "file-name")

        assertSame(expectedViewModel, actualViewModel)
        verify(exactly = 1) { factory.create("local-path", "content-url", "file-name") }
    }
}
