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
package com.wire.android.ui.common.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
open class WireModalSheetState<T : Any>(
    density: Density,
    private val scope: CoroutineScope,
    private val keyboardController: SoftwareKeyboardController? = null,
    private val onDismissAction: () -> Unit = {},
    initialValue: WireSheetValue<T> = WireSheetValue.Hidden,
    skipPartiallyExpanded: Boolean = true,
) {
    val sheetState: SheetState = SheetState(
        density = density,
        skipPartiallyExpanded = skipPartiallyExpanded,
        initialValue = initialValue.originalValue,
        confirmValueChange = { true },
        skipHiddenState = false
    )

    var currentValue: WireSheetValue<T> by mutableStateOf(initialValue)
        private set

    fun show(value: T, hideKeyboard: Boolean = false) {
        scope.launch {
            // workaround for jumping bottom sheet when keyboard hides
            if (hideKeyboard && keyboardController != null) {
                keyboardController.hide()
                delay(DELAY_TO_SHOW_BOTTOM_SHEET_WHEN_KEYBOARD_IS_OPEN)
            }
            currentValue = WireSheetValue.Expanded(value)
        }
    }

    fun hide(onComplete: suspend () -> Unit = {}) = scope.launch {
        sheetState.hide()
        currentValue = WireSheetValue.Hidden
        onComplete()
    }

    fun hide() = hide {}

    /**
     *  To be used when the content needs to be updated while the sheet is already shown, e.g. when switching from "loading" state
     *  to the actual content. It's a workaround for the cases when the animations and/or drag gestures are disabled or limited
     *  and then bottom sheet doesn't update its peek height correctly after the content height changes.
     */
    fun updateContent() {
        scope.launch {
            sheetState.show()
        }
    }

    fun onDismissRequest() {
        onDismissAction()
        currentValue = WireSheetValue.Hidden
    }

    val isVisible
        get() = currentValue !is WireSheetValue.Hidden

    companion object {
        const val DELAY_TO_SHOW_BOTTOM_SHEET_WHEN_KEYBOARD_IS_OPEN = 300L
    }
}

@OptIn(ExperimentalMaterial3Api::class)
sealed class WireSheetValue<out T : Any>(val originalValue: SheetValue) {
    data object Hidden : WireSheetValue<Nothing>(SheetValue.Hidden)
    data class Expanded<T : Any>(val value: T) : WireSheetValue<T>(SheetValue.Expanded)
}

/**
 * Creates a [WireModalSheetState] that can be used to show and hide a [WireModalSheetLayout],
 * that can override the default dismiss action.
 *
 * @param initialValue The initial value of the sheet.
 * @param onDismissAction The action to be executed when the sheet is dismissed.
 * @param skipPartiallyExpanded Whether to skip the partially expanded state.
 */
@Composable
inline fun <reified T : Any> rememberWireModalSheetState(
    initialValue: WireSheetValue<T> = WireSheetValue.Hidden,
    skipPartiallyExpanded: Boolean = true,
    noinline onDismissAction: () -> Unit = {}
): WireModalSheetState<T> {
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return rememberSaveable(
        saver = WireModalSheetState.saver(
            density = density,
            softwareKeyboardController = softwareKeyboardController,
            onDismissAction = onDismissAction,
            scope = scope,
        )
    ) {
        WireModalSheetState(
            density = density,
            scope = scope,
            keyboardController = softwareKeyboardController,
            onDismissAction = onDismissAction,
            initialValue = initialValue,
            skipPartiallyExpanded = skipPartiallyExpanded,
        )
    }
}

// to simplify execution of the sheet with Unit value
fun WireModalSheetState<Unit>.show(hideKeyboard: Boolean = false) = this.show(Unit, hideKeyboard = hideKeyboard)
