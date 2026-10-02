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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.kalium.cells.domain.CellUploadItem
import com.wire.kalium.cells.domain.CellUploadState
import com.wire.android.ui.common.R as commonR

/**
 * Compact persistent bar shown while a Shared Drive upload batch exists, so the state stays visible
 * while the user browses away from [UploadStatusBottomSheet]. Reads [uploads] straight from
 * [UploadStatusViewModel], which itself only forwards `CellUploadCoordinator.uploads` — this component
 * owns no upload state of its own. Tapping it opens the full [UploadStatusBottomSheet].
 */
@Composable
internal fun UploadStatusIndicator(
    uploads: List<CellUploadItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uploads.isEmpty()) return

    val isActive = uploads.any { it.state is CellUploadState.Queued || it.state is CellUploadState.Uploading }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing12x),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x),
    ) {
        if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimensions().spacing20x),
                color = colorsScheme().primary,
                trackColor = colorsScheme().primaryVariant,
                strokeWidth = dimensions().spacing2x,
                strokeCap = StrokeCap.Round,
            )
        }
        Text(
            text = uploadStatusTitle(uploads),
            style = typography().body02,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(commonR.drawable.ic_chevron_right),
            contentDescription = stringResource(R.string.content_description_open_upload_status),
            tint = colorsScheme().secondaryText,
            modifier = Modifier.size(dimensions().spacing16x),
        )
    }
}
