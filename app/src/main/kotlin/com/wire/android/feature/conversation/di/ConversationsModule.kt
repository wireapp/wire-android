package com.wire.android.feature.conversation.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.core.network.di.AuthenticationType
import com.wire.android.feature.conversation.list.ui.ConversationListViewModel
import com.wire.android.feature.conversation.list.ui.navigation.MainNavigator
import com.wire.android.feature.conversation.list.usecase.ConversationApi
import com.wire.android.feature.conversation.list.usecase.ConversationDataSource
import com.wire.android.feature.conversation.list.usecase.ConversationRemoteDataSource
import com.wire.android.feature.conversation.list.usecase.ConversationsRepository
import com.wire.android.feature.conversation.list.usecase.GetConversationsUseCase
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val conversationsModule = module {
    single { MainNavigator() }
    viewModel { ConversationListViewModel(get(), get(), get()) }
    factory { GetConversationsUseCase(get()) }
    single<ConversationsRepository> { ConversationDataSource(get()) }
    factory {
        get<NetworkClient>(parameters = {
            parametersOf(AuthenticationType.TOKEN)
        }).create(ConversationApi::class.java)
    }
    factory { ConversationRemoteDataSource(get(), get()) }
}
