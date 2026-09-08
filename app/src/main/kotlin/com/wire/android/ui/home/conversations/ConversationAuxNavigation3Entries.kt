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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wire.android.model.Contact
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.ui.debug.conversation.DebugConversationRouteScreen
import com.wire.android.ui.debug.debugConversationViewModel
import com.wire.android.ui.home.conversations.channels.BrowseChannelsRouteScreen
import com.wire.android.ui.home.conversations.folder.ConversationFoldersRouteScreen
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderArgs
import com.wire.android.ui.home.conversations.folder.NewConversationFolderRouteScreen
import com.wire.android.ui.home.conversations.folder.NewFolderViewModel
import com.wire.android.ui.home.conversations.folder.ConversationFoldersStateArgs
import com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminRouteScreen
import com.wire.android.ui.home.conversations.search.adddembertoconversation.AddMembersSearchRouteScreen
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesRouteScreen
import com.wire.kalium.logic.data.conversation.FolderType
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId

internal val ConversationFoldersNavigation3ResultType = WireNavigation3ResultType(
    ConversationFoldersResultContract,
    ConversationFoldersResult.serializer(),
)

internal val NewConversationFolderNavigation3ResultType = WireNavigation3ResultType(
    NewConversationFolderResultContract,
    NewConversationFolderResult.serializer(),
)

internal interface ConversationAuxNavigation3Actions {
    fun openConversationAtMessage(conversationId: ConversationAuxId, messageId: String)
    fun openCellsSearch(conversationId: ConversationAuxId)
    fun openUserProfile(userId: ConversationAuxId)
    fun openService(
        conversationId: ConversationAuxId,
        serviceId: ConversationAuxId,
        shouldUseNewAppsUi: Boolean,
    )
}

internal object ConversationAuxNavigation3Contribution {
    val resultTypes: List<WireNavigation3ResultType<*>> = listOf(
        ConversationFoldersNavigation3ResultType,
        NewConversationFolderNavigation3ResultType,
    )

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: ConversationAuxNavigation3Actions,
    ): List<WireEntryProviderInstaller> = listOf(
        conversationAuxNavigation3Entries(runtime, actions)
    )
}

internal fun conversationAuxNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: ConversationAuxNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<BrowseChannelsRoute>(presentation = WireEntryPresentation.PopUp) {
        BrowseChannelsRouteScreen(runtime.navigator::goBack)
    }
    wireEntry<ConversationFoldersRoute>(presentation = WireEntryPresentation.Slide) {
        ConversationFoldersNavigation3Entry(it, runtime)
    }
    wireEntry<NewConversationFolderRoute>(presentation = WireEntryPresentation.Slide) {
        NewConversationFolderNavigation3Entry(runtime, newFolderViewModel())
    }
    wireEntry<SearchConversationMessagesRoute>(presentation = WireEntryPresentation.PopUp) {
        SearchConversationMessagesRouteScreen(
            viewModel = searchConversationMessagesViewModel(it.toViewModelArgs()),
            onMessageClick = { messageId -> actions.openConversationAtMessage(it.conversationId, messageId) },
            onCloseSearchClicked = runtime.navigator::goBack,
            onSearchFilesButtonClick = { actions.openCellsSearch(it.conversationId) },
        )
    }
    wireEntry<PromoteAdminRoute>(presentation = WireEntryPresentation.PopUp) {
        PromoteAdminRouteScreen(
            viewModel = promoteAdminViewModel(it.toViewModelArgs()),
            onNavigateBack = runtime.navigator::goBack,
        )
    }
    wireEntry<AddMembersSearchRoute> {
        AddMembersSearchRouteScreen(
            viewModel = addMembersToConversationViewModel(it.toViewModelArgs()),
            navArgs = it.toViewModelArgs(),
            onNavigateBack = runtime.navigator::goBack,
            onOpenUserProfile = { contact -> actions.openUserProfile(contact.toRouteId()) },
            onOpenService = { contact ->
                actions.openService(it.conversationId, contact.toRouteId(), it.shouldUseNewAppsUi)
            },
        )
    }
    wireEntry<DebugConversationRoute> {
        DebugConversationRouteScreen(
            viewModel = debugConversationViewModel(it.toViewModelArgs()),
            onNavigateBack = runtime.navigator::goBack,
        )
    }
}

@Composable
private fun ConversationFoldersNavigation3Entry(
    route: ConversationFoldersRoute,
    runtime: WireNavigation3Runtime,
) {
    val args = route.toViewModelArgs()
    val foldersViewModel = conversationFoldersViewModel(ConversationFoldersStateArgs(route.currentFolderId))
    val moveViewModel = moveConversationToFolderViewModel(
        MoveConversationToFolderArgs(args.conversationId, args.conversationName, args.currentFolderId)
    )
    var pendingFolderRequest by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    val currentEntryId = runtime.navigator.currentRoute?.entryId

    LaunchedEffect(pendingFolderRequest, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = pendingFolderRequest?.let(::WireNavResultRequestId) ?: return@LaunchedEffect
        when (val result = runtime.consumeResult(requestId, NewConversationFolderNavigation3ResultType)) {
            is WireNavResult.Value -> {
                moveViewModel.moveConversationToFolder(
                    com.wire.kalium.logic.data.conversation.ConversationFolder(
                        result.value.folderId,
                        result.value.folderName,
                        FolderType.USER,
                    )
                )
                pendingFolderRequest = null
            }
            WireNavResult.Canceled -> pendingFolderRequest = null
            null -> Unit
        }
    }

    ConversationFoldersRouteScreen(
        args = args,
        foldersViewModel = foldersViewModel,
        moveToFolderViewModel = moveViewModel,
        onNavigateBack = runtime.navigator::goBack,
        onCompleted = { message ->
            if (!runtime.completeCurrentAndPop(
                    ConversationFoldersNavigation3ResultType,
                    WireNavResult.Value(ConversationFoldersResult(message)),
                )
            ) {
                runtime.navigator.goBack()
            }
        },
        onCreateFolderPressed = {
            pendingFolderRequest = runtime.navigateForResult(
                NewConversationFolderRoute(route.sessionId, flowId = route.entryId.value),
                NewConversationFolderNavigation3ResultType,
            )?.value
        },
    )
}

@Suppress("ComposeViewModelForwarding")
@Composable
private fun NewConversationFolderNavigation3Entry(
    runtime: WireNavigation3Runtime,
    viewModel: NewFolderViewModel,
) {
    NewConversationFolderRouteScreen(
        viewModel = viewModel,
        onBackPressed = runtime.navigator::goBack,
        onFolderCreated = {
            val result = NewConversationFolderResult(it.folderName, it.folderId)
            if (!runtime.completeCurrentAndPop(
                    NewConversationFolderNavigation3ResultType,
                    WireNavResult.Value(result),
                )
            ) {
                runtime.navigator.goBack()
            }
        },
    )
}

private fun Contact.toRouteId() = ConversationAuxId(id, domain)
