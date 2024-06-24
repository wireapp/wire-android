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
@file:OptIn(ExperimentalComposeUiApi::class)

package com.wire.android.ui.common.textfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import io.github.esentsov.PackagePrivate

@OptIn(ExperimentalComposeUiApi::class)
@PackagePrivate
@Composable
internal fun autoFillModifier(type: WireAutoFillType, onFill: ((String) -> Unit)) = if (type.autoFillTypes.isNotEmpty()) {
    val autofillNode = AutofillNode(
        autofillTypes = type.autoFillTypes,
        onFill = onFill,
    )
    LocalAutofillTree.current += autofillNode
    Modifier
        .fillBounds(autofillNode)
        .defaultOnFocusAutoFill(LocalAutofill.current, autofillNode)
} else {
    Modifier
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.fillBounds(autofillNode: AutofillNode) = this.then(
    Modifier.onGloballyPositioned { autofillNode.boundingBox = it.boundsInWindow() }
)

private fun Modifier.defaultOnFocusAutoFill(autofill: Autofill?, autofillNode: AutofillNode): Modifier =
    then(
        Modifier.onFocusChanged {
            focusState ->
                if (focusState.isFocused) {
                    autofill?.requestAutofillForNode(autofillNode)
                } else {
                    autofill?.cancelAutofillForNode(autofillNode)
                }
        }
    )

@Composable
fun clearAutofillTree() = LocalAutofillTree.current.children.clear()

enum class WireAutoFillType(val autoFillTypes: List<AutofillType>) {
    None(emptyList()),
    Login(listOf(AutofillType.EmailAddress, AutofillType.Username)),
    Password(listOf(AutofillType.Password)),
}
