package com.wire.android.feature.conversation.di

import com.wire.android.R
import com.wire.android.core.network.NetworkClient
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.core.ui.navigation.FragmentContainerProvider
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.datasources.MessageDataSource
import com.wire.android.feature.conversation.content.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import com.wire.android.feature.conversation.content.mapper.MessageStateMapper
import com.wire.android.feature.conversation.content.mapper.MessageTypeMapper
import com.wire.android.feature.conversation.data.ConversationDataSource
import com.wire.android.feature.conversation.data.ConversationMapper
import com.wire.android.feature.conversation.data.ConversationRepository
import com.wire.android.feature.conversation.data.ConversationTypeMapper
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
import com.wire.android.feature.conversation.list.ui.icon.ConversationIconProvider
import com.wire.android.feature.conversation.list.ui.navigation.MainNavigator
import com.wire.android.feature.conversation.list.usecase.GetConversationListUseCase
import com.wire.android.feature.conversation.list.usecase.GetConversationMembersUseCase
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val conversationModules
    get() = arrayOf(
        conversationsModule,
        conversationListModule,
        conversationMembersModule,
        conversationContentModule
    )

val conversationsModule = module {
    single { MainNavigator(get()) }
    factory(qualifier<MainActivity>()) {
        FragmentContainerProvider.fixedProvider(R.id.mainFragmentContainer)
    }
    single<ConversationRepository> { ConversationDataSource(get(), get(), get()) }
    factory { ConversationMapper(get()) }
    factory { ConversationTypeMapper() }

    factory { ConversationsRemoteDataSource(get(), get()) }
    single { get<NetworkClient>().create(ConversationsApi::class.java) }

    factory { get<UserDatabase>().conversationDao() }
    factory { ConversationLocalDataSource(get(), get()) }
}

val conversationListModule = module {
    factory { ConversationListAdapter(get(), get(), get()) }
    factory { ConversationListDiffCallback() }
    viewModel { ConversationListViewModel(get(), get(), get(), get()) }

    factory { ConversationIconProvider(get()) }

    factory { get<UserDatabase>().conversationListDao() }
    factory { ConversationListLocalDataSource(get()) }
    factory { ConversationListMapper(get(), get()) }
    factory<ConversationListRepository> { ConversationListDataSource(get(), get(), get()) }
    factory { GetConversationListUseCase(get()) }
}

val conversationMembersModule = module {
    factory { get<UserDatabase>().conversationMembersDao() }
    factory { GetConversationMembersUseCase(get(), get()) }
}

val conversationContentModule = module {
    factory { get<UserDatabase>().messageDao() }
    factory { MessageLocalDataSource(get()) }
    factory { MessageTypeMapper() }
    factory { MessageStateMapper() }
    factory { MessageMapper(get(), get()) }
    factory<MessageRepository> { MessageDataSource(get(), get(), get()) }
}
