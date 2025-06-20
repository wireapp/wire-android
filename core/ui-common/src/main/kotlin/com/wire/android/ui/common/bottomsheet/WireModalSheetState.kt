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

import android.os.Bundle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.Density
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull

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

    fun onDismissRequest() {
        onDismissAction()
        currentValue = WireSheetValue.Hidden
    }

    val isVisible
        get() = currentValue !is WireSheetValue.Hidden

    companion object {
        const val DELAY_TO_SHOW_BOTTOM_SHEET_WHEN_KEYBOARD_IS_OPEN = 300L

        @Suppress("TooGenericExceptionCaught")
        @OptIn(InternalSerializationApi::class)
        inline fun <reified T : Any> saver(
            density: Density,
            softwareKeyboardController: SoftwareKeyboardController?,
            noinline onDismissAction: () -> Unit,
            scope: CoroutineScope
        ): Saver<WireModalSheetState<T>, List<Any>> = Saver(
            save = {
                when (it.currentValue) {
                    is WireSheetValue.Hidden -> listOf(false) // hidden
                    is WireSheetValue.Expanded<T> -> {
                        val value = (it.currentValue as WireSheetValue.Expanded<T>).value
                        when {
                            value is Unit -> // expanded and with Unit value
                                listOf(true, SavedType.Unit)

                            canBeSaved(value) -> // expanded and non-Unit value that can be saved normally
                                listOf(true, SavedType.Regular, value)

                            T::class.serializerOrNull() != null -> // expanded and with non-Unit value that can be serialized
                                try {
                                    val serializedBundleValue = Bundlizer.bundle(T::class.serializer(), value)
                                    listOf(true, SavedType.SerializedBundle, serializedBundleValue)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    listOf(false) // hidden because value cannot be serialized properly
                                }

                            else -> listOf(false) // hidden because value cannot be saved
                        }
                    }
                }
            },
            restore = { savedValue ->
                val isExpanded = savedValue[0] as Boolean
                val sheetValue = when (isExpanded) {
                    true -> when (savedValue[1] as SavedType) {
                        SavedType.Unit -> WireSheetValue.Expanded(Unit as T)

                        SavedType.Regular -> WireSheetValue.Expanded(savedValue[2] as T)

                        SavedType.SerializedBundle ->
                            WireSheetValue.Expanded(Bundlizer.unbundle(T::class.serializer(), savedValue[2] as Bundle))
                    }

                    false -> WireSheetValue.Hidden
                }
                WireModalSheetState(density, scope, softwareKeyboardController, onDismissAction, sheetValue)
            }
        )
    }
}

enum class SavedType { Unit, Regular, SerializedBundle }

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
inline fun <reified T : Any> rememberWireModalSheetState(
    initialValue: WireSheetValue<T> = WireSheetValue.Hidden,
    skipPartiallyExpanded: Boolean = true,
    noinline onDismissAction: () -> Unit = {}
): WireModalSheetState<T> {
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // TODO: we can use rememberSaveable instead of remember to save the state but first we need to make sure that we don't store too much,
    //  especially for conversations and messages to not keep such data unencrypted anywhere
    return remember {
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
