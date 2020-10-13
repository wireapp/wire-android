package com.wire.android.feature.conversation.list

import com.wire.android.feature.conversation.list.ui.ConversationListViewModel
import com.wire.android.feature.conversation.list.ui.navigation.MainNavigator
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val conversationsModule = module {
    single { MainNavigator() }
    viewModel { ConversationListViewModel(get(), get()) }
}
