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
package com.wire.android.ui.common.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.wire.android.ui.common.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun WireOutlinedCard(
    title: String,
    textContent: String,
    modifier: Modifier = Modifier,
    mainActionButtonText: String? = null,
    onMainActionClick: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorsScheme().secondaryButtonSelected),
        border = BorderStroke(dimensions().spacing1x, colorsScheme().secondaryButtonSelectedOutline),
    ) {
        Row(
            modifier = Modifier.padding(
                start = dimensions().spacing8x,
                top = dimensions().spacing8x,
                end = dimensions().spacing8x,
            )
        ) {
            trailingIcon?.invoke()
            Text(
                modifier = Modifier.padding(start = dimensions().spacing8x),
                text = title,
                style = MaterialTheme.wireTypography.body02,
                color = colorsScheme().onBackground
            )
        }
        Text(
            modifier = Modifier.padding(
                start = dimensions().spacing8x,
                top = dimensions().spacing4x,
                end = dimensions().spacing8x
            ),
            text = textContent,
            style = MaterialTheme.wireTypography.body01,
            color = colorsScheme().onBackground
        )

        if (!mainActionButtonText.isNullOrEmpty()) {
            onMainActionClick?.let { onClick ->
                WireSecondaryButton(
                    modifier = Modifier
                        .padding(dimensions().spacing8x)
                        .height(dimensions().createTeamInfoCardButtonHeight),
                    text = mainActionButtonText,
                    onClick = onClick,
                    fillMaxWidth = false,
                    minSize = dimensions().buttonSmallMinSize,
                    minClickableSize = dimensions().buttonMinClickableSize,
                )
            }
        }
        if (onMainActionClick == null) Spacer(modifier = Modifier.height(dimensions().spacing8x))
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewWireOutlinedCard() = WireTheme {
    WireOutlinedCard(
        title = "Your team doesn't use apps yet.",
        textContent = "To enable apps, please contact your team admin.",
        mainActionButtonText = "Learn more",
        trailingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_attention),
                contentDescription = null,
                tint = colorsScheme().onBackground
            )
        }
    )
}
