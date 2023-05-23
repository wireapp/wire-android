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
 * Wrapper for view model states with additional [isLoading] variable for UI purposes
 */
data class LoadableState<T>(
    val state: T,
    val isLoading: Boolean = false
)

fun <T> LoadableState<T>.startLoading(): LoadableState<T> = this.copy(isLoading = true)
fun <T> LoadableState<T>.finishLoading(): LoadableState<T> = this.copy(isLoading = false)
fun <T> LoadableState<T>.updateState(newState: T): LoadableState<T> = this.copy(state = newState, isLoading = false)
