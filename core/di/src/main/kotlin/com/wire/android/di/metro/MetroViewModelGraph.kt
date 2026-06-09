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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel

interface MetroViewModelGraph {
    val viewModelScopeKey: String?
        get() = null
}

val LocalWireViewModelScopeKey = staticCompositionLocalOf<String?> {
    null
}

fun scopedMetroViewModelKey(defaultKey: String?, key: String?, scopeKey: String?): String? {
    if (scopeKey == null) return key
    return "${key ?: defaultKey}:$scopeKey"
}

/**
 * Creates a Metro-backed ViewModel using the current Wire ViewModel scope key.
 *
 * Session screens provide [LocalWireViewModelScopeKey] from the active account/session graph.
 * Including that value in the ViewModel key prevents Compose from reusing a ViewModel that was
 * created for a different account after account switching, logout, or client removal.
 *
 * Use this for regular Metro ViewModels that do not need runtime screen arguments. Pass [key] only
 * when the same ViewModel class can have multiple instances inside one scope, for example one
 * instance per tab, conversation, or picker target.
 */
@Composable
inline fun <reified VM> scopedMetroViewModel(
    key: String? = null,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): VM where VM : ViewModel =
    metroViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = scopedMetroViewModelKey(
            defaultKey = VM::class.qualifiedName,
            key = key,
            scopeKey = LocalWireViewModelScopeKey.current,
        ),
    )

/**
 * Creates an assisted Metro-backed ViewModel using the current Wire ViewModel scope key.
 *
 * This has the same account/session isolation guarantees as [scopedMetroViewModel], but it also
 * lets the caller pass runtime screen arguments through a [ManualViewModelAssistedFactory].
 *
 * Use this when the ViewModel constructor needs values that are only known by the screen route or
 * call site, such as conversation id, selected folder id, login arguments, or tab type.
 */
@Composable
inline fun <reified VM, reified Factory> scopedAssistedMetroViewModel(
    key: String? = null,
    crossinline createViewModel: Factory.() -> VM,
): VM where VM : ViewModel, Factory : ManualViewModelAssistedFactory =
    assistedMetroViewModel<VM, Factory>(
        key = scopedMetroViewModelKey(
            defaultKey = VM::class.qualifiedName,
            key = key,
            scopeKey = LocalWireViewModelScopeKey.current,
        ),
    ) {
        createViewModel()
    }
