/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.search

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SearchAssistedFactorySourceTest {

    @Test
    fun givenSearchRuntimeArguments_whenInspectingCreation_thenGeneratedFactoryOwnsTheirBindings() {
        val graph = source("SearchViewModelGraph.kt")
        val searchAppsViewModel = source("apps/SearchAppsViewModel.kt")
        val searchUserViewModel = source("users/SearchUserViewModel.kt")

        assertTrue(graph.contains("@WireAssistedViewModelFactoryGroup"))
        assertTrue(searchAppsViewModel.contains("@WireAssistedViewModelBinding(SearchManualViewModelFactoryGroup::class)"))
        assertTrue(searchAppsViewModel.contains("fun create(protocolInfo: Conversation.ProtocolInfo?): SearchAppsViewModel"))
        assertTrue(searchUserViewModel.contains("@WireAssistedViewModelBinding(SearchManualViewModelFactoryGroup::class)"))
        assertTrue(
            searchUserViewModel.contains(
                "fun create(conversationId: ConversationId?, onlyConnectedContacts: Boolean): SearchUserViewModel"
            )
        )
        assertFalse(File("src/main/kotlin/com/wire/android/search/SearchMetroViewModelBindings.kt").exists())
    }

    private fun source(name: String) =
        File("src/main/kotlin/com/wire/android/search/$name").readText()
}
