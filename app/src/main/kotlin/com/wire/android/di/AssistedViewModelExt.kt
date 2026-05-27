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

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dagger.hilt.android.lifecycle.withCreationCallback

inline fun <reified VM : ViewModel, reified VMF> ComponentActivity.assistedViewModels(
    crossinline create: (VMF) -> VM
) = viewModels<VM>(extrasProducer = {
    defaultViewModelCreationExtras.withCreationCallback<VMF> { factory -> create(factory) }
})

@Composable
inline fun <reified VM : ViewModel> wireViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
): VM = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner, key = key)

@Composable
inline fun <reified VM : ViewModel, reified VMF> wireViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    noinline creationCallback: (VMF) -> VM
): VM = hiltViewModel(
    viewModelStoreOwner = viewModelStoreOwner,
    key = key,
    creationCallback = creationCallback
)
