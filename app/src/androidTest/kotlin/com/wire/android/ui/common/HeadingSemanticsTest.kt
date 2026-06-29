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
package com.wire.android.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.common.rowitem.BigSectionHeader
import com.wire.android.ui.common.rowitem.EmptyListContent
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.newauthentication.login.NewAuthTitle
import org.junit.Rule
import org.junit.Test

class HeadingSemanticsTest {

    @get:Rule
    val composeTestRule by lazy { createComposeRule() }

    @Test
    fun givenTopAppBarTitle_whenRendered_thenSemanticsHeadingIsSet() {
        composeTestRule.setContent {
            WireTestTheme {
                WireCenterAlignedTopAppBar(title = "Conversations")
            }
        }

        composeTestRule
            .onNodeWithText("Conversations")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun givenSectionHeader_whenRendered_thenSemanticsHeadingIsSet() {
        composeTestRule.setContent {
            WireTestTheme {
                SectionHeader(name = "Settings")
            }
        }

        composeTestRule
            .onNodeWithText("SETTINGS")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun givenBigSectionHeader_whenRendered_thenSemanticsHeadingIsSet() {
        composeTestRule.setContent {
            WireTestTheme {
                BigSectionHeader(name = "Today")
            }
        }

        composeTestRule
            .onNodeWithText("Today")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun givenEmptyListTitle_whenRendered_thenSemanticsHeadingIsSet() {
        composeTestRule.setContent {
            WireTestTheme {
                EmptyListContent(
                    title = "No conversations yet",
                    text = "Start a conversation",
                    footer = {},
                    modifier = Modifier
                )
            }
        }

        composeTestRule
            .onNodeWithText("No conversations yet")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun givenNewAuthTitle_whenRendered_thenSemanticsHeadingIsSet() {
        composeTestRule.setContent {
            WireTestTheme {
                NewAuthTitle(title = "Log in")
            }
        }

        composeTestRule
            .onNodeWithText("Log in")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}
