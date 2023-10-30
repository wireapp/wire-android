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
package com.wire.android.ui.settings.devices.e2ei

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState

@Composable
fun E2eiCertificateDetailsBottomSheet(
    sheetState: WireModalSheetState,
    onCopyToClipboard: () -> Unit,
    onDownload: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    WireModalSheetLayout(sheetState = sheetState, coroutineScope = coroutineScope) {
        MenuModalSheetContent(
            header = MenuModalSheetHeader.Gone,
            menuItems = buildList {
                add {
                    CreateCertificateSheetItem(
                        title = stringResource(R.string.e2ei_certificate_details_copy_to_clipboard),
                        icon = R.drawable.ic_copy,
                        onClicked = onCopyToClipboard,
                        enabled = true
                    )
                }
                add {
                    CreateCertificateSheetItem(
                        title = stringResource(R.string.e2ei_certificate_details_download),
                        icon = R.drawable.ic_download,
                        onClicked = onDownload,
                        enabled = true
                    )
                }
            }
        )
    }
}

@Composable
private fun CreateCertificateSheetItem(
    title: String,
    icon: Int,
    onClicked: () -> Unit,
    enabled: Boolean = true,
) {
    MenuBottomSheetItem(
        title = title,
        onItemClick = onClicked,
        icon = {
            MenuItemIcon(
                id = icon,
                contentDescription = "",
            )
        },
        enabled = enabled
    )
}

@Preview
@Composable
fun PreviewE2eiCertificateDetailsBottomSheet() {
    E2eiCertificateDetailsBottomSheet(
        sheetState = WireModalSheetState(),
        onCopyToClipboard = { },
        onDownload = { }
    )
}
