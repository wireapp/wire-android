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
package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
internal fun OnboardingPendingInfo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = dimensions().spacing24x)
    ) {
        VerticalSpace.x24()
        Text(
            text = stringResource(com.wire.android.ui.common.R.string.onboarding_pending),
            textAlign = TextAlign.Center,
            color = MaterialTheme.wireColorScheme.onSurface,
            style = MaterialTheme.wireTypography.title02,
            modifier = Modifier.semantics { heading() }
        )
        VerticalSpace.x8()
        Text(
            text = stringResource(com.wire.android.ui.common.R.string.onboarding_pending_description),
            textAlign = TextAlign.Center,
            color = MaterialTheme.wireColorScheme.secondaryText,
            style = MaterialTheme.wireTypography.body01
        )
        VerticalSpace.x24()
    }
}
