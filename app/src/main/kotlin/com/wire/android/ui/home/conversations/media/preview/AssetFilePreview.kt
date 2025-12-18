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
package com.wire.android.ui.home.conversations.media.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.FileSizeFormatter
import java.util.Locale

@Composable
fun AssetFilePreview(assetName: String, sizeInBytes: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimensions().spacing32x)
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Icon(
                modifier = Modifier.size(dimensions().spacing80x),
                painter = painterResource(id = R.drawable.ic_file),
                contentDescription = assetName,
                tint = MaterialTheme.wireColorScheme.secondaryText
            )
            Text(
                modifier = Modifier.padding(bottom = dimensions().spacing8x),
                text = assetName.split(".").last().uppercase(Locale.getDefault()),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.wireTypography.title05
            )
        }
        VerticalSpace.x16()
        Text(
            assetName,
            style = MaterialTheme.wireTypography.title02,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        VerticalSpace.x8()
        val fileSizeFormatted = FileSizeFormatter(context).formatSize(sizeInBytes)
        Text(
            fileSizeFormatted,
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.secondaryText
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewAssetFilePreview(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(400.dp)
            .height(800.dp)
    ) {
        WireTheme {
            AssetFilePreview(
                assetName = "very long file naaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaame.png",
                sizeInBytes = 1500
            )
        }
    }
}
