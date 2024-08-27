/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.messagecomposer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.type

/**
 * Enhances [Modifier] to handle the keyboard's dismiss event by providing a
 * mechanism to intercept the event based on a specified condition.
 *
 * This function intercepts the keyboard hide button event. When `shouldIntercept`
 * is true, it executes the provided callback to manage any necessary UI changes
 * (e.g., hiding attachments) before the keyboard is dismissed. If `shouldIntercept`
 * is false, the keyboard will dismiss normally.
 *
 * @param shouldIntercept A boolean flag that determines whether to intercept the
 *                        keyboard hide event. True to handle custom actions before
 *                        keyboard dismissal, false to allow normal dismissal.
 * @param handleOnBackPressed A lambda function to execute if the intercept condition
 *                            is met when the keyboard hide button is pressed. This
 *                            function should contain actions such as hiding UI elements.
 * @return [Modifier] The original [Modifier] enhanced with pre-intercept keyboard
 *                    event handling capabilities.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onKeyboardDismiss(
    shouldIntercept: Boolean,
    handleOnBackPressed: () -> Unit
): Modifier =
    this.onPreInterceptKeyBeforeSoftKeyboard { event: KeyEvent ->
        if (shouldIntercept && event.type == KeyEventType.KeyDown &&
            event.key.keyCode == KEYBOARD_HIDE_BUTTON_CODE
        ) {
            handleOnBackPressed.invoke()
            return@onPreInterceptKeyBeforeSoftKeyboard true // Consumes the event, preventing
            // further propagation.
        }
        false // Does not consume the event, allowing it to be handled by other components
    }

private const val KEYBOARD_HIDE_BUTTON_CODE = 17179869184 // The key code for the keyboard
