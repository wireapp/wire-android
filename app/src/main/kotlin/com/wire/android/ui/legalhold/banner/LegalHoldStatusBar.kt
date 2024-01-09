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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import com.wire.android.R
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun LegalHoldStatusBar(
    legalHoldState: LegalHoldUIState,
    onPendingClicked: () -> Unit,
) {
    val isVisible = legalHoldState !is LegalHoldUIState.None
    AnimatedVisibility(
        visible = isVisible,
        enter = expandIn(initialSize = { fullSize -> IntSize(fullSize.width, 0) }),
        exit = shrinkOut(targetSize = { fullSize -> IntSize(fullSize.width, 0) })
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (legalHoldState) {
                is LegalHoldUIState.Pending -> LegalHoldPendingContent(onPendingClicked)
                is LegalHoldUIState.Active -> LegalHoldActiveContent()
                is LegalHoldUIState.None -> {}
            }
        }
    }
}

@Composable
private fun LegalHoldPendingContent(onPendingClicked: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x, Alignment.CenterHorizontally),
        modifier = Modifier
            .padding(horizontal = dimensions().spacing8x, vertical = dimensions().spacing4x)
            .clickable(onClick = onPendingClicked)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_warning_circle),
            contentDescription = null,
            tint = MaterialTheme.wireColorScheme.error,
            modifier = Modifier.size(dimensions().spacing12x)
        )
        Text(
            text = stringResource(id = R.string.legal_hold_is_pending_label).uppercase(),
            color = MaterialTheme.wireColorScheme.error,
            style = MaterialTheme.wireTypography.title03,
        )
    }
}

@Composable
private fun LegalHoldActiveContent() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing8x, vertical = dimensions().spacing4x)
    ) {
        LegalHoldIndicator()
        Text(
            text = stringResource(id = R.string.legal_hold_is_active_label).uppercase(),
            color = MaterialTheme.wireColorScheme.secondaryText,
            style = MaterialTheme.wireTypography.title03,
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldStatusBarActive() {
    WireTheme {
        LegalHoldStatusBar(legalHoldState = LegalHoldUIState.Active) {}
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldStatusBarPending() {
    WireTheme {
        LegalHoldStatusBar(legalHoldState = LegalHoldUIState.Pending) {}
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldStatusBarNone() {
    WireTheme {
        LegalHoldStatusBar(legalHoldState = LegalHoldUIState.None) {}
    }
}
