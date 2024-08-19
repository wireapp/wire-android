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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
open class WireModalSheetState<T : Any> internal constructor(
    density: Density,
    private val scope: CoroutineScope,
    initialValue: WireSheetValue<T> = WireSheetValue.Hidden,
    private val onDismissAction: () -> Unit = {}
) {
    val sheetState: SheetState = SheetState(
        density = density,
        skipPartiallyExpanded = true,
        initialValue = initialValue.originalValue,
        confirmValueChange = { true },
        skipHiddenState = false
    )

    var currentValue: WireSheetValue<T> by mutableStateOf(initialValue)
        private set

    fun show(value: T) {
        currentValue = WireSheetValue.Expanded(value)
    }

    fun hide(onComplete: suspend () -> Unit = {}) = scope.launch {
        sheetState.hide()
        currentValue = WireSheetValue.Hidden
        onComplete()
    }
    fun hide() = hide {}

    fun onDismissRequest() {
        onDismissAction()
        currentValue = WireSheetValue.Hidden
    }

    val isVisible
        get() = currentValue !is WireSheetValue.Hidden

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> saver(density: Density, scope: CoroutineScope): Saver<WireModalSheetState<T>, *> = Saver(
            save = {
                val isExpanded = it.currentValue is WireSheetValue.Expanded<T>
                val (isValueOfTypeUnit, value) = (it.currentValue as? WireSheetValue.Expanded<T>)?.let {
                    val isValueOfTypeUnit = it.value is Unit // Unit cannot be saved into Bundle, need to handle it separately
                    val value = if (isValueOfTypeUnit) null else it.value
                    isValueOfTypeUnit to value
                } ?: (false to null)
                listOf(isExpanded, isValueOfTypeUnit, value)
            },
            restore = { savedValue ->
                val isExpanded = savedValue[0] as Boolean
                val sheetValue = when (isExpanded) {
                    true -> {
                        val isValueOfTypeUnit = savedValue[1] as Boolean
                        if (isValueOfTypeUnit) {
                            WireSheetValue.Expanded(Unit as T)
                        } else {
                            val value = savedValue[2] as T
                            WireSheetValue.Expanded(value)
                        }
                    }
                    false -> WireSheetValue.Hidden
                }
                WireModalSheetState(density, scope, sheetValue)
            }
        )
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
 */
@Composable
fun <T : Any> rememberWireModalSheetState(
    initialValue: WireSheetValue<T> = WireSheetValue.Hidden,
    onDismissAction: () -> Unit = {}
): WireModalSheetState<T> {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return rememberSaveable(saver = WireModalSheetState.saver(density, scope)) {
        WireModalSheetState(density, scope, initialValue, onDismissAction)
    }
}

// to simplify execution of the sheet with Unit value
fun WireModalSheetState<Unit>.show() = this.show(Unit)
