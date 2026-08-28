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
package com.wire.android.ui.common.textfield

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.wire.android.ui.WireTestTheme
import org.junit.Rule
import org.junit.Test

class WireTextFieldAutofillTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenLoginAutofill_whenRendered_thenEmailAndUsernameContentTypeIsSet() {
        composeTestRule.setContent {
            WireTestTheme {
                WireTextField(
                    textState = rememberTextFieldState(),
                    autoFillType = WireAutoFillType.Login,
                    testTag = FIELD_TAG,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(FIELD_TAG, useUnmergedTree = true)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentType,
                    requireNotNull(WireAutoFillType.Login.contentType),
                )
            )
    }

    @Test
    fun givenPasswordAutofill_whenRendered_thenPasswordContentTypeIsSet() {
        composeTestRule.setContent {
            WireTestTheme {
                WirePasswordTextField(
                    textState = rememberTextFieldState(),
                    autoFill = true,
                    testTag = FIELD_TAG,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(FIELD_TAG, useUnmergedTree = true)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentType,
                    requireNotNull(WireAutoFillType.Password.contentType),
                )
            )
    }

    private companion object {
        const val FIELD_TAG = "autofillField"
    }
}
