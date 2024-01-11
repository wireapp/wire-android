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
package com.wire.android.ui.legalhold.banner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.stringWithStyledArgs

@Composable
fun LegalHoldSubjectBanner(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
        modifier = modifier
            .clip(RoundedCornerShape(dimensions().spacing12x))
            .background(colorsScheme().surface)
            .border(
                width = dimensions().spacing1x,
                shape = RoundedCornerShape(dimensions().spacing12x),
                color = colorsScheme().error
            )
            .clickable(onClick = onClick)
            .heightIn(min = 26.dp)
            .padding(
                horizontal = dimensions().spacing12x,
                vertical = dimensions().spacing4x
            )
    ) {
        LegalHoldIndicator()
        val resources = LocalContext.current.resources
        Text(
            text = resources.stringWithStyledArgs(
                stringResId = R.string.legal_hold_subject_to,
                normalStyle = typography().label01,
                argsStyle = typography().label02.copy(textDecoration = TextDecoration.Underline),
                normalColor = colorsScheme().onSurface,
                argsColor = colorsScheme().onSurface,
                resources.getString(R.string.legal_hold_label)
            ),
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldSubjectBanner() {
    WireTheme {
        LegalHoldSubjectBanner()
    }
}
