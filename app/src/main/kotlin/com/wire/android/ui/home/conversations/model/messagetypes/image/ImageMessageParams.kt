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
 *
 *
 */

package com.wire.android.ui.home.conversations.model.messagetypes.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.dimensions

data class ImageMessageParams(private val realImgWidth: Int, private val realImgHeight: Int) {
    // Image size normalizations to keep the ratio of the inline message image
    val normalizedWidth: Dp
        @Composable
        get() = dimensions().messageImageMaxWidth

    val normalizedHeight: Dp
        @Composable
        get() = Dp(normalizedWidth.value * realImgHeight.toFloat() / realImgWidth)
}
