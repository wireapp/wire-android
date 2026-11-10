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
package com.wire.android.ui.home.conversations.migration

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.wire.kalium.logic.data.id.ConversationId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MigratedConversationRedirectTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val replaced = mutableListOf<ConversationId>()

    @Test
    fun givenMigratedConversation_whenRecomposedRepeatedly_thenReplacesOnlyOnce() {
        var recompositions by mutableIntStateOf(0)
        composeTestRule.setContent {
            val current = recompositions
            MigratedConversationRedirect(TARGET) { replaced += it; current }
        }

        repeat(RECOMPOSITIONS) {
            composeTestRule.runOnIdle { recompositions++ }
            composeTestRule.waitForIdle()
        }

        assertEquals(listOf(TARGET), replaced)
    }

    @Test
    fun givenNoMigratedConversation_whenRecomposed_thenDoesNotReplace() {
        var recompositions by mutableIntStateOf(0)
        composeTestRule.setContent {
            val current = recompositions
            MigratedConversationRedirect(null) { replaced += it; current }
        }

        repeat(RECOMPOSITIONS) {
            composeTestRule.runOnIdle { recompositions++ }
            composeTestRule.waitForIdle()
        }

        assertEquals(emptyList<ConversationId>(), replaced)
    }

    @Test
    fun givenMigratedConversationChanges_whenTargetChanges_thenReplacesOncePerTarget() {
        var target by mutableStateOf<ConversationId?>(null)
        composeTestRule.setContent {
            MigratedConversationRedirect(target) { replaced += it }
        }

        composeTestRule.runOnIdle { target = TARGET }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { target = OTHER_TARGET }
        composeTestRule.waitForIdle()

        assertEquals(listOf(TARGET, OTHER_TARGET), replaced)
    }

    private companion object {
        const val RECOMPOSITIONS = 3
        val TARGET = ConversationId("target", "wire.com")
        val OTHER_TARGET = ConversationId("other-target", "wire.com")
    }
}
