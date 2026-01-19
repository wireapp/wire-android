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
package com.wire.android.ui.home.conversations

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.attachment.FailedAttachmentDialogState
import kotlinx.coroutines.launch
import com.wire.android.ui.common.R as commonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FailedAttachmentDialog(
    state: FailedAttachmentDialogState,
    onRetryUpload: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    if (state is FailedAttachmentDialogState.Visible) {
        ModalBottomSheet(
            modifier = modifier,
            onDismissRequest = { onDismiss() },
            sheetState = sheetState
        ) {
            if (state.showRetryOption) {
                WireDivider(modifier = Modifier.fillMaxWidth())
                MenuItem(
                    iconRes = commonR.drawable.ic_refresh,
                    text = stringResource(R.string.failed_attachment_retry_upload),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            onRetryUpload()
                        }
                    }
                )
            }
            WireDivider(modifier = Modifier.fillMaxWidth())
            MenuItem(
                iconRes = commonR.drawable.ic_filled_delete,
                text = stringResource(R.string.failed_attachment_remove),
                highlightColor = colorsScheme().error,
                onClick = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        onRemoveAttachment()
                    }
                }
            )
            WireDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MenuItem(
    @DrawableRes
    iconRes: Int,
    text: String,
    highlightColor: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions().spacing48x)
            .clickable {
                onClick()
            }
            .padding(horizontal = dimensions().spacing16x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = highlightColor ?: colorsScheme().onSurface,
        )
        Spacer(modifier = Modifier.width(dimensions().spacing16x))
        Text(
            text = text,
            style = typography().body01,
            color = highlightColor ?: colorsScheme().onSurface,
        )
    }
}
