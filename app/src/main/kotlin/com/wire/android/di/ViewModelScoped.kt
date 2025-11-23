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

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sebaslogen.resaca.hilt.hiltViewModelScoped
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Returns a proper scoped arguments instance from the given [SavedStateHandle] for the scoped [ViewModel].
 */
inline fun <reified R : ScopedArgs> SavedStateHandle.scopedArgs(): R =
    scopedArgs(R::class, this)

/**
 * Returns a proper scoped arguments instance from the given [SavedStateHandle].
 *
 * @param argsClass the class of the arguments, must implement [ScopedArgs] and be serializable
 * @param argsContainer the [SavedStateHandle] to get the arguments from
 */
@OptIn(InternalSerializationApi::class)
fun <R : ScopedArgs> scopedArgs(argsClass: KClass<R>, argsContainer: SavedStateHandle): R =
    Bundlizer.unbundle(argsClass.serializer(), argsContainer.toBundle())

/**
 * Custom implementation of [hiltViewModelScoped] that uses our generated previews for scoped ViewModels
 * and takes proper scoped serializable arguments that implement [ScopedArgs]
 * and provides them into scoped [ViewModel] converting it automatically to [Bundle] using [Bundlizer].
 *
 * [ViewModel] needs to implement an interface annotated with [ViewModelScopedPreview] and with default
 * implementations, and use @AssistedInject with an @AssistedFactory.
 *
 * Proper key will be taken from the [ScopedArgs.key] property.
 *
 * @param arguments The arguments that will be provided to the [ViewModel], must implement [ScopedArgs] and be serializable
 * @param F The factory type that creates the ViewModel with assisted injection
 */
@OptIn(InternalSerializationApi::class)
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
@Composable
inline fun <reified T, reified S, reified F, reified R : ScopedArgs> hiltViewModelScoped(arguments: R): S where T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> {
        val bundle = Bundlizer.bundle(R::class.serializer(), arguments)
        val savedStateHandle = SavedStateHandle.createHandle(null, bundle)
        hiltViewModelScoped<T, F>(
            key = arguments.key,
            creationCallback = { factory: F ->
                // Use reflection to find and invoke the create method on the factory
                val createMethod = factory!!::class.java.methods.find {
                    it.name == "create" && it.parameterCount == 1
                }
                createMethod?.invoke(factory, savedStateHandle) as T
            }
        )
    }
}

/**
 * Custom implementation of [hiltViewModelScoped] that uses our generated previews for scoped ViewModels.
 * This is version that does not take and pass any arguments, it does not use any key to generate a new version when it changes,
 * so it basically keeps the same instance.
 *
 * [ViewModel] needs to implement an interface annotated with [ViewModelScopedPreview] and with default
 * implementations.
 */
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
@Composable
inline fun <reified T, reified S> hiltViewModelScoped(): S where T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    espresso -> ViewModelScopedPreviews.firstNotNullOf { it as? S }
    else -> hiltViewModelScoped<T>()
}

val espresso
    get() = try {
        Class.forName("androidx.test.espresso.Espresso")
        true
    } catch (e: ClassNotFoundException) {
        false
    }

/**
 * Creates a [Bundle] with all key-values from the given [SavedStateHandle].
 */
@Suppress("SpreadOperator")
fun SavedStateHandle.toBundle(): Bundle = bundleOf(*(keys().map { it to get<Any>(it) }.toTypedArray()))

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
