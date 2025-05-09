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

package com.wire.android.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.common.button.wireCheckBoxColors
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun WireLabelledCheckbox(
    label: String,
    checked: Boolean,
    onCheckClicked: ((Boolean) -> Unit),
    modifier: Modifier = Modifier,
    maxLine: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Visible,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    contentPadding: PaddingValues = PaddingValues(dimensions().spacing0x),
    checkboxEnabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = {
                    if (checkboxEnabled) {
                        onCheckClicked(!checked)
                    }
                }
            )
            .padding(contentPadding)
    ) {
        Checkbox(
            checked = checked,
            enabled = checkboxEnabled,
            colors = wireCheckBoxColors(),
            onCheckedChange = null // null since we are handling the click on parent
        )

        Spacer(modifier = Modifier.size(MaterialTheme.wireDimensions.spacing8x))

        Text(
            text = label,
            style = MaterialTheme.wireTypography.body01,
            overflow = overflow,
            maxLines = maxLine,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun WireCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Checkbox(
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        colors = wireCheckBoxColors(),
        modifier = modifier,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireLabelledCheckboxes() = WireTheme {
    Column {
        WireLabelledCheckbox(label = "checked", checked = true, checkboxEnabled = true, onCheckClicked = {})
        WireLabelledCheckbox(label = "not checked", checked = false, checkboxEnabled = true, onCheckClicked = {})
        WireLabelledCheckbox(label = "checked disabled", checked = true, checkboxEnabled = false, onCheckClicked = {})
        WireLabelledCheckbox(label = "not checked disabled", checked = false, checkboxEnabled = false, onCheckClicked = {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewWireCheckboxes() = WireTheme {
    Column {
        WireCheckbox(checked = true, enabled = true, onCheckedChange = {})
        WireCheckbox(checked = false, enabled = true, onCheckedChange = {})
        WireCheckbox(checked = true, enabled = false, onCheckedChange = {})
        WireCheckbox(checked = false, enabled = false, onCheckedChange = {})
    }
}
