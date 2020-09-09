package com.wire.android.feature.conversation.list

import com.wire.android.feature.conversation.list.ui.ConversationListViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val conversationsModule = module {
    viewModel { ConversationListViewModel(get(), get()) }
}
