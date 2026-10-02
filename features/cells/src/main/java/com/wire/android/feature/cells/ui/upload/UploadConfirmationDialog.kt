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
package com.wire.android.feature.cells.ui.upload

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.domain.model.icon
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.logic.util.fileExtension

@Composable
internal fun UploadConfirmationDialog(
    fileNames: List<String>,
    destinationName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WireDialog(
        title = pluralStringResource(R.plurals.cells_upload_confirmation_title, fileNames.size, fileNames.size),
        text = stringResource(R.string.cells_upload_confirmation_destination, destinationName),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(R.string.cells_upload_confirmation_confirm),
            type = WireDialogButtonType.Primary,
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(R.string.cancel),
            onClick = onDismiss,
        ),
        content = {
            LazyColumn(modifier = Modifier.heightIn(max = dimensions().spacing200x)) {
                items(fileNames) { fileName ->
                    UploadConfirmationFileRow(fileName)
                }
            }
        },
    )
}

@Composable
private fun UploadConfirmationFileRow(fileName: String) {
    val fileType = remember(fileName) {
        fileName.fileExtension()?.let(AttachmentFileType::fromExtension) ?: AttachmentFileType.OTHER
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensions().spacing8x),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x),
    ) {
        Image(
            painter = painterResource(fileType.icon()),
            contentDescription = null,
            modifier = Modifier.size(dimensions().spacing24x),
        )
        Text(
            text = fileName,
            style = typography().body02,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@MultipleThemePreviews
@Composable
private fun PreviewUploadConfirmationDialog() {
    WireTheme {
        UploadConfirmationDialog(
            fileNames = listOf("2026 Q2 Marketing Budget.xlsx", "IMG_20.jpg", "2026 all projects report.pptx"),
            destinationName = "Shared Drive",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
