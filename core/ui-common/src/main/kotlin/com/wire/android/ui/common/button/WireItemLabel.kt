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

package com.wire.android.ui.common.button

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun WireItemLabel(
    text: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues =
    PaddingValues(horizontal = dimensions().spacing6x, vertical = dimensions().spacing2x),
    shape: Shape = RoundedCornerShape(dimensions().spacing6x),
    contentDescription: String = text
) = Box(
    modifier = modifier
        .border(width = 1.dp, color = MaterialTheme.wireColorScheme.outline, shape = shape)
        .padding(contentPadding)
        .semantics { this.contentDescription = contentDescription }
        .wrapContentWidth()
        .wrapContentHeight(),
) {
    Text(
        modifier = Modifier.clearAndSetSemantics { },
        text = text,
        style = MaterialTheme.wireTypography.label02,
    )
}

@Preview(name = "Wire item label", showBackground = true)
@Composable
fun PreviewWireItemLabel() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        WireItemLabel(text = "pending")
    }
}
