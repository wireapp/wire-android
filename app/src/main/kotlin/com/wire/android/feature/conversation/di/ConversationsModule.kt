package com.wire.android.feature.conversation.di

import com.wire.android.R
import com.wire.android.core.network.NetworkClient
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.core.ui.navigation.FragmentContainerProvider
import com.wire.android.feature.conversation.data.ConversationDataSource
import com.wire.android.feature.conversation.data.ConversationMapper
import com.wire.android.feature.conversation.data.ConversationTypeMapper
import com.wire.android.feature.conversation.data.ConversationsRepository
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource
import com.wire.android.feature.conversation.data.remote.ConversationsApi
import com.wire.android.feature.conversation.data.remote.ConversationsRemoteDataSource
import com.wire.android.feature.conversation.list.ConversationListRepository
import com.wire.android.feature.conversation.list.MainActivity
import com.wire.android.feature.conversation.list.datasources.ConversationListDataSource
import com.wire.android.feature.conversation.list.datasources.ConversationListMapper
import com.wire.android.feature.conversation.list.datasources.local.ConversationListLocalDataSource
import com.wire.android.feature.conversation.list.ui.ConversationListAdapter
import com.wire.android.feature.conversation.list.ui.ConversationListDiffCallback
import com.wire.android.feature.conversation.list.ui.ConversationListViewModel
import com.wire.android.feature.conversation.list.ui.icon.ContactIconProvider
import com.wire.android.feature.conversation.list.ui.icon.ConversationIconProvider
import com.wire.android.feature.conversation.list.ui.navigation.MainNavigator
import com.wire.android.feature.conversation.list.usecase.GetConversationListUseCase
import com.wire.android.feature.conversation.list.usecase.GetConversationMembersUseCase
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val conversationsModule = module {
    single { MainNavigator(get()) }
    factory(qualifier<MainActivity>()) {
        FragmentContainerProvider.fixedProvider(R.id.mainFragmentContainer)
    }

    factory { ConversationListAdapter(get(), get(), get()) }
    factory { ConversationListDiffCallback() }
    viewModel { ConversationListViewModel(get(), get(), get(), get(), get()) }

    factory { ContactIconProvider(get()) }
    factory { ConversationIconProvider(get()) }

    single<ConversationsRepository> { ConversationDataSource(get(), get(), get()) }
    factory { ConversationMapper(get()) }
    factory { ConversationTypeMapper() }

    factory { ConversationsRemoteDataSource(get(), get()) }
    single { get<NetworkClient>().create(ConversationsApi::class.java) }

    factory { get<UserDatabase>().conversationDao() }
    factory { get<UserDatabase>().conversationMembersDao() }
    factory { ConversationLocalDataSource(get(), get()) }

    factory { get<UserDatabase>().conversationListDao() }
    factory { ConversationListLocalDataSource(get()) }
    factory { ConversationListMapper(get(), get()) }
    factory<ConversationListRepository> { ConversationListDataSource(get(), get(), get(), get()) }
    factory { GetConversationListUseCase(get()) }

    factory { GetConversationMembersUseCase(get(), get()) }
}
