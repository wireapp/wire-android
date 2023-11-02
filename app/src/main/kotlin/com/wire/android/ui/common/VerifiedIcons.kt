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
package com.wire.android.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R

@Composable
fun ProteusVerifiedIcon(
    modifier: Modifier = Modifier,
    @StringRes contentDescriptionId: Int = R.string.label_client_verified
) {
    Image(
        modifier = modifier.padding(start = dimensions().spacing4x),
        painter = painterResource(id = R.drawable.ic_certificate_valid_proteus),
        contentDescription = stringResource(contentDescriptionId)
    )
}

@Composable
fun MLSVerifiedIcon(
    modifier: Modifier = Modifier,
    @StringRes contentDescriptionId: Int = R.string.label_client_verified
) {
    Image(
        modifier = modifier.padding(start = dimensions().spacing4x),
        painter = painterResource(id = R.drawable.ic_certificate_valid_mls),
        contentDescription = stringResource(contentDescriptionId)
    )
}
