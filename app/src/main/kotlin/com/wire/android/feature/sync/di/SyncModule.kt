package com.wire.android.feature.sync.di

import com.wire.android.feature.sync.conversation.usecase.SyncConversationsUseCase
import com.wire.android.feature.sync.slow.SlowSyncWorkHandler
import com.wire.android.feature.sync.ui.SyncViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val syncModule = module {
    single { SlowSyncWorkHandler(androidApplication()) }
    viewModel { SyncViewModel(get()) }

    factory { SyncConversationsUseCase(get()) }
}
