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
package com.wire.android.feature.cells.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import com.wire.android.ui.common.R as commonR

/**
 * Floating pill with the actions applied to every node picked in selection mode.
 * Each action shows its icon above a short label, so up to ~5 actions fit on a phone.
 */
@Composable
internal fun CellBulkActionsBar(
    actions: ImmutableList<CellBulkAction>,
    onActionClick: (CellBulkAction) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = colorsScheme().primary,
        contentColor = colorsScheme().onPrimary,
        shadowElevation = dimensions().spacing4x,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimensions().spacing12x, vertical = dimensions().spacing4x),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { action -> BulkActionItem(action, onClick = { onActionClick(action) }) }
        }
    }
}

@Composable
private fun BulkActionItem(action: CellBulkAction, onClick: () -> Unit) {
    val description = stringResource(action.contentDescriptionRes ?: action.labelRes)
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .widthIn(min = dimensions().spacing64x)
            .padding(horizontal = dimensions().spacing8x, vertical = dimensions().spacing8x),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
    ) {
        Icon(
            painter = painterResource(action.iconRes),
            contentDescription = null,
            modifier = Modifier.size(dimensions().spacing20x),
        )
        Text(
            text = stringResource(action.labelRes),
            style = typography().label03,
            maxLines = 1,
        )
    }
}

/**
 * One entry of [CellBulkActionsBar]. [labelRes] must stay short (one word) so several actions fit on a phone;
 * [contentDescriptionRes] carries the full wording for accessibility when the label is abbreviated.
 *
 * Only describes the action: what a click does is up to the screen, through `onActionClick`.
 */
internal enum class CellBulkAction(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    @StringRes val contentDescriptionRes: Int? = null,
) {
    Move(R.drawable.ic_folder, R.string.cells_bulk_action_move),
    Delete(commonR.drawable.ic_delete, R.string.cells_bulk_action_delete),
    MakeAvailableOffline(R.drawable.ic_arrow_down_circle, R.string.cells_bulk_action_offline, R.string.make_available_offline_label),
}

@PreviewMultipleThemes
@Composable
private fun PreviewCellBulkActionsBar() {
    WireTheme {
        CellBulkActionsBar(
            actions = persistentListOf(CellBulkAction.Move),
            onActionClick = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewCellBulkActionsBarAllActions() {
    WireTheme {
        CellBulkActionsBar(
            actions = CellBulkAction.entries.toPersistentList(),
            onActionClick = {},
        )
    }
}
