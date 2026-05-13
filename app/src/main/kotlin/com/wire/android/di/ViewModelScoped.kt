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
import androidx.lifecycle.ViewModel
import com.sebaslogen.resaca.KeyInScopeResolver
import com.sebaslogen.resaca.hilt.hiltViewModelScoped
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
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
@Composable
inline fun <reified T, reified S, reified R : ScopedArgs, reified F : AssistedViewModelFactory<T, R>>
        wireViewModelScoped(arguments: R, clearDelay: Duration? = null): S where T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> hiltViewModelScoped<T, F>(key = arguments.key?.toString(), clearDelay = clearDelay) { factory ->
        factory.create(arguments)
    }
}

@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
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
        val scopedKey = requireNotNull(arguments.key?.toString()) {
            "Scoped key must not be null for ${T::class.qualifiedName}"
        }
        hiltViewModelScoped<T, F, String>(
            key = scopedKey,
            keyInScopeResolver = keyInScopeResolver,
            clearDelay = clearDelay
        ) { factory ->
            factory.create(arguments)
        }
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
    else -> hiltViewModelScoped<T>()
}

@Deprecated(
    message = "Use wireViewModelScoped to avoid exposing the DI implementation in local APIs.",
    replaceWith = ReplaceWith("wireViewModelScoped(arguments, clearDelay)")
)
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
@Composable
inline fun <reified T, reified S, reified R : ScopedArgs, reified F : AssistedViewModelFactory<T, R>>
        hiltViewModelScoped(arguments: R, clearDelay: Duration? = null): S where T : ViewModel, T : S =
    wireViewModelScoped<T, S, R, F>(arguments = arguments, clearDelay = clearDelay)

@Deprecated(
    message = "Use wireViewModelScoped to avoid exposing the DI implementation in local APIs.",
    replaceWith = ReplaceWith("wireViewModelScoped(arguments, keyInScopeResolver, clearDelay)")
)
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
@Composable
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

@Deprecated(
    message = "Use wireViewModelScoped to avoid exposing the DI implementation in local APIs.",
    replaceWith = ReplaceWith("wireViewModelScoped()")
)
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
@Composable
inline fun <reified T, reified S> hiltViewModelScoped(): S where T : ViewModel, T : S =
    wireViewModelScoped<T, S>()

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
