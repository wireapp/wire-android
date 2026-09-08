/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home

import androidx.lifecycle.ViewModel
import com.wire.android.BuildConfig
import com.wire.android.ui.home.conversationslist.ConversationListViewModelImpl
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.drawer.HomeDrawerViewModel
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
object HomeMetroViewModelBindings {

    @Provides
    @IntoMap
    @ViewModelKey(AppSyncViewModel::class)
    fun appSyncViewModel(viewModel: AppSyncViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(FeatureFlagNotificationViewModel::class)
    fun featureFlagNotificationViewModel(viewModel: FeatureFlagNotificationViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    fun homeViewModel(viewModel: HomeViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(HomeDrawerViewModel::class)
    fun homeDrawerViewModel(viewModel: HomeDrawerViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelKey(NewConversationViewModel::class)
    fun newConversationViewModel(viewModel: NewConversationViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(HomeManualViewModelFactory::class)
    internal fun homeManualViewModelFactory(
        factory: ConversationListViewModelImpl.Factory,
    ): ManualViewModelAssistedFactory = object : HomeManualViewModelFactory {
        override fun conversationListViewModel(conversationsSource: ConversationsSource): ConversationListViewModelImpl =
            factory.create(
                conversationsSource = conversationsSource,
                usePagination = BuildConfig.PAGINATED_CONVERSATION_LIST_ENABLED,
            )
    }
}
