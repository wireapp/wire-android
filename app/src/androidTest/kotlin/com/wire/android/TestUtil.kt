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
@file:OptIn(ExperimentalTestApi::class)

package com.wire.android

import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.wire.android.ui.WireActivity

fun AndroidComposeTestRule<ActivityScenarioRule<WireActivity>, WireActivity>.assertNodeWithTextExist(@StringRes resId: Int) {
    onNodeWithText(activity.getString(resId)).assertIsDisplayed()
}

fun AndroidComposeTestRule<ActivityScenarioRule<WireActivity>, WireActivity>.performClickWithNodeWithText(@StringRes resId: Int) {
    onNodeWithText(activity.getString(resId)).performClick()
}

fun AndroidComposeTestRule<ActivityScenarioRule<WireActivity>, WireActivity>.waitUntilNodeCount(
    matcher: SemanticsMatcher,
    count: Int,
    timeoutMillis: Long = WAIT_UNTIL_TIMEOUT
) {
    waitUntil(timeoutMillis) {
        onAllNodes(matcher).fetchSemanticsNodes().size == count
    }
}

fun AndroidComposeTestRule<ActivityScenarioRule<WireActivity>, WireActivity>.waitUntilExists(
    @StringRes resId: Int,
    timeoutMillis: Long = WAIT_UNTIL_TIMEOUT,
) = waitUntil(timeoutMillis = timeoutMillis) {
    onAllNodesWithText(activity.getString(resId))
        .fetchSemanticsNodes().size == 1
}

fun AndroidComposeTestRule<ActivityScenarioRule<WireActivity>, WireActivity>.waitUntilDoesNotExist(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = WAIT_UNTIL_TIMEOUT
) = waitUntilNodeCount(matcher, 0, timeoutMillis)


private const val WAIT_UNTIL_TIMEOUT = 2_000L
