package com.wire.android.feature.conversation.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.feature.conversation.data.ConversationMapper
import com.wire.android.feature.conversation.data.ConversationsRepository
import com.wire.android.feature.conversation.data.remote.ConversationsApi
import com.wire.android.feature.conversation.data.remote.ConversationDataSource
import com.wire.android.feature.conversation.data.remote.ConversationsRemoteDataSource
import com.wire.android.feature.conversation.list.ui.ConversationListViewModel
import com.wire.android.feature.conversation.list.ui.navigation.MainNavigator
import com.wire.android.feature.conversation.list.usecase.GetConversationsUseCase
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val conversationsModule = module {
    single { MainNavigator() }
    viewModel { ConversationListViewModel(get(), get(), get()) }
    factory { GetConversationsUseCase(get()) }
    factory { ConversationMapper() }
    factory { ConversationsRemoteDataSource(get(), get()) }
    single<ConversationsRepository> { ConversationDataSource(get(), get()) }
    single { get<NetworkClient>().create(ConversationsApi::class.java) }
}

