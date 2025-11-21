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
@file:Suppress("ComposeModifierMissing", "ComposeNamingUppercase")

package com.wire.android.ui.common.spacers

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.dimensions

object HorizontalSpace {

    @Composable
    fun x2() {
        Spacer(Modifier.width(dimensions().spacing2x))
    }

    @Composable
    fun x4() {
        Spacer(Modifier.width(dimensions().spacing4x))
    }

    @Composable
    fun x8() {
        Spacer(Modifier.width(dimensions().spacing8x))
    }

    @Composable
    fun x12() {
        Spacer(Modifier.width(dimensions().spacing12x))
    }

    @Composable
    fun x16() {
        Spacer(Modifier.width(dimensions().spacing16x))
    }

    @Composable
    fun x24() {
        Spacer(Modifier.width(dimensions().spacing24x))
    }

    @Composable
    fun x32() {
        Spacer(Modifier.width(dimensions().spacing32x))
    }

    @Composable
    fun x48() {
        Spacer(Modifier.width(dimensions().spacing48x))
    }
}
