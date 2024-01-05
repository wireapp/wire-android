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

package com.wire.android.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.theme.WireColorScheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.id.ConversationId
import kotlin.math.absoluteValue

@Composable
internal fun dimensions() = MaterialTheme.wireDimensions

@Composable
internal fun colorsScheme() = MaterialTheme.wireColorScheme

@Composable
internal fun typography() = MaterialTheme.wireTypography

@Composable
internal fun WireColorScheme.conversationColor(id: ConversationId): Color {
    val colors = this.groupAvatarColors
    return colors[(id.hashCode() % colors.size).absoluteValue]
}
