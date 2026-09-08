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

package com.wire.android.ui.common.banner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.common.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ViewerAccessBannerTest {

    @get:Rule
    val composeTestRule by lazy { createComposeRule() }

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenViewerAccessBanner_whenRendered_thenViewerAccessTextIsDisplayed() {
        composeTestRule.setContent {
            WireTestTheme {
                ViewerAccessBanner(onCloseClick = {})
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.conversation_viewer_access_banner))
            .assertIsDisplayed()
    }

    @Test
    fun givenViewerAccessBanner_whenCloseIsClicked_thenOnCloseClickIsCalled() {
        var closeClickCount = 0
        composeTestRule.setContent {
            WireTestTheme {
                ViewerAccessBanner(onCloseClick = { closeClickCount++ })
            }
        }

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.content_description_close_access_info))
            .performClick()

        assertEquals(1, closeClickCount)
    }
}
