/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.meetings.ui.create

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MeetingsNavigation3SourceTest {

    @Test
    fun `typed entries do not depend on generated destinations or Nav2`() {
        val source = source("MeetingsNavigation3Entries.kt")

        assertFalse(source.contains("com.ramcosta.composedestinations"))
        assertFalse(source.contains("WireNavigator"))
        assertFalse(source.contains("SavedStateHandle"))
        assertTrue(source.contains("wireEntry<NewMeetingDetailsRoute>"))
        assertTrue(source.contains("wireEntry<NewMeetingParticipantsRoute>"))
        assertTrue(source.contains("wireViewModelStoreOwner(WireViewModelOwner.Flow(flowId))"))
        assertTrue(source.contains("viewModelStoreOwner = flowOwner"))
        assertTrue(source.contains("newMeetingFlowViewModel(route.type, route.meetingId, route.flowId)"))
    }

    @Test
    fun `typed meeting view model path passes arguments through Metro assisted factory`() {
        val graph = File(
            "src/main/java/com/wire/android/feature/meetings/ui/MeetingsViewModelGraph.kt"
        ).readText()
        val viewModel = File(
            "src/main/java/com/wire/android/feature/meetings/ui/create/NewMeetingViewModel.kt"
        ).readText()

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("newMeetingViewModel(navArgs)"))
        assertTrue(viewModel.contains("@WireAssistedViewModelBinding(MeetingsManualViewModelFactoryGroup::class)"))
        assertTrue(viewModel.contains("@Assisted val navArgs: NewMeetingNavArgs"))
        assertTrue(viewModel.contains("fun create(navArgs: NewMeetingNavArgs): NewMeetingViewModelImpl"))
    }

    private fun source(name: String) =
        File("src/main/java/com/wire/android/feature/meetings/ui/create/$name").readText()
}
