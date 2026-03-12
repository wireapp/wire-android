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

package com.wire.android.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Preview(name = "Foundation Tokens", showBackground = true, widthDp = 360)
@PreviewTest
@Composable
fun FoundationTokensScreenshotPreview() {
    WireTheme {
        Surface(color = MaterialTheme.wireColorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions().spacing16x)
            ) {
                Text(
                    text = "UI Common foundation",
                    style = MaterialTheme.wireTypography.title01,
                    color = MaterialTheme.wireColorScheme.onBackground
                )
                VerticalSpace.x16()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(dimensions().spacing32x)
                            .clip(CircleShape)
                            .background(MaterialTheme.wireColorScheme.primary)
                    )
                    HorizontalSpace.x12()
                    Text(
                        text = "Primary color token",
                        style = MaterialTheme.wireTypography.body01,
                        color = MaterialTheme.wireColorScheme.onBackground
                    )
                }
                VerticalSpace.x16()
                WireDivider()
                VerticalSpace.x16()
                Text(
                    text = "Spacing scale",
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground
                )
                VerticalSpace.x8()
                Row(horizontalArrangement = Arrangement.Start) {
                    Box(
                        modifier = Modifier
                            .size(dimensions().spacing16x)
                            .background(MaterialTheme.wireColorScheme.positive)
                    )
                    HorizontalSpace.x8()
                    Box(
                        modifier = Modifier
                            .size(dimensions().spacing16x)
                            .background(MaterialTheme.wireColorScheme.warning)
                    )
                    HorizontalSpace.x8()
                    Box(
                        modifier = Modifier
                            .size(dimensions().spacing16x)
                            .background(MaterialTheme.wireColorScheme.error)
                    )
                }
            }
        }
    }
}
