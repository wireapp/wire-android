/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.media.player

import kotlin.test.Test
import kotlin.test.assertEquals

class AudioPlaybackStateStoreTest {
    @Test
    fun `state history keeps message position duration and playing state independently`() {
        val store = AudioPlaybackStateStore<String>()

        store.update("first", AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.Playing))
        store.update("first", AudioStateUpdate.PositionChanged(200))
        store.update("first", AudioStateUpdate.TotalTimeChanged(1_000))
        store.update("second", AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.Paused))

        assertEquals(AudioMediaPlayingState.Playing, store.state("first")?.audioMediaPlayingState)
        assertEquals(200, store.state("first")?.currentPositionInMs)
        assertEquals(AudioState.TotalTimeInMs.Known(1_000), store.state("first")?.totalTimeInMs)
        assertEquals(AudioMediaPlayingState.Paused, store.state("second")?.audioMediaPlayingState)
        assertEquals(200, store.restoredPosition("first"))
    }

    @Test
    fun `completed message restores from the beginning`() {
        val store = AudioPlaybackStateStore<String>()
        store.update("message", AudioStateUpdate.PositionChanged(800))
        store.update("message", AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.Completed))

        assertEquals(0, store.restoredPosition("message"))
    }
}
