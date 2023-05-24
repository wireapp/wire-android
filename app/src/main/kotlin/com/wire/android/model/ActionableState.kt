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

package com.wire.android.model

/**
 * Wrapper for view model states with additional [isPerformingAction] which informs UI that action is being performed,
 * like:
 * - blocking button action when some action is already triggered
 */
data class ActionableState<T>(
    val state: T,
    val isPerformingAction: Boolean = false
)

fun <T> ActionableState<T>.performAction(): ActionableState<T> = this.copy(isPerformingAction = true)
fun <T> ActionableState<T>.finishAction(): ActionableState<T> = this.copy(isPerformingAction = false)
fun <T> ActionableState<T>.updateState(newState: T): ActionableState<T> = this.copy(state = newState, isPerformingAction = false)
