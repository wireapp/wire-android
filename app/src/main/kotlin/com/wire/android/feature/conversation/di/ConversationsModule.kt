package com.wire.android.feature.conversation.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.feature.conversation.data.ConversationDataSource
import com.wire.android.feature.conversation.data.ConversationMapper
import com.wire.android.feature.conversation.data.ConversationsRepository
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource
import com.wire.android.feature.conversation.data.remote.ConversationsApi
import com.wire.android.feature.conversation.data.remote.ConversationsRemoteDataSource
import com.wire.android.feature.conversation.list.ConversationListRepository
import com.wire.android.feature.conversation.list.datasources.ConversationListDataSource
import com.wire.android.feature.conversation.list.datasources.ConversationListMapper
import com.wire.android.feature.conversation.list.datasources.local.ConversationListLocalDataSource
import com.wire.android.feature.conversation.list.ui.ConversationListAdapter
import com.wire.android.feature.conversation.list.ui.ConversationListDiffCallback
import com.wire.android.feature.conversation.list.ui.ConversationListPagingDelegate
import com.wire.android.feature.conversation.list.ui.ConversationListViewModel
import com.wire.android.feature.conversation.list.ui.navigation.MainNavigator
import com.wire.android.feature.conversation.list.usecase.GetConversationMembersUseCase
import com.wire.android.feature.conversation.list.usecase.GetMembersOfConversationsUseCase
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val conversationsModule = module {
    single { MainNavigator() }

    factory { ConversationListAdapter(get(), get(), get()) }
    factory { ConversationListDiffCallback() }
    viewModel { ConversationListViewModel(get(), get(), get(), get()) }

    single<ConversationsRepository> { ConversationDataSource(get(), get(), get()) }
    factory { ConversationMapper() }

    factory { ConversationsRemoteDataSource(get(), get()) }
    single { get<NetworkClient>().create(ConversationsApi::class.java) }

    factory { get<UserDatabase>().conversationDao() }
    factory { get<UserDatabase>().conversationMembersDao() }
    factory { ConversationLocalDataSource(get(), get()) }

    factory { get<UserDatabase>().conversationListDao() }
    factory { ConversationListLocalDataSource(get()) }
    factory { ConversationListMapper(get(), get()) }
    factory<ConversationListRepository> { ConversationListDataSource(get(), get(), get()) }
    factory { ConversationListPagingDelegate(get()) }

    factory { GetConversationMembersUseCase(get(), get()) }
    factory { GetMembersOfConversationsUseCase(get(), get()) }
}
