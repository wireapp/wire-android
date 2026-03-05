/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.search.filter.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews

@Composable
fun FooterButtons(
    onRemoveAll: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    hasChanges: Boolean = false
) {
    Column {
        Spacer(modifier.height(dimensions().spacing12x))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensions().spacing12x),
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x)
        ) {
            WireSecondaryButton(
                text = stringResource(R.string.button_remove_all_label),
                onClick = onRemoveAll,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = dimensions().spacing14x)
            )

            WirePrimaryButton(
                text = stringResource(R.string.save_label),
                onClick = onSave,
                modifier = Modifier.weight(1f),
                state = if (hasChanges) WireButtonState.Default else WireButtonState.Disabled,
                contentPadding = PaddingValues(vertical = dimensions().spacing14x)
            )
        }
        Spacer(Modifier.height(dimensions().spacing8x))
    }
}

@MultipleThemePreviews
@Composable
fun FooterButtonsPreview() {
    FooterButtons(
        onRemoveAll = {},
        onSave = {},
        hasChanges = true
    )
}
