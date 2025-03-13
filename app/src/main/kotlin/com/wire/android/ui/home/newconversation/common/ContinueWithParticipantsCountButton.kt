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
package com.wire.android.ui.home.newconversation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun ContinueWithParticipantsCountButton(
    modifier: Modifier = Modifier,
    selectedParticipantsCount: Int = 0,
    leadingIcon: @Composable (() -> Unit)? = null,
    onContinue: () -> Unit,
) {
    val countText = pluralStringResource(
        R.plurals.label_x_participants,
        selectedParticipantsCount,
        selectedParticipantsCount
    )
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = dimensions().spacing16x)
            .height(dimensions().groupButtonHeight)
    ) {
        WirePrimaryButton(
            text = "${stringResource(R.string.label_continue)} ($countText)",
            leadingIcon = leadingIcon,
            onClick = onContinue,
            state = if (selectedParticipantsCount > 0) WireButtonState.Default else WireButtonState.Disabled,
            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewContinueWithParticipantsCountButton() {
    ContinueWithParticipantsCountButton(
        selectedParticipantsCount = 3,
        onContinue = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSContinueWithParticipantsCountButtonDisabled() {
    ContinueWithParticipantsCountButton(
        selectedParticipantsCount = 0,
        onContinue = {},
    )
}
