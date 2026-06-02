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
import com.sebaslogen.resaca.hilt.hiltViewModelScoped as resacaHiltViewModelScoped
import com.sebaslogen.resaca.viewModelScoped as resacaViewModelScoped
import kotlin.time.Duration

/**
 * Common assisted factory contract for scoped ViewModels that receive [ScopedArgs].
 */
interface AssistedViewModelFactory<VM : ViewModel, R : ScopedArgs> {
    fun create(args: R): VM
}

/**
 * Repo-local scoped ViewModel accessor that uses our generated previews for scoped ViewModels
 * and creates assisted injected scoped [ViewModel] instances using [ScopedArgs].
 *
 * [ViewModel] needs to implement an interface annotated with [ViewModelScopedPreview] and with default
 * implementations.
 *
 * Proper key will be taken from the [ScopedArgs.key] property.
 *
 * @param arguments The arguments that will be provided to the [ViewModel].
 */
@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified T, reified S, reified R : ScopedArgs, reified F : AssistedViewModelFactory<T, R>>
        wireViewModelScoped(arguments: R, clearDelay: Duration? = null): S where T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> resacaHiltViewModelScoped<T, F>(key = arguments.key?.toString(), clearDelay = clearDelay) { factory ->
        factory.create(arguments)
    } as S
}

@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified T, reified S, reified R : ScopedArgs, reified F : AssistedViewModelFactory<T, R>>
        wireViewModelScoped(
    arguments: R,
    noinline keyInScopeResolver: KeyInScopeResolver<String>,
    clearDelay: Duration? = null,
): S where T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> {
        val scopedKey = requireNotNull(arguments.key?.toString()) {
            "Scoped key must not be null for ${T::class.qualifiedName}"
        }
        resacaHiltViewModelScoped<T, F, String>(
            key = scopedKey,
            keyInScopeResolver = keyInScopeResolver,
            clearDelay = clearDelay
        ) { factory ->
            factory.create(arguments)
        } as S
    }
}

/**
 * Repo-local scoped ViewModel accessor that uses our generated previews for scoped ViewModels.
 * This is version that does not take and pass any arguments, it does not use any key to generate a new
 * version when it changes, so it basically keeps the same instance.
 *
 * [ViewModel] needs to implement an interface annotated with [ViewModelScopedPreview] and with default
 * implementations.
 */
@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified T, reified S> wireViewModelScoped(): S where T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> resacaHiltViewModelScoped<T>() as S
}

@Composable
inline fun <reified T : ViewModel> wireViewModelScoped(): T = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? T }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? T }
    else -> resacaHiltViewModelScoped<T>()
}

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

@Deprecated("Use wireViewModelScoped so call sites do not depend on the Hilt-backed implementation.")
@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified T, reified S, reified R : ScopedArgs, reified F : AssistedViewModelFactory<T, R>>
        hiltViewModelScoped(arguments: R, clearDelay: Duration? = null): S where T : ViewModel, T : S =
    wireViewModelScoped<T, S, R, F>(arguments = arguments, clearDelay = clearDelay)

@Deprecated("Use wireViewModelScoped so call sites do not depend on the Hilt-backed implementation.")
@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified T, reified S, reified R : ScopedArgs, reified F : AssistedViewModelFactory<T, R>>
        hiltViewModelScoped(
    arguments: R,
    noinline keyInScopeResolver: KeyInScopeResolver<String>,
    clearDelay: Duration? = null,
): S where T : ViewModel, T : S =
    wireViewModelScoped<T, S, R, F>(
        arguments = arguments,
        keyInScopeResolver = keyInScopeResolver,
        clearDelay = clearDelay
    )

@Deprecated("Use wireViewModelScoped so call sites do not depend on the Hilt-backed implementation.")
@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified T, reified S> hiltViewModelScoped(): S where T : ViewModel, T : S =
    wireViewModelScoped<T, S>()

@Deprecated("Use wireViewModelScoped so call sites do not depend on the Hilt-backed implementation.")
@Composable
inline fun <reified T : ViewModel> hiltViewModelScoped(): T = wireViewModelScoped<T>()

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
