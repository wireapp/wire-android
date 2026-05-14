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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebaslogen.resaca.metro.metroViewModelScoped
import com.sebaslogen.resaca.KeyInScopeResolver
import com.wire.android.di.metro.LocalMetroViewModelGraph
import com.wire.android.di.metro.WireMetroGraph
import com.wire.android.di.metro.createWireMetroGraph
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
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER", "UnusedParameter")
@Composable
inline fun <reified T, reified S, reified R : ScopedArgs, reified F : AssistedViewModelFactory<T, R>>
        wireViewModelScoped(arguments: R, clearDelay: Duration? = null): S where T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> metroViewModelScoped<T>(
        key = arguments.key,
        clearDelay = clearDelay,
        factory = rememberMetroScopedViewModelFactory<T> {
            metroFactory<F>().create(arguments)
        },
    )
}

@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER", "UnusedParameter")
@Composable
inline fun <reified T, reified S, reified R : ScopedArgs, reified F : AssistedViewModelFactory<T, R>>
        wireViewModelScoped(
    arguments: R,
    noinline keyInScopeResolver: KeyInScopeResolver<String>,
    clearDelay: Duration? = null,
): S where T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> {
        requireNotNull(arguments.key?.toString()) {
            "Scoped key must not be null for ${T::class.qualifiedName}"
        }
        metroViewModelScoped<T, String>(
            key = arguments.key.toString(),
            keyInScopeResolver = keyInScopeResolver,
            clearDelay = clearDelay,
            factory = rememberMetroScopedViewModelFactory<T> {
                metroFactory<F>().create(arguments)
            },
        )
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
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
@Composable
inline fun <reified T, reified S> wireViewModelScoped(): S where T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> metroViewModelScoped<T>(
        factory = rememberMetroScopedViewModelFactory {
            metroFactory<AssistedViewModelFactory<T, ScopedArgs>>().create(EmptyScopedArgs)
        },
    )
}

@PublishedApi
@Composable
internal inline fun <reified VM : ViewModel> rememberMetroScopedViewModelFactory(
    crossinline create: WireMetroGraph.() -> VM,
): ViewModelProvider.Factory {
    val providedGraph = LocalMetroViewModelGraph.current as? WireMetroGraph
    val context = LocalContext.current
    val graph = providedGraph ?: remember(context) { createWireMetroGraph(context) }
    return remember(graph) {
        viewModelFactory {
            initializer {
                graph.create()
            }
        }
    }
}

@PublishedApi
internal inline fun <reified F> WireMetroGraph.metroFactory(): F =
    this::class.java.methods
        .firstOrNull { method ->
            method.parameterCount == 0 && F::class.java.isAssignableFrom(method.returnType)
        }
        ?.invoke(this) as? F
        ?: error("No Metro factory matching ${F::class.qualifiedName} was provided.")

@PublishedApi
internal data object EmptyScopedArgs : ScopedArgs {
    override val key: Any? = null
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
