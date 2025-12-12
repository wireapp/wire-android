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
package com.wire.android.feature.cells.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme

@Composable
fun ErrorScreen(
    onRetry: () -> Unit,
    isConnectionError: Boolean = true,
    modifier: Modifier = Modifier,
    titleDefault: String = stringResource(R.string.file_list_load_error_title),
    titleConnectionError: String = stringResource(R.string.file_list_load_network_error_title),
    descriptionDefault: String = stringResource(R.string.file_list_load_error),
    descriptionConnectionError: String = stringResource(R.string.file_list_load_network_error)
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensions().spacing16x),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )

        Text(
            text = if (isConnectionError) titleConnectionError else titleDefault,
            textAlign = TextAlign.Center,
            style = typography().title01,
            color = if (isConnectionError) colorsScheme().onBackground else colorsScheme().error,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isConnectionError) descriptionConnectionError else descriptionDefault,
            textAlign = TextAlign.Center,
            color = if (isConnectionError) colorsScheme().onBackground else colorsScheme().error,
        )

        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )

        WirePrimaryButton(
            text = stringResource(R.string.reload),
            onClick = { onRetry() }
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewErrorScreen() {
    WireTheme {
        ErrorScreen(
            isConnectionError = false,
            onRetry = {}
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewNetworkErrorScreen() {
    WireTheme {
        ErrorScreen(
            isConnectionError = true,
            onRetry = {}
        )
    }
}
