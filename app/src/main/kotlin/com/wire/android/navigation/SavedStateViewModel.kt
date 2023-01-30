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

package com.wire.android.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap

/*
 * ViewModel extension which already has SavedStateHandle field.
 * It is recommended to use it for NavigationItem together with the hiltSavedStateViewModel(backNavArgs: ImmutableMap<String, Any>) which
 * requires a map of back navigation arguments which are passed to the NavBackStackEntry's SavedStateHandle when using popWithArguments.
 */
open class SavedStateViewModel(open val savedStateHandle: SavedStateHandle) : ViewModel()

@Composable
inline fun <reified T : SavedStateViewModel> hiltSavedStateViewModel(backNavArgs: ImmutableMap<String, Any>) =
    hiltViewModel<T>().apply {
        savedStateHandle[EXTRA_BACK_NAVIGATION_ARGUMENTS] = backNavArgs.toMap()
    }

fun <V : Any> SavedStateHandle.getBackNavArgs(): ImmutableMap<String, V> =
    get<Map<String, V>>(EXTRA_BACK_NAVIGATION_ARGUMENTS)?.toImmutableMap() ?: persistentMapOf()

@Suppress("UNCHECKED_CAST")
fun <V : Any> SavedStateHandle.getBackNavArg(key: String): V? = getBackNavArgs<Any>()[key] as? V
