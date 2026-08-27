/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.di.metro

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.EmptyPreviewProvider
import com.wire.android.di.PreviewProvider
import com.wire.android.di.findPreviewOr
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * Builds the identity of one ViewModel instance inside its [ViewModelStoreOwner].
 *
 * Owner, session and Metro graph identity deliberately do not participate in this key. The owner
 * defines lifetime; this key only distinguishes the ViewModel class and, when explicitly needed,
 * multiple immutable instances of that class inside the same owner.
 */
fun wireViewModelInstanceKey(
    viewModelClassName: String,
    instanceKey: String? = null,
): String {
    require(viewModelClassName.isNotBlank()) { "ViewModel class name cannot be blank" }
    return instanceKey?.let { "$viewModelClassName:$it" } ?: viewModelClassName
}

inline fun <reified VM : ViewModel> wireViewModelInstanceKey(
    instanceKey: String? = null,
): String = wireViewModelInstanceKey(
    viewModelClassName = checkNotNull(VM::class.qualifiedName) {
        "ViewModel classes used by Metro must have a qualified name"
    },
    instanceKey = instanceKey,
)

/**
 * The single gateway for creating a regular Metro-backed ViewModel.
 */
@Composable
inline fun <reified VM : ViewModel> wireMetroViewModel(
    instanceKey: String? = null,
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    previewProvider: PreviewProvider = EmptyPreviewProvider,
): VM = previewProvider.findPreviewOr {
    wireMetroViewModelWithResolvedKey(
        owner = owner,
        resolvedKey = wireViewModelInstanceKey<VM>(instanceKey),
    )
}

/**
 * Activity/service entry-point variant of the same Metro ViewModel gateway.
 *
 * Platform coordinators that must obtain a ViewModel before Compose starts pass the graph factory
 * explicitly. Identity and diagnostics remain identical to the composable gateway.
 */
inline fun <reified VM : ViewModel> wireMetroViewModel(
    owner: ViewModelStoreOwner,
    factory: MetroViewModelFactory,
    instanceKey: String? = null,
): VM {
    val resolvedKey = wireViewModelInstanceKey<VM>(instanceKey)
    WireViewModelDiagnostics.viewModelRequested(owner, resolvedKey)
    return ViewModelProvider(owner, factory)[resolvedKey, VM::class.java]
}

/**
 * Interface-exposing overload used while generated preview implementations target an interface.
 */
@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <reified VM, reified S> wireMetroViewModelAs(
    instanceKey: String? = null,
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    previewProvider: PreviewProvider = EmptyPreviewProvider,
): S where VM : ViewModel, VM : S = previewProvider.findPreviewOr {
    wireMetroViewModelWithResolvedKey<VM>(
        owner = owner,
        resolvedKey = wireViewModelInstanceKey<VM>(instanceKey),
    )
}

/**
 * The single gateway for creating a manually-assisted Metro-backed ViewModel.
 */
@Composable
inline fun <
    reified VM : ViewModel,
    reified Factory : ManualViewModelAssistedFactory,
> wireAssistedMetroViewModel(
    instanceKey: String? = null,
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    previewProvider: PreviewProvider = EmptyPreviewProvider,
    crossinline create: Factory.(CreationExtras) -> VM,
): VM = previewProvider.findPreviewOr {
    wireAssistedMetroViewModelWithResolvedKey(
        owner = owner,
        resolvedKey = wireViewModelInstanceKey<VM>(instanceKey),
        create = create,
    )
}

/**
 * Assisted interface-exposing overload used while previews target a ViewModel interface.
 */
@Composable
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
inline fun <
    reified VM,
    reified S,
    reified Factory : ManualViewModelAssistedFactory,
> wireAssistedMetroViewModelAs(
    instanceKey: String? = null,
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    previewProvider: PreviewProvider = EmptyPreviewProvider,
    crossinline create: Factory.(CreationExtras) -> VM,
): S where VM : ViewModel, VM : S = previewProvider.findPreviewOr {
    wireAssistedMetroViewModelWithResolvedKey<VM, Factory>(
        owner = owner,
        resolvedKey = wireViewModelInstanceKey<VM>(instanceKey),
        create = create,
    )
}

@Composable
@PublishedApi
internal inline fun <reified VM : ViewModel> wireMetroViewModelWithResolvedKey(
    owner: ViewModelStoreOwner,
    resolvedKey: String?,
): VM {
    WireViewModelDiagnostics.viewModelRequested(owner, checkNotNull(resolvedKey))
    return metroViewModel(
        viewModelStoreOwner = owner,
        key = resolvedKey,
    )
}

@Composable
@PublishedApi
internal inline fun <
    reified VM : ViewModel,
    reified Factory : ManualViewModelAssistedFactory,
> wireAssistedMetroViewModelWithResolvedKey(
    owner: ViewModelStoreOwner,
    resolvedKey: String?,
    crossinline create: Factory.(CreationExtras) -> VM,
): VM {
    WireViewModelDiagnostics.viewModelRequested(owner, checkNotNull(resolvedKey))
    return assistedMetroViewModel<VM, Factory>(
        viewModelStoreOwner = owner,
        key = resolvedKey,
    ) { extras ->
        create(extras)
    }
}
