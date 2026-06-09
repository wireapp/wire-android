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
package com.wire.android.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.metro.scopedAssistedMetroViewModel
import com.wire.android.di.metro.scopedMetroViewModel
import com.wire.android.ui.home.conversationslist.ConversationListViewModel
import com.wire.android.ui.home.conversationslist.ConversationListViewModelImpl
import com.wire.android.ui.home.conversationslist.ConversationListViewModelPreview
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.drawer.HomeDrawerViewModel
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory

interface HomeManualViewModelFactory : ManualViewModelAssistedFactory {
    fun conversationListViewModel(conversationsSource: ConversationsSource): ConversationListViewModelImpl
}

@Composable
fun homeViewModel(): HomeViewModel =
    scopedMetroViewModel()

@Composable
fun appSyncViewModel(): AppSyncViewModel =
    scopedMetroViewModel()

@Composable
fun homeDrawerViewModel(): HomeDrawerViewModel =
    scopedMetroViewModel()

@Composable
fun conversationListViewModel(conversationsSource: ConversationsSource): ConversationListViewModel = when {
    LocalInspectionMode.current -> ConversationListViewModelPreview()
    else -> scopedAssistedMetroViewModel<ConversationListViewModelImpl, HomeManualViewModelFactory>(
        key = "list_$conversationsSource",
    ) {
        conversationListViewModel(conversationsSource)
    }
}

@Composable
fun newConversationViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): NewConversationViewModel =
    scopedMetroViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
    )

@Composable
fun featureFlagNotificationViewModel(): FeatureFlagNotificationViewModel =
    scopedMetroViewModel()
