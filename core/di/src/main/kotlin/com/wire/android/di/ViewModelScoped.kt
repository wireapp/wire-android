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
package com.wire.android.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.sebaslogen.resaca.KeyInScopeResolver
import com.wire.android.di.metro.LocalWireViewModelScopeKey
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.time.Duration
import com.sebaslogen.resaca.metro.metroViewModelScoped as resacaMetroViewModelScoped

@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified T, reified S, reified R : ScopedArgs, reified FactoryType> wireManualMetroViewModelScoped(
    arguments: R,
    previewProvider: PreviewProvider = EmptyPreviewProvider,
    clearDelay: Duration? = null,
    noinline create: FactoryType.(SavedStateHandle, R) -> T,
): S where T : ViewModel, T : S, FactoryType : ManualViewModelAssistedFactory = previewProvider.findPreviewOr {
    resacaMetroViewModelScoped<T>(
        key = scopedResacaKey(LocalWireViewModelScopeKey.current, arguments.key?.toString()),
        clearDelay = clearDelay,
        factory = manualScopedViewModelFactory<T, FactoryType> { extras ->
            create(extras.createSavedStateHandle(), arguments)
        },
        creationExtras = defaultViewModelCreationExtras(),
    ) as S
}

@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified T, reified S, reified R : ScopedArgs, reified FactoryType> wireManualMetroViewModelScoped(
    arguments: R,
    noinline keyInScopeResolver: KeyInScopeResolver<String>,
    previewProvider: PreviewProvider = EmptyPreviewProvider,
    clearDelay: Duration? = null,
    noinline create: FactoryType.(SavedStateHandle, R) -> T,
): S where T : ViewModel, T : S, FactoryType : ManualViewModelAssistedFactory = previewProvider.findPreviewOr {
    val argumentsKey = requireNotNull(arguments.key?.toString()) {
        "Scoped key must not be null for ${T::class.qualifiedName}"
    }
    resacaMetroViewModelScoped<T, String>(
        key = requireNotNull(scopedResacaKey(LocalWireViewModelScopeKey.current, argumentsKey)),
        keyInScopeResolver = { keyInScopeResolver(argumentsKey) },
        clearDelay = clearDelay,
        factory = manualScopedViewModelFactory<T, FactoryType> { extras ->
            create(extras.createSavedStateHandle(), arguments)
        },
        creationExtras = defaultViewModelCreationExtras(),
    ) as S
}

@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified T, reified S, reified FactoryType> wireManualMetroViewModelScoped(
    previewProvider: PreviewProvider = EmptyPreviewProvider,
    clearDelay: Duration? = null,
    noinline create: FactoryType.(SavedStateHandle) -> T,
): S where T : ViewModel, T : S, FactoryType : ManualViewModelAssistedFactory = previewProvider.findPreviewOr {
    resacaMetroViewModelScoped<T>(
        key = scopedResacaKey(LocalWireViewModelScopeKey.current),
        clearDelay = clearDelay,
        factory = manualScopedViewModelFactory<T, FactoryType> { extras ->
            create(extras.createSavedStateHandle())
        },
        creationExtras = defaultViewModelCreationExtras(),
    ) as S
}

@Composable
inline fun <reified T, reified FactoryType> wireManualMetroViewModelScoped(
    previewProvider: PreviewProvider = EmptyPreviewProvider,
    clearDelay: Duration? = null,
    noinline create: FactoryType.(SavedStateHandle) -> T,
): T where T : ViewModel, FactoryType : ManualViewModelAssistedFactory = previewProvider.findPreviewOr {
    wireManualMetroViewModelScoped<T, T, FactoryType>(
        clearDelay = clearDelay,
        create = create,
    )
}

@Composable
@PublishedApi
internal inline fun <reified T, reified FactoryType> manualScopedViewModelFactory(
    crossinline create: FactoryType.(CreationExtras) -> T,
): ViewModelProvider.Factory where T : ViewModel, FactoryType : ManualViewModelAssistedFactory {
    val metroViewModelFactory = LocalMetroViewModelFactory.current
    return object : ViewModelProvider.Factory {
        override fun <VM : ViewModel> create(modelClass: KClass<VM>, extras: CreationExtras): VM {
            val factory = metroViewModelFactory.createManuallyAssistedFactory(FactoryType::class)()
            return modelClass.cast(factory.create(extras))
        }
    }
}

@Composable
@PublishedApi
internal fun defaultViewModelCreationExtras(): CreationExtras {
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    return if (viewModelStoreOwner is HasDefaultViewModelProviderFactory) {
        viewModelStoreOwner.defaultViewModelCreationExtras
    } else {
        CreationExtras.Empty
    }
}

@PublishedApi
internal fun scopedResacaKey(scopeKey: String?, key: String? = null): String? =
    when (scopeKey) {
        null -> key
        else -> listOfNotNull(key, scopeKey).joinToString(":")
    }

@Composable
@PublishedApi
internal inline fun <reified S> PreviewProvider.findPreviewOr(provideViewModel: @Composable () -> S): S {
    val errorMessage = "No preview found for ${S::class.qualifiedName} in preview provider ${this::class.qualifiedName}." +
            " Make sure to add @ViewModelScopedPreview to the ViewModel interface."
    return when {
        LocalInspectionMode.current -> previews.firstNotNullOfOrNull { it as? S } ?: error(errorMessage)
        espresso -> previews.firstNotNullOfOrNull { it as? S } ?: error(errorMessage)
        else -> provideViewModel()
    }
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

@Stable
interface PreviewProvider {
    val previews: List<Any> get() = emptyList()
}
object EmptyPreviewProvider : PreviewProvider

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ViewModelScopedPreview
