/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.conversations

import androidx.lifecycle.ViewModel
import com.wire.android.ui.home.conversations.folder.ConversationFoldersStateArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersVMImpl
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderArgs
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderVMImpl
import com.wire.android.ui.home.conversations.folder.NewFolderViewModel
import com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminNavArgs
import com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminViewModel
import com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs
import com.wire.android.ui.home.conversations.search.adddembertoconversation.AddMembersToConversationViewModel
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesNavArgs
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
object ConversationSearchFolderMetroViewModelBindings {

    @Provides
    @IntoMap
    @ViewModelKey(NewFolderViewModel::class)
    fun newFolderViewModel(viewModel: NewFolderViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ConversationSearchFolderManualViewModelFactory::class)
    internal fun manualFactory(
        foldersFactory: ConversationFoldersVMImpl.Factory,
        moveFactory: MoveConversationToFolderVMImpl.Factory,
        addMembersFactory: AddMembersToConversationViewModel.Factory,
        searchMessagesFactory: SearchConversationMessagesViewModel.Factory,
        promoteAdminFactory: PromoteAdminViewModel.Factory,
    ): ManualViewModelAssistedFactory = object : ConversationSearchFolderManualViewModelFactory {
        override fun conversationFoldersViewModel(args: ConversationFoldersStateArgs) = foldersFactory.create(args)
        override fun moveConversationToFolderViewModel(args: MoveConversationToFolderArgs) = moveFactory.create(args)
        override fun addMembersToConversationViewModel(args: AddMembersSearchNavArgs) = addMembersFactory.create(args)
        override fun searchConversationMessagesViewModel(args: SearchConversationMessagesNavArgs) =
            searchMessagesFactory.create(args)
        override fun promoteAdminViewModel(args: PromoteAdminNavArgs) = promoteAdminFactory.create(args)
    }
}
