/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class HomeMetroViewModelBindingsSourceTest {

    @Test
    fun givenHomeViewModels_whenInspectingBindings_thenFeatureOwnsNarrowCreationContracts() {
        val bindings = source("HomeMetroViewModelBindings.kt")
        val listViewModel = source("conversationslist/ConversationListViewModel.kt")
        val homeViewModel = source("HomeViewModel.kt")

        assertTrue(bindings.contains("object HomeMetroViewModelBindings"))
        assertTrue(bindings.contains("factory: ConversationListViewModelImpl.Factory"))
        assertTrue(bindings.contains("factory.create("))
        assertTrue(listViewModel.contains("class ConversationListViewModelImpl @AssistedInject constructor"))
        assertTrue(listViewModel.contains("@AssistedFactory\n    interface Factory"))
        assertTrue(homeViewModel.contains("@CurrentAccount private val currentAccount: UserId"))
        assertFalse(homeViewModel.contains("CurrentSessionFlowUseCase"))
        assertFalse(bindings.contains("HomeViewModelFactory"))
        assertFalse(File("src/main/kotlin/com/wire/android/ui/home/HomeViewModelFactory.kt").exists())
    }

    private fun source(name: String) = File("src/main/kotlin/com/wire/android/ui/home/$name").readText()
}
