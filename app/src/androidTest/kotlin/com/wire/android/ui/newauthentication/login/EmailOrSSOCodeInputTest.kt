/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.test.junit4.createComposeRule
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.common.textfield.WireAutoFillType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
class EmailOrSSOCodeInputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenInputEnabled_whenRendered_thenAutofillTypeIsLogin() {
        val autofillTree = renderInput(enabled = true)

        composeTestRule.runOnIdle {
            assertEquals(
                setOf(WireAutoFillType.Login.autoFillTypes),
                autofillTree.children.values.map { it.autofillTypes }.toSet(),
            )
        }
    }

    @Test
    fun givenInputDisabled_whenRendered_thenAutofillTypeIsNone() {
        val autofillTree = renderInput(enabled = false)

        composeTestRule.runOnIdle {
            assertTrue("A disabled input must not register for autofill", autofillTree.children.isEmpty())
        }
    }

    private fun renderInput(enabled: Boolean): AutofillTree {
        val autofillTree = AutofillTree()
        val textState = TextFieldState("wire-fd994b20-b9af-11ec-ae36-00163e9b33ca")

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAutofillTree provides autofillTree,
                LocalAutofill provides null,
            ) {
                WireTestTheme {
                    EmailOrSSOCodeInput(
                        userIdentifierState = textState,
                        error = null,
                        enabled = enabled,
                    )
                }
            }
        }
        return autofillTree
    }
}
