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

package com.wire.android.ui.home.conversations.search.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
<<<<<<< HEAD
fun SearchFailureBox(@StringRes failureMessage: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(224.dp)
    ) {
=======
fun SearchFailureBox(@StringRes failureMessage: Int) {
    Box(Modifier.fillMaxSize()) {
>>>>>>> cba33119f (fix: show proper empty user search screens [WPB-6257] 🍒 (#3602))
        Text(
            stringResource(id = failureMessage),
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.wireTypography.label04.copy(color = MaterialTheme.wireColorScheme.secondaryText)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun SearchFailureBoxPreview() = WireTheme {
    SearchFailureBox(failureMessage = com.wire.android.R.string.label_no_results_found)
}
