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
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.metroSavedStateViewModel
import com.wire.android.di.metro.metroViewModel
import com.wire.android.ui.home.conversationslist.ConversationListViewModel
import com.wire.android.ui.home.conversationslist.ConversationListViewModelImpl
import com.wire.android.ui.home.conversationslist.ConversationListViewModelPreview
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.drawer.HomeDrawerViewModel
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel

interface HomeViewModelGraph : MetroViewModelGraph {
    val homeViewModelFactory: HomeViewModelFactory
}

@Composable
fun homeViewModel(): HomeViewModel =
    metroSavedStateViewModel<HomeViewModelGraph, HomeViewModel> { homeViewModelFactory.homeViewModel(it) }

@Composable
fun appSyncViewModel(): AppSyncViewModel =
    metroViewModel<HomeViewModelGraph, AppSyncViewModel> { homeViewModelFactory.appSyncViewModel() }

@Composable
fun homeDrawerViewModel(): HomeDrawerViewModel =
    metroSavedStateViewModel<HomeViewModelGraph, HomeDrawerViewModel> { homeViewModelFactory.homeDrawerViewModel(it) }

@Composable
fun conversationListViewModel(conversationsSource: ConversationsSource): ConversationListViewModel = when {
    LocalInspectionMode.current -> ConversationListViewModelPreview()
    else -> metroViewModel<HomeViewModelGraph, ConversationListViewModelImpl>(key = "list_$conversationsSource") {
        homeViewModelFactory.conversationListViewModel(conversationsSource)
    }
}

@Composable
fun newConversationViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): NewConversationViewModel =
    metroViewModel<HomeViewModelGraph, NewConversationViewModel>(viewModelStoreOwner = viewModelStoreOwner) {
        homeViewModelFactory.newConversationViewModel()
    }

@Composable
fun featureFlagNotificationViewModel(): FeatureFlagNotificationViewModel =
    metroViewModel<HomeViewModelGraph, FeatureFlagNotificationViewModel> {
        homeViewModelFactory.featureFlagNotificationViewModel()
    }
