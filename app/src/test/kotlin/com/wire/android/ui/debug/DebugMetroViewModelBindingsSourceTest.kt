/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.debug

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DebugMetroViewModelBindingsSourceTest {

    @Test
    fun givenDebugViewModels_whenInspectingCreation_thenFeatureOwnsNarrowContracts() {
        val graph = source("DebugInfoViewModelGraph.kt")
        val bindings = source("DebugMetroViewModelBindings.kt")
        val assistedViewModel = source("conversation/DebugConversationViewModel.kt")

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(graph.contains("debugConversationViewModel(args)"))
        assertTrue(bindings.contains("@ViewModelKey(SecurityProvidersViewModel::class)"))
        assertTrue(
            assistedViewModel.contains(
                "@WireAssistedViewModelBinding(DebugInfoManualViewModelFactoryGroup::class)"
            )
        )
        assertTrue(assistedViewModel.contains("class DebugConversationViewModel @AssistedInject constructor"))
        assertTrue(assistedViewModel.contains("@Assisted val args: DebugConversationScreenNavArgs"))
        assertTrue(
            assistedViewModel.contains(
                "fun create(args: DebugConversationScreenNavArgs): DebugConversationViewModel"
            )
        )
        assertFalse(File("src/main/kotlin/com/wire/android/ui/debug/DebugInfoViewModelFactory.kt").exists())
    }

    private fun source(name: String) = File("src/main/kotlin/com/wire/android/ui/debug/$name").readText()
}
