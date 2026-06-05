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
package com.wire.android.ui.debug

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.metroSavedStateViewModel
import com.wire.android.di.metro.metroViewModel
import com.wire.android.ui.debug.conversation.DebugConversationViewModel
import com.wire.android.ui.debug.cryptostats.ConversationCryptoStatsViewModel
import com.wire.android.ui.debug.featureflags.DebugFeatureFlagsViewModel
import com.wire.android.ui.home.settings.about.dependencies.DependenciesViewModel
import com.wire.android.ui.home.settings.about.licenses.LicensesViewModel
import com.wire.android.ui.home.whatsnew.WhatsNewViewModel
import com.wire.android.ui.settings.about.AboutThisAppViewModel

interface DebugInfoViewModelGraph : MetroViewModelGraph {
    val debugInfoViewModelFactory: DebugInfoViewModelFactory
}

@Composable
inline fun <reified VM> debugInfoViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    crossinline create: DebugInfoViewModelFactory.() -> VM,
): VM where VM : ViewModel =
    metroViewModel<DebugInfoViewModelGraph, VM>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
    ) {
        debugInfoViewModelFactory.create()
    }

@Composable
inline fun <reified VM> debugInfoSavedStateViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    crossinline create: DebugInfoViewModelFactory.(SavedStateHandle) -> VM,
): VM where VM : ViewModel =
    metroSavedStateViewModel<DebugInfoViewModelGraph, VM>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
    ) { savedStateHandle ->
        debugInfoViewModelFactory.create(savedStateHandle)
    }

@Composable
fun userDebugViewModel(): UserDebugViewModel = debugInfoViewModel { userDebugViewModel() }

@Composable
fun logManagementViewModel(): LogManagementViewModel = debugInfoViewModel { logManagementViewModel() }

@Composable
fun debugDataOptionsViewModel(): DebugDataOptionsViewModel =
    debugInfoViewModel<DebugDataOptionsViewModelImpl> { debugDataOptionsViewModel() }

@Composable
fun exportObfuscatedCopyViewModel(): ExportObfuscatedCopyViewModel =
    debugInfoViewModel<ExportObfuscatedCopyViewModelImpl> { exportObfuscatedCopyViewModel() }

@Composable
fun debugConversationViewModel(): DebugConversationViewModel =
    debugInfoSavedStateViewModel { debugConversationViewModel(it) }

@Composable
fun conversationCryptoStatsViewModel(): ConversationCryptoStatsViewModel =
    debugInfoViewModel { conversationCryptoStatsViewModel() }

@Composable
fun debugFeatureFlagsViewModel(): DebugFeatureFlagsViewModel =
    debugInfoViewModel { debugFeatureFlagsViewModel() }

@Composable
fun whatsNewViewModel(): WhatsNewViewModel = debugInfoViewModel { whatsNewViewModel() }

@Composable
fun aboutThisAppViewModel(): AboutThisAppViewModel = debugInfoViewModel { aboutThisAppViewModel() }

@Composable
fun dependenciesViewModel(): DependenciesViewModel = debugInfoViewModel { dependenciesViewModel() }

@Composable
fun licensesViewModel(): LicensesViewModel = debugInfoViewModel { licensesViewModel() }

@Composable
fun aiAssistantDebugViewModel(): AiAssistantDebugViewModel =
    debugInfoViewModel<AiAssistantDebugViewModelImpl> { aiAssistantDebugViewModel() }
