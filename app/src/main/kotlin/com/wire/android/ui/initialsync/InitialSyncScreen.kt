/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.initialsync

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.wire.android.R
import com.wire.android.ui.authentication.initialsync.InitialSyncRouteContent
import com.wire.android.ui.initialSyncViewModel

@Composable
internal fun InitialSyncRouteScreen(
    onSyncCompleted: (shouldMoveToBackground: Boolean) -> Unit,
) {
    InitialSyncRouteContent(
        viewModel = initialSyncViewModel(),
        topBarTitle = stringResource(R.string.migration_title),
        message = AnnotatedString(stringResource(R.string.migration_message)),
        icon = painterResource(R.drawable.ic_migration),
        onSyncCompleted = onSyncCompleted,
    )
}
