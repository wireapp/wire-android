/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock

@Composable
fun MDIndentedCodeBlock(indentedCodeBlock: IndentedCodeBlock) {
    Text(
        text = indentedCodeBlock.literal,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensions().spacing4x)
            .background(MaterialTheme.wireColorScheme.outlineVariant)
            .border(
                dimensions().spacing1x, MaterialTheme.wireColorScheme.outline,
                shape = RoundedCornerShape(dimensions().spacing4x)
            )
            .padding(dimensions().spacing4x)
    )
}

@Composable
fun MDFencedCodeBlock(fencedCodeBlock: FencedCodeBlock) {
    Text(
        text = fencedCodeBlock.literal,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensions().spacing4x)
            .background(MaterialTheme.wireColorScheme.outlineVariant)
            .border(
                dimensions().spacing1x, MaterialTheme.wireColorScheme.outline,
                shape = RoundedCornerShape(dimensions().spacing4x)
            )
            .padding(dimensions().spacing4x)
    )
}
