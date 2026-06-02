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

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.R
import com.wire.android.ui.WireTestTheme
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import org.junit.Rule
import org.junit.Test

class SecurityClassificationBannerTest {

    @get:Rule
    val composeTestRule by lazy { createComposeRule() }

    @Test
    fun givenSecurityClassificationBanner_whenRendered_thenSemanticsDescriptionIsPlainText() {
        val expectedDescription = InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(R.string.conversation_details_is_classified)

        composeTestRule.setContent {
            WireTestTheme {
                SecurityClassificationBannerForConversation(
                    conversationId = ConversationId("conversation_id", "domain"),
                    viewModel = FakeSecurityClassificationViewModel(SecurityClassificationType.CLASSIFIED)
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(expectedDescription)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(expectedDescription)))
        composeTestRule.onNodeWithText(expectedDescription).assertDoesNotExist()
    }

    private class FakeSecurityClassificationViewModel(
        private val state: SecurityClassificationType
    ) : SecurityClassificationViewModel {

        override fun state(): SecurityClassificationType = state
    }
}
