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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.sessionKeyedMetroViewModel
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
): VM where VM : ViewModel =
    sessionKeyedMetroViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
    )

@Composable
fun userDebugViewModel(): UserDebugViewModel =
    debugInfoViewModel()

@Composable
fun logManagementViewModel(): LogManagementViewModel =
    debugInfoViewModel()

@Composable
fun debugDataOptionsViewModel(): DebugDataOptionsViewModel =
    debugInfoViewModel<DebugDataOptionsViewModelImpl>()

@Composable
fun exportObfuscatedCopyViewModel(): ExportObfuscatedCopyViewModel =
    debugInfoViewModel<ExportObfuscatedCopyViewModelImpl>()

@Composable
fun debugConversationViewModel(): DebugConversationViewModel =
    debugInfoViewModel()

@Composable
fun conversationCryptoStatsViewModel(): ConversationCryptoStatsViewModel =
    debugInfoViewModel()

@Composable
fun debugFeatureFlagsViewModel(): DebugFeatureFlagsViewModel =
    debugInfoViewModel()

@Composable
fun whatsNewViewModel(): WhatsNewViewModel =
    debugInfoViewModel()

@Composable
fun aboutThisAppViewModel(): AboutThisAppViewModel =
    debugInfoViewModel()

@Composable
fun dependenciesViewModel(): DependenciesViewModel =
    debugInfoViewModel()

@Composable
fun licensesViewModel(): LicensesViewModel =
    debugInfoViewModel()

@Composable
fun aiAssistantDebugViewModel(): AiAssistantDebugViewModel =
    debugInfoViewModel<AiAssistantDebugViewModelImpl>()
