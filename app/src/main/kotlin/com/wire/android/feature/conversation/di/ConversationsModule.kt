package com.wire.android.feature.conversation.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.feature.conversation.list.ui.ConversationListViewModel
import com.wire.android.feature.conversation.list.ui.navigation.MainNavigator
import com.wire.android.feature.conversation.list.usecase.ConversationApi
import com.wire.android.feature.conversation.list.usecase.ConversationDataSource
import com.wire.android.feature.conversation.list.usecase.ConversationRemoteDataSource
import com.wire.android.feature.conversation.list.usecase.ConversationsRepository
import com.wire.android.feature.conversation.list.usecase.GetConversationsUseCase
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val conversationsModule = module {
    single { MainNavigator() }

    viewModel { ConversationListViewModel(get(), get(), get()) }
    factory { GetConversationsUseCase(get()) }
    factory { ConversationDataSource(get()) as ConversationsRepository }
    factory { ConversationRemoteDataSource(get(), get<NetworkClient>().create(ConversationApi::class.java)) }
}
