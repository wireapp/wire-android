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
package com.wire.android.ui.home.newconversation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WirePromotionDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme

@Composable
fun ChannelNotAvailableDialog(
    onCreateTeam: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WirePromotionDialog(
        title = stringResource(R.string.channel_not_available_dialog_title),
        description = stringResource(R.string.channel_not_available_dialog_description),
        buttonLabel = stringResource(R.string.channel_not_available_dialog_create_team_button),
        onDismiss = onDismiss,
        onButtonClick = onCreateTeam,
        modifier = modifier,
    )
}

@Composable
@MultipleThemePreviews
fun PreviewWireDialogWithWaves() = WireTheme {
    Box(modifier = Modifier.padding(dimensions().dialogCardMargin)) {
        ChannelNotAvailableDialog(
            onDismiss = {},
            onCreateTeam = {},
        )
    }
}
