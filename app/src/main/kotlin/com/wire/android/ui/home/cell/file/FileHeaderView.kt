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
package com.wire.android.ui.home.cell.file

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun FileHeaderView(
    extension: String,
    size: Long,
    modifier: Modifier = Modifier,
) {
    val fileType = remember(extension) { AttachmentFileType.fromExtension(extension) }
    val sizeString = remember(size) { size.formattedFileSize() }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            modifier = Modifier.size(16.dp),
            painter = painterResource(id = fileType.icon()),
            contentDescription = null,
        )
        Text(
            text = "${extension.uppercase()} ($sizeString)",
            style = typography().subline01,
            color = colorsScheme().secondaryText,
        )
    }
}

@Suppress("MagicNumber")
private fun Long.formattedFileSize() = when {
    this >= 1 shl 20 -> "%.1f MB".format(toDouble() / (1 shl 20))
    this >= 1 shl 10 -> "%.0f kB".format(toDouble() / (1 shl 10))
    else -> "$this B"
}

@PreviewMultipleThemes
@Composable
private fun PreviewFileHeader() {
    WireTheme {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FileHeaderView(
                extension = "PDF",
                size = 1241235,
            )
            FileHeaderView(
                extension = "DOCX",
                size = 6796203,
            )
            FileHeaderView(
                extension = "ZIP",
                size = 512746,
            )
            FileHeaderView(
                extension = "OTHER",
                size = 78238296,
            )
        }
    }
}
