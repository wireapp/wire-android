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
package com.wire.android.extensions

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText

fun ComposeContentTestRule.waitUntilExists(
    text: String,
    timeoutMillis: Long = WAIT_UNTIL_TIMEOUT,
) = waitUntil(timeoutMillis = timeoutMillis) {
    onAllNodesWithText(text)
        .fetchSemanticsNodes().size == 1
}

private const val WAIT_UNTIL_TIMEOUT = 2_000L
