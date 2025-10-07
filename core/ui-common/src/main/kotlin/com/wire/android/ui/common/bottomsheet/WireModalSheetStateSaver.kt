/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.Density
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull

@Suppress("CyclomaticComplexMethod", "TooGenericExceptionCaught")
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> WireModalSheetState.Companion.saver(
    softwareKeyboardController: SoftwareKeyboardController?,
    noinline onDismissAction: () -> Unit,
    scope: CoroutineScope,
): Saver<WireModalSheetState<T>, SavedData> = Saver(
    save = {
        when (val currentValue = it.currentValue) {
            is WireSheetValue.Hidden -> SavedData.Hidden

            is WireSheetValue.Expanded<T> -> {
                when {
                    // expanded and with Unit value
                    currentValue.value is Unit ->
                        SavedData.Expanded(null) // Unit cannot be saved, pass null instead

                    // expanded and with value that can be saved normally
                    canBeSaved(currentValue.value) ->
                        SavedData.Expanded(currentValue.value)

                    // expanded and with value that can be serialized to Bundle
                    T::class.serializerOrNull() != null ->
                        try {
                            SavedData.Expanded(Bundlizer.bundle(T::class.serializer(), currentValue.value))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            SavedData.Hidden // hidden because key cannot be serialized to Bundle properly
                        }

                    else -> SavedData.Hidden // hidden because value cannot be saved properly
                }
            }
        }
    },
    restore = { savedValue ->
        WireModalSheetState(
            scope = scope,
            keyboardController = softwareKeyboardController,
            onDismissAction = onDismissAction,
            initialValue = when (savedValue) {
                is SavedData.Hidden -> WireSheetValue.Hidden

                is SavedData.Expanded -> when {
                    // Unit value
                    T::class.isInstance(Unit) -> WireSheetValue.Expanded(Unit as T)

                    // regular value that can be restored normally
                    savedValue.value is T -> WireSheetValue.Expanded(savedValue.value)

                    // value that can be deserialized from Bundle
                    savedValue.value is Bundle && T::class.serializerOrNull() != null ->
                        try {
                            WireSheetValue.Expanded(Bundlizer.unbundle(T::class.serializer(), savedValue.value))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            WireSheetValue.Hidden // hidden because value cannot be deserialized from Bundle properly
                        }

                    else -> WireSheetValue.Hidden // hidden because value cannot be restored properly
                }
            }
        )
    }
)

@Parcelize
sealed interface SavedData : Parcelable {
    @Parcelize
    data object Hidden : SavedData

    @Parcelize
    data class Expanded(val value: @RawValue Any?) : SavedData
}
