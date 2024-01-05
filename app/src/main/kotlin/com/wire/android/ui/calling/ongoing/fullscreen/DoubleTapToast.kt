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
package com.wire.android.ui.calling.ongoing.fullscreen

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.model.Clickable
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography

private enum class Visibility {
    VISIBLE,
    GONE
}

@Composable
fun DoubleTapToast(
    modifier: Modifier,
    enabled: Boolean,
    text: String,
    onTap: () -> Unit
) {
    val transition = updateTransition(
        if (enabled) Visibility.VISIBLE else Visibility.GONE,
        label = "Jump to Top visibility animation"
    )
    val topOffset by transition.animateDp(label = "Jump to top offset animation") {
        if (it == Visibility.GONE) {
            -dimensions().spacing8x
        } else {
            dimensions().spacing8x
        }
    }
    if (topOffset > 0.dp) {
        Box(
            modifier = modifier
                .offset(x = 0.dp, y = topOffset)
                .background(
                    color = colorsScheme().primary,
                    shape = RoundedCornerShape(dimensions().corner14x)
                )
                .clickable(Clickable(true, onClick = { onTap() })),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modifier = Modifier.padding(
                    top = dimensions().spacing8x,
                    bottom = dimensions().spacing8x,
                    start = dimensions().spacing16x,
                    end = dimensions().spacing16x,
                ),
                style = MaterialTheme.wireTypography.label01,
                color = Color.White,
                text = text
            )
        }
    }
}

@Preview
@Composable
fun PreviewDoubleTapToast() {
    DoubleTapToast(
        modifier = Modifier,
        enabled = true,
        text = "Double tap to go back"
    ) { }
}
