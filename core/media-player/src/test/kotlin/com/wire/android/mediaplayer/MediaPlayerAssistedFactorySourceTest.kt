/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.mediaplayer

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MediaPlayerAssistedFactorySourceTest {

    @Test
    fun givenVideoRouteArguments_whenInspectingCreation_thenNarrowAssistedFactoryOwnsOnlyThoseArguments() {
        val viewModel = source("VideoPlayerViewModel.kt")
        val graph = source("MediaPlayerViewModelGraph.kt")

        assertTrue(viewModel.contains("class VideoPlayerViewModel @AssistedInject constructor"))
        assertTrue(viewModel.contains("sessionFactory: AndroidVideoPlaybackSessionFactory"))
        assertTrue(viewModel.contains("@Assisted val localPath: String?"))
        assertTrue(viewModel.contains("@Assisted val contentUrl: String?"))
        assertTrue(viewModel.contains("@Assisted val fileName: String?"))
        assertTrue(viewModel.contains("@AssistedFactory\n    interface Factory"))
        assertTrue(viewModel.contains("@WireAssistedViewModelBinding(MediaPlayerManualViewModelFactoryGroup::class)"))
        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertFalse(viewModel.contains("Context"))
        assertFalse(viewModel.contains("ExoPlayer"))
        assertFalse(viewModel.contains("java.io.File"))
        assertFalse(File("src/main/kotlin/com/wire/android/mediaplayer/MediaPlayerMetroViewModelBindings.kt").exists())
        assertFalse(File("src/main/kotlin/com/wire/android/mediaplayer/MediaPlayerViewModelFactory.kt").exists())
    }

    private fun source(name: String) =
        File("src/main/kotlin/com/wire/android/mediaplayer/$name").readText()
}
