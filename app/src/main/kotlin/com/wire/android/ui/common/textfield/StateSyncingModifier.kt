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
package com.wire.android.ui.common.textfield

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.input.TextFieldValue
import io.github.esentsov.PackagePrivate

/**
 * Enables us to temporarily still use TextFieldValue and onValueChanged callback instead of TextFieldState directly,
 * also allows us to get selection updates as by default BasicTextField2 callback only gives a String without selection.
 * @sample androidx.compose.foundation.samples.BasicTextFieldWithValueOnValueChangeSample
 * TODO: Remove this class once all WireTextField usages are migrated to use TextFieldState.
 */
@PackagePrivate
internal class StateSyncingModifier(
    private val state: TextFieldState,
    private val value: TextFieldValue,
    private val onValueChanged: (TextFieldValue) -> Unit,
) : ModifierNodeElement<StateSyncingModifierNode>() {

    override fun create(): StateSyncingModifierNode = StateSyncingModifierNode(state, onValueChanged)

    override fun update(node: StateSyncingModifierNode) {
        node.update(value, onValueChanged)
    }

    @Suppress("EqualsAlwaysReturnsTrueOrFalse")
    override fun equals(other: Any?): Boolean = false

    override fun hashCode(): Int = state.hashCode()

    @Suppress("EmptyFunctionBlock")
    override fun InspectorInfo.inspectableProperties() {}
}

@OptIn(ExperimentalFoundationApi::class)
@PackagePrivate
internal class StateSyncingModifierNode(
    private val state: TextFieldState,
    private var onValueChanged: (TextFieldValue) -> Unit,
) : Modifier.Node(), ObserverModifierNode {
    override val shouldAutoInvalidate: Boolean
        get() = false

    fun update(value: TextFieldValue, onValueChanged: (TextFieldValue) -> Unit) {
        this.onValueChanged = onValueChanged
        if (value.text != state.text.toString() || value.selection != state.text.selection) {
            state.edit {
                if (value.text != state.text.toString()) {
                    replace(0, length, value.text)
                }
                if (value.selection != state.text.selection) {
                    selection = value.selection
                }
            }
            onValueChanged(value)
        }
    }

    override fun onAttach() {
        observeTextState(fireOnValueChanged = false)
    }

    override fun onObservedReadsChanged() {
        observeTextState()
    }

    private fun observeTextState(fireOnValueChanged: Boolean = true) {
        lateinit var text: TextFieldCharSequence
        observeReads {
            text = state.text
        }
        if (fireOnValueChanged) {
            val newValue = TextFieldValue(text.toString(), text.selection, text.composition)
            onValueChanged(newValue)
        }
    }
}
