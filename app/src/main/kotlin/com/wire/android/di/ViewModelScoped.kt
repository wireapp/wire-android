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
package com.wire.android.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.wire.android.di.metro.LocalMetroViewModelGraph
import com.wire.android.di.metro.MetroViewModelGraph
import com.sebaslogen.resaca.KeyInScopeResolver
import com.sebaslogen.resaca.viewModelScoped as resacaViewModelScoped
import kotlin.time.Duration

@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified Graph, reified T, reified S, reified R : ScopedArgs> wireMetroViewModelScoped(
    arguments: R,
    clearDelay: Duration? = null,
    noinline create: Graph.(SavedStateHandle, R) -> T,
): S where Graph : MetroViewModelGraph, T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> {
        val graph = currentMetroViewModelGraph<Graph>()
        resacaViewModelScoped<T>(
            key = arguments.key?.toString(),
            clearDelay = clearDelay,
        ) { savedStateHandle ->
            graph.create(savedStateHandle, arguments)
        } as S
    }
}

@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified Graph, reified T, reified S, reified R : ScopedArgs> wireMetroViewModelScoped(
    arguments: R,
    noinline keyInScopeResolver: KeyInScopeResolver<String>,
    clearDelay: Duration? = null,
    noinline create: Graph.(SavedStateHandle, R) -> T,
): S where Graph : MetroViewModelGraph, T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> {
        val graph = currentMetroViewModelGraph<Graph>()
        val scopedKey = requireNotNull(arguments.key?.toString()) {
            "Scoped key must not be null for ${T::class.qualifiedName}"
        }
        resacaViewModelScoped<T, String>(
            key = scopedKey,
            keyInScopeResolver = keyInScopeResolver,
            clearDelay = clearDelay,
        ) { savedStateHandle ->
            graph.create(savedStateHandle, arguments)
        } as S
    }
}

@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified Graph, reified T, reified S> wireMetroViewModelScoped(
    clearDelay: Duration? = null,
    noinline create: Graph.(SavedStateHandle) -> T,
): S where Graph : MetroViewModelGraph, T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> {
        val graph = currentMetroViewModelGraph<Graph>()
        resacaViewModelScoped<T>(clearDelay = clearDelay) { savedStateHandle ->
            graph.create(savedStateHandle)
        } as S
    }
}

@Composable
inline fun <reified Graph, reified T> wireMetroViewModelScoped(
    clearDelay: Duration? = null,
    noinline create: Graph.(SavedStateHandle) -> T,
): T where Graph : MetroViewModelGraph, T : ViewModel = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? T }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? T }
    else -> {
        val graph = currentMetroViewModelGraph<Graph>()
        resacaViewModelScoped<T>(clearDelay = clearDelay) { savedStateHandle ->
            graph.create(savedStateHandle)
        }
    }
}

@Composable
inline fun <reified Graph : MetroViewModelGraph> currentMetroViewModelGraph(): Graph =
    checkNotNull(LocalMetroViewModelGraph.current as? Graph) {
        "No Metro graph matching ${Graph::class.qualifiedName} was provided"
    }

val espresso
    get() = try {
        Class.forName("androidx.test.espresso.Espresso")
        true
    } catch (e: ClassNotFoundException) {
        false
    }

/**
 * Interface for arguments for scoped ViewModels.
 * It is used to provide a unique key for the scoped ViewModel.
 */
interface ScopedArgs {
    val key: Any?
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ViewModelScopedPreview
