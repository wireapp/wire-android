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
package com.wire.android.ui.home.messagecomposer.attachments

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import com.wire.android.R
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun AdditionalOptionButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = if (LocalInspectionMode.current) {
        IsFileSharingEnabledPreviewModel(true)
    } else {
        hiltViewModelScoped<IsFileSharingEnabledViewModelImpl, IsFileSharingEnabledArgs>(IsFileSharingEnabledArgs)
    }

    Box(modifier = modifier) {
        WireSecondaryIconButton(
            onButtonClicked = onClick,
            iconResource = R.drawable.ic_add,
            contentDescription = R.string.content_description_attachment_item,
            state = if (!viewModel.isFileSharingEnabled()) WireButtonState.Disabled
            else if (isSelected) WireButtonState.Selected else WireButtonState.Default,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAdditionalOptionButtonEnabled() {
    AdditionalOptionButton(isSelected = false, onClick = {})
}
