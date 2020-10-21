package com.wire.android.feature.conversation.di

import com.wire.android.feature.conversation.list.ui.ConversationListViewModel
import com.wire.android.feature.conversation.list.ui.navigation.MainNavigator
import com.wire.android.feature.conversation.list.usecase.GetConversationsUseCase
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val conversationsModule = module {
    single { MainNavigator() }

    viewModel { ConversationListViewModel(get(), get(), get()) }
    factory { GetConversationsUseCase() }
}
