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
package com.wire.android.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireColorScheme

/** The adjacent pending subtitle supplies the accessible description. */
@Composable
fun OnboardingPendingBadge(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_onboarding_pending),
        contentDescription = null,
        tint = MaterialTheme.wireColorScheme.secondaryText,
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}
