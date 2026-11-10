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
package com.wire.android.ui.home.meetings

import android.content.ClipboardManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.wire.android.feature.meetings.ui.create.NewMeetingContent
import com.wire.android.feature.meetings.ui.create.NewMeetingState
import com.wire.android.feature.meetings.ui.create.NewMeetingType
import com.wire.android.ui.WireTestTheme
import com.wire.kalium.logic.data.id.MeetingId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Instant
import com.wire.android.feature.meetings.R as meetingsR
import com.wire.android.ui.common.R as commonR

@RunWith(Parameterized::class)
class NewMeetingPasswordInputTest(private val meetingType: NewMeetingType) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun meetingTypes(): List<Array<NewMeetingType>> = listOf(arrayOf(NewMeetingType.MeetNow), arrayOf(NewMeetingType.Schedule))
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var errorColor = Color.Unspecified
    private lateinit var inputModeManager: InputModeManager
    private val initialState = NewMeetingState(
        startTime = Instant.parse("2026-11-10T12:00:00Z"),
        endTime = Instant.parse("2026-11-10T13:00:00Z"),
        tzid = "UTC",
        isGuestLinkSharingEnabled = true,
        initialLoading = NewMeetingState.InitialLoadingState.Loaded,
    )

    @Test
    fun givenGuestLinkSharingEnabled_whenRendered_thenOptionalPasswordActionsAndConfirmationAreAvailable() {
        renderMeeting()

        assertOptionalPasswordAndActionsAreAvailable()
    }

    @Test
    fun givenGuestLinkSharingDisabled_whenRendered_thenPasswordInputsAndActionsAreAbsent() {
        renderMeeting(state = initialState.copy(isGuestLinkSharingEnabled = false))

        assertPasswordInputsAndActionsAreAbsent()
    }

    @Test
    fun givenEmptyPassword_whenGenerating_thenCallbackUpdatesPasswordAndEnablesCopyBeforeStarting() {
        val passwordState = TextFieldState()
        var generateCount = 0
        renderMeeting(
            passwordState = passwordState,
            onGenerateRandomPassword = {
                generateCount++
                passwordState.setTextAndPlaceCursorAtEnd("Abcd12!?")
            },
        )

        composeTestRule.onNodeWithText(context.getString(commonR.string.generate_password_button_text))
            .performScrollTo()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, generateCount)
            assertEquals("Abcd12!?", passwordState.text.toString())
        }
        composeTestRule.onNodeWithText(context.getString(commonR.string.copy_password_button_text)).assertIsEnabled()
    }

    @Test
    fun givenEnteredPassword_whenCopyingBeforeStarting_thenPasswordIsCopied() {
        val password = "Abcd12!?"
        renderMeeting(passwordState = TextFieldState(password))

        composeTestRule.onNodeWithText(context.getString(commonR.string.copy_password_button_text))
            .performScrollTo()
            .performClick()

        composeTestRule.runOnIdle {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            assertEquals(password, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
    }

    @Test
    fun givenInvalidPassword_whenRendered_thenHelperUsesErrorColor() {
        renderMeeting(passwordState = TextFieldState("short"), state = initialState.copy(invalidPassword = true))

        assertHelperUsesErrorColor(meetingsR.string.new_meeting_password_error)
    }

    @Test
    fun givenPasswordFocused_whenPressingImeNext_thenConfirmationReceivesFocus() {
        renderMeeting()

        composeTestRule.runOnIdle {
            assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard))
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { inputModeManager.inputMode == InputMode.Keyboard }

        val passwordInput = composeTestRule.onNodeWithTag("meetingPasswordInput").performScrollTo()
        val passwordCenter = passwordInput.fetchSemanticsNode().boundsInWindow.center
        val locationOnScreen = IntArray(2)
        val locationInWindow = IntArray(2)
        composeTestRule.runOnIdle {
            composeTestRule.activity.window.decorView.getLocationOnScreen(locationOnScreen)
            composeTestRule.activity.window.decorView.getLocationInWindow(locationInWindow)
        }
        // Inject through Android so the tap also switches the host out of keyboard navigation mode.
        assertTrue(
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(
                (passwordCenter.x + locationOnScreen[0] - locationInWindow[0]).toInt(),
                (passwordCenter.y + locationOnScreen[1] - locationInWindow[1]).toInt(),
            )
        )
        composeTestRule.waitUntil(timeoutMillis = 5_000) { inputModeManager.inputMode == InputMode.Touch }
        passwordInput.assertIsFocused().performImeAction()

        composeTestRule.onNodeWithTag("meetingConfirmPasswordInput").assertIsFocused()
    }

    @Test
    fun givenMismatchedPasswords_whenRendered_thenMismatchHelperUsesErrorColor() {
        renderMeeting(
            passwordState = TextFieldState("ValidPassword1!"),
            confirmPasswordState = TextFieldState("DifferentPassword1!"),
            state = initialState.copy(passwordsDoNotMatch = true),
        )

        assertHelperUsesErrorColor(meetingsR.string.new_meeting_password_mismatch_error)
    }

    private fun assertHelperUsesErrorColor(helperText: Int) {
        val textLayouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText(context.getString(helperText))
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayouts) }

        assertTrue(textLayouts.isNotEmpty())
        assertEquals(errorColor, textLayouts.single().layoutInput.style.color)
    }

    private fun assertOptionalPasswordAndActionsAreAvailable() {
        val label = context.getString(meetingsR.string.new_meeting_password_input_label)
        composeTestRule.onNodeWithTag("meetingPasswordInput")
            .performScrollTo()
            .assertIsDisplayed()
            .assert(hasContentDescription(label, ignoreCase = true))
        composeTestRule.onNodeWithText(label, ignoreCase = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(meetingsR.string.new_meeting_password_error))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(commonR.string.copy_password_button_text))
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(commonR.string.generate_password_button_text))
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onNodeWithTag("meetingConfirmPasswordInput")
            .performScrollTo()
            .assertIsDisplayed()
            .assert(
                hasContentDescription(
                    context.getString(meetingsR.string.new_meeting_confirm_password_input_label),
                    ignoreCase = true,
                )
            )
    }

    private fun assertPasswordInputsAndActionsAreAbsent() {
        composeTestRule.onNodeWithTag("meetingPasswordInput").assertDoesNotExist()
        composeTestRule.onNodeWithTag("meetingConfirmPasswordInput").assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(commonR.string.copy_password_button_text)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(commonR.string.generate_password_button_text)).assertDoesNotExist()
    }

    private fun renderMeeting(
        passwordState: TextFieldState = TextFieldState(),
        confirmPasswordState: TextFieldState = TextFieldState(),
        state: NewMeetingState = initialState,
        onGenerateRandomPassword: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            WireTestTheme {
                errorColor = MaterialTheme.colorScheme.error
                inputModeManager = LocalInputModeManager.current
                NewMeetingContent(
                    state = state,
                    titleState = TextFieldState("Meeting"),
                    passwordState = passwordState,
                    confirmPasswordState = confirmPasswordState,
                    type = meetingType,
                    onGenerateRandomPassword = onGenerateRandomPassword,
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { composeTestRule.activity.hasWindowFocus() }
    }
}

class EditMeetingPasswordInputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenGuestLinkSharingEnabled_whenEditMeetingIsRendered_thenPasswordInputsAndActionsAreAbsent() {
        composeTestRule.setContent {
            WireTestTheme {
                NewMeetingContent(
                    state = NewMeetingState(
                        startTime = Instant.parse("2026-11-10T12:00:00Z"),
                        endTime = Instant.parse("2026-11-10T13:00:00Z"),
                        tzid = "UTC",
                        isGuestLinkSharingEnabled = true,
                        initialLoading = NewMeetingState.InitialLoadingState.Loaded,
                    ),
                    titleState = TextFieldState("Meeting"),
                    passwordState = TextFieldState(),
                    confirmPasswordState = TextFieldState(),
                    type = NewMeetingType.Edit(MeetingId("meeting", "domain")),
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.onNodeWithTag("meetingPasswordInput").assertDoesNotExist()
        composeTestRule.onNodeWithTag("meetingConfirmPasswordInput").assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(commonR.string.copy_password_button_text)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(commonR.string.generate_password_button_text)).assertDoesNotExist()
    }
}
