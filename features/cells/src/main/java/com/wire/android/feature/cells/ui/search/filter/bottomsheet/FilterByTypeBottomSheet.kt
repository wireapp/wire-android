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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.search.filter.data.FilterTypeUi
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.cells.data.MIMEType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterByTypeBottomSheet(
    sheetState: SheetState,
    items: List<FilterTypeUi>,
    onDismiss: () -> Unit,
    onSave: (List<FilterTypeUi>) -> Unit,
    onRemoveFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var itemsState by remember { mutableStateOf(items) }

    val hasChanges = itemsState.any { tag ->
        val initial = items.first { it.id == tag.id }
        tag.selected != initial.selected
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions().spacing700x)
                .padding(bottom = dimensions().spacing16x)
        ) {
            Text(
                text = stringResource(R.string.bottom_sheet_title_filter_by_type),
                style = typography().title02,
                modifier = Modifier.padding(
                    horizontal = dimensions().spacing16x,
                    vertical = dimensions().spacing16x
                )
            )

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = dimensions().spacing4x)
            ) {
                items(itemsState, key = { it.id }) { item ->
                    FilterRow(
                        label = stringResource(item.label),
                        iconRes = item.iconRes,
                        checked = item.selected,
                        onCheckedChange = { checked ->
                            itemsState = itemsState.map {
                                if (it.id == item.id) it.copy(selected = checked) else it
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }

            Spacer(Modifier.height(dimensions().spacing12x))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions().spacing16x),
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x)
            ) {
                WireSecondaryButton(
                    text = stringResource(R.string.button_remove_filter),
                    onClick = {
                        itemsState = itemsState.map { it.copy(selected = false) }
                        onRemoveFilter()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = dimensions().spacing14x)
                )

                WirePrimaryButton(
                    text = stringResource(R.string.save_label),
                    onClick = { onSave(itemsState) },
                    modifier = Modifier.weight(1f),
                    state = if (hasChanges) WireButtonState.Default else WireButtonState.Disabled,
                    contentPadding = PaddingValues(vertical = dimensions().spacing14x)
                )
            }

            Spacer(Modifier.height(dimensions().spacing8x))
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    iconRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onCheckedChange(!checked) }
            .padding(
                horizontal = dimensions().spacing16x,
                vertical = dimensions().spacing8x
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(dimensions().spacing20x)
        )

        Spacer(Modifier.width(dimensions().spacing8x))

        Text(
            text = label,
            style = typography().body01,
            modifier = Modifier.weight(1f)
        )

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@MultipleThemePreviews
@Composable
fun PreviewFilterByTypeBottomSheet() {
    val sampleItems = listOf(
        FilterTypeUi(id = "1", label = R.string.filter_images_type, iconRes = android.R.drawable.ic_menu_gallery, selected = true, mimeType = MIMEType.IMAGE),
        FilterTypeUi(
            id = "2",
            label = R.string.filter_videos_type,
            iconRes = android.R.drawable.ic_menu_slideshow,
            selected = false,
            mimeType = MIMEType.VIDEO
        ),
        FilterTypeUi(
            id = "3",
            label = R.string.filter_documents_type,
            iconRes = android.R.drawable.ic_menu_edit,
            selected = true,
            mimeType = MIMEType.DOCUMENT
        ),
        FilterTypeUi(id = "4", label = R.string.filter_audio_type, iconRes = android.R.drawable.ic_media_play, selected = false, mimeType = MIMEType.AUDIO),
        FilterTypeUi(id = "6", label = R.string.filter_spreadsheets_type, iconRes = android.R.drawable.ic_menu_edit, selected = false, mimeType = MIMEType.EXCEL),
        FilterTypeUi(id = "7", label = R.string.filter_pdf_type, iconRes = android.R.drawable.ic_menu_view, selected = false, mimeType = MIMEType.PDF),
    )
    WireTheme {
        FilterByTypeBottomSheet(
            items = sampleItems,
            onDismiss = {},
            onSave = {},
            onRemoveFilter = {},
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        )
    }
}
