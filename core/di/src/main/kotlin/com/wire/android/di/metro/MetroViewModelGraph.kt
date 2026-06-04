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
package com.wire.android.di.metro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

interface MetroViewModelGraph {
    val viewModelScopeKey: String?
        get() = null
}

/**
 * Temporary Android Compose bridge used while moving ViewModel creation to Metro.
 *
 * Reusable UI should not treat this as a general DI entry point. The target direction is to keep
 * common UI components UI-only and pass state, callbacks, or narrow models from screen/container
 * code.
 */
val LocalMetroViewModelGraph = staticCompositionLocalOf<MetroViewModelGraph?> {
    null
}

@Composable
inline fun <reified Graph, reified VM> metroViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    scopeKeyOverride: String? = null,
    crossinline create: Graph.() -> VM,
): VM where Graph : MetroViewModelGraph, VM : ViewModel {
    val graph = checkNotNull(LocalMetroViewModelGraph.current as? Graph) {
        "No Metro graph matching ${Graph::class.qualifiedName} was provided"
    }
    val scopedKey = scopedMetroViewModelKey(
        defaultKey = VM::class.qualifiedName,
        key = key,
        scopeKey = scopeKeyOverride ?: graph.viewModelScopeKey,
    )
    val factory = remember(graph) {
        viewModelFactory {
            initializer {
                graph.create()
            }
        }
    }
    return viewModel(
        modelClass = VM::class,
        viewModelStoreOwner = viewModelStoreOwner,
        key = scopedKey,
        factory = factory,
    )
}

@Composable
inline fun <reified Graph, reified VM> metroSavedStateViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    scopeKeyOverride: String? = null,
    crossinline create: Graph.(SavedStateHandle) -> VM,
): VM where Graph : MetroViewModelGraph, VM : ViewModel {
    val graph = checkNotNull(LocalMetroViewModelGraph.current as? Graph) {
        "No Metro graph matching ${Graph::class.qualifiedName} was provided"
    }
    val scopedKey = scopedMetroViewModelKey(
        defaultKey = VM::class.qualifiedName,
        key = key,
        scopeKey = scopeKeyOverride ?: graph.viewModelScopeKey,
    )
    val factory = remember(graph) {
        viewModelFactory {
            initializer {
                graph.create(createSavedStateHandle())
            }
        }
    }
    return viewModel(
        modelClass = VM::class,
        viewModelStoreOwner = viewModelStoreOwner,
        key = scopedKey,
        factory = factory,
    )
}

fun scopedMetroViewModelKey(defaultKey: String?, key: String?, scopeKey: String?): String? {
    if (scopeKey == null) return key
    return "${key ?: defaultKey}:$scopeKey"
}
