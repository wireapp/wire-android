/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.conversations.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.ui.home.conversations.ConversationCompletionNavigation3ResultType
import com.wire.android.ui.home.conversations.ConversationAuxId
import com.wire.android.ui.home.conversations.ConversationFoldersNavigation3ResultType
import com.wire.android.ui.home.conversations.ConversationFoldersRoute
import com.wire.android.ui.home.conversations.toNavigation3
import com.wire.android.ui.home.conversations.createPasswordGuestLinkViewModel
import com.wire.android.ui.home.conversations.editConversationMetadataViewModel
import com.wire.android.ui.home.conversations.editGuestAccessViewModel
import com.wire.android.ui.home.conversations.editSelfDeletingMessagesViewModel
import com.wire.android.ui.home.conversations.groupConversationDetailsViewModel
import com.wire.android.ui.home.conversations.groupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.updateAppsAccessViewModel
import com.wire.android.ui.home.conversations.updateChannelAccessViewModel
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessRouteScreen
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordProtectedGuestLinkRouteScreen
import com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesRouteScreen
import com.wire.android.ui.home.conversations.details.metadata.EditConversationNameRouteScreen
import com.wire.android.ui.home.conversations.details.participants.GroupConversationAllParticipantsRouteScreen
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessRouteScreen
import com.wire.android.ui.home.conversations.details.updatechannelaccess.ChannelAccessOnUpdateRouteScreen
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireNavigationCommand

internal val EditConversationNameNavigation3ResultType = WireNavigation3ResultType(
    EditConversationNameResultContract,
    EditConversationNameResult.serializer(),
)

internal val ChannelAccessUpdateNavigation3ResultType = WireNavigation3ResultType(
    ChannelAccessUpdateResultContract,
    ChannelAccessUpdateResult.serializer(),
)

internal interface ConversationDetailsNavigation3Actions {
    fun exitDetails()
    fun openParticipantProfile(participant: UIParticipant, conversationId: ConversationDetailsId)
    fun openAddMembers(
        conversationId: ConversationDetailsId,
        isConversationAppsEnabled: Boolean,
        isSelfPartOfATeam: Boolean,
        protocolInfo: Conversation.ProtocolInfo,
        shouldUseNewAppsUi: Boolean,
    )

    fun openSearchMessages(conversationId: ConversationDetailsId, isCellsConversation: Boolean, groupName: String)
    fun openConversationMedia(conversationId: ConversationDetailsId, isCellsConversation: Boolean, groupName: String)
    fun completeDetails(action: GroupConversationActionType, conversationName: String)
    fun openPromoteAdmin(conversationId: ConversationId, eligibleMembers: List<UserId>)
    fun openConversationDebugMenu(conversationId: ConversationId)
}

internal object ConversationDetailsNavigation3Contribution {
    val resultTypes: List<WireNavigation3ResultType<*>> = listOf(
        EditConversationNameNavigation3ResultType,
        ChannelAccessUpdateNavigation3ResultType,
    )

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: ConversationDetailsNavigation3Actions,
    ): List<WireEntryProviderInstaller> = listOf(
        conversationDetailsNavigation3Entries(runtime, actions)
    )
}

@Suppress("LongMethod")
internal fun conversationDetailsNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: ConversationDetailsNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<GroupConversationDetailsRoute>(presentation = WireEntryPresentation.PopUp) {
        GroupConversationDetailsNavigation3Entry(it, runtime, actions)
    }
    wireEntry<EditConversationNameRoute>(presentation = WireEntryPresentation.Slide) {
        EditConversationNameNavigation3Entry(it, runtime)
    }
    wireEntry<EditSelfDeletingMessagesRoute>(presentation = WireEntryPresentation.Slide) {
        EditSelfDeletingMessagesRouteScreen(
            viewModel = editSelfDeletingMessagesViewModel(it.toViewModelArgs()),
            onNavigateBack = runtime.navigator::goBack,
        )
    }
    wireEntry<GroupConversationAllParticipantsRoute> {
        GroupConversationAllParticipantsRouteScreen(
            viewModel = groupConversationParticipantsViewModel(it.toViewModelArgs()),
            onBackPressed = runtime.navigator::goBack,
            onProfilePressed = { participant ->
                actions.openParticipantProfile(participant, it.conversationId)
            },
        )
    }
    wireEntry<UpdateAppsAccessRoute>(presentation = WireEntryPresentation.Slide) {
        UpdateAppsAccessRouteScreen(
            viewModel = updateAppsAccessViewModel(it.toViewModelArgs()),
            onNavigateBack = runtime.navigator::goBack,
        )
    }
    wireEntry<ChannelAccessOnUpdateRoute>(presentation = WireEntryPresentation.Slide) {
        ChannelAccessOnUpdateRouteScreen(
            viewModel = updateChannelAccessViewModel(it.toViewModelArgs()),
            onNavigateBack = { result ->
                val typedResult = ChannelAccessUpdateResult(
                    result.accessType.toNavigation3(),
                    result.permissionType.toNavigation3(),
                )
                if (!runtime.completeCurrentAndPop(
                        ChannelAccessUpdateNavigation3ResultType,
                        WireNavResult.Value(typedResult),
                    )
                ) {
                    runtime.navigator.goBack()
                }
            },
        )
    }
    wireEntry<EditGuestAccessRoute>(presentation = WireEntryPresentation.Slide) {
        EditGuestAccessRouteScreen(
            viewModel = editGuestAccessViewModel(it.toViewModelArgs()),
            onNavigateBack = runtime.navigator::goBack,
            onCreatePasswordProtectedLink = {
                runtime.navigator.navigate(
                    WireNavigationCommand(
                        CreatePasswordProtectedGuestLinkRoute(it.sessionId, it.conversationId)
                    )
                )
            },
        )
    }
    wireEntry<CreatePasswordProtectedGuestLinkRoute> {
        CreatePasswordProtectedGuestLinkRouteScreen(
            viewModel = createPasswordGuestLinkViewModel(it.toViewModelArgs()),
            navigateBack = runtime.navigator::goBack,
        )
    }
}

@Composable
private fun EditConversationNameNavigation3Entry(
    route: EditConversationNameRoute,
    runtime: WireNavigation3Runtime,
) {
    EditConversationNameRouteScreen(
        viewModel = editConversationMetadataViewModel(route.toViewModelArgs()),
        onNavigateBack = runtime.navigator::goBack,
        onCompleted = {
            if (!runtime.completeCurrentAndPop(
                    EditConversationNameNavigation3ResultType,
                    WireNavResult.Value(EditConversationNameResult(it)),
                )
            ) {
                runtime.navigator.goBack()
            }
        },
    )
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun GroupConversationDetailsNavigation3Entry(
    route: GroupConversationDetailsRoute,
    runtime: WireNavigation3Runtime,
    actions: ConversationDetailsNavigation3Actions,
) {
    val viewModel = groupConversationDetailsViewModel(route.toViewModelArgs())
    var renameRequest by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    var channelRequest by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    var folderRequest by rememberSaveable(route.entryId.value) { mutableStateOf<String?>(null) }
    var renameResult by rememberSaveable(route.entryId.value) { mutableStateOf<Boolean?>(null) }
    val snackbarHostState = LocalSnackbarHostState.current
    val currentEntryId = runtime.navigator.currentRoute?.entryId

    LaunchedEffect(renameRequest, channelRequest, folderRequest, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        renameRequest?.let(::WireNavResultRequestId)?.let { requestId ->
            when (val result = runtime.consumeResult(requestId, EditConversationNameNavigation3ResultType)) {
                is WireNavResult.Value -> {
                    renameResult = result.value.succeeded
                    renameRequest = null
                }
                WireNavResult.Canceled -> renameRequest = null
                null -> Unit
            }
        }
        channelRequest?.let(::WireNavResultRequestId)?.let { requestId ->
            when (val result = runtime.consumeResult(requestId, ChannelAccessUpdateNavigation3ResultType)) {
                is WireNavResult.Value -> {
                    viewModel.updateChannelAccess(result.value.accessType.toLegacy())
                    viewModel.updateChannelAddPermission(result.value.permissionType.toLegacy())
                    channelRequest = null
                }
                WireNavResult.Canceled -> channelRequest = null
                null -> Unit
            }
        }
        folderRequest?.let(::WireNavResultRequestId)?.let { requestId ->
            when (val result = runtime.consumeResult(requestId, ConversationFoldersNavigation3ResultType)) {
                is WireNavResult.Value -> {
                    // The snackbar is the durable fallback; no pre-recreation callback is required.
                    snackbarHostState.showSnackbar(result.value.message)
                    folderRequest = null
                }
                WireNavResult.Canceled -> folderRequest = null
                null -> Unit
            }
        }
    }

    GroupConversationDetailsRouteScreen(
        viewModel = viewModel,
        renameResult = renameResult,
        actions = GroupConversationDetailsRouteScreenActions(
            onBackPressed = {
                if (!runtime.navigator.goBack()) actions.exitDetails()
            },
            onProfilePressed = { actions.openParticipantProfile(it, route.conversationId) },
            onAddParticipants = { apps, team, protocol, newUi ->
                actions.openAddMembers(route.conversationId, apps, team, protocol, newUi)
            },
            onEditGuestAccess = { guests, services, updating ->
                runtime.navigator.navigate(
                    WireNavigationCommand(
                        EditGuestAccessRoute(route.sessionId, route.conversationId, guests, services, updating)
                    )
                )
            },
            onAppsAccess = { guests, apps, newUi ->
                runtime.navigator.navigate(
                    WireNavigationCommand(
                        UpdateAppsAccessRoute(route.sessionId, route.conversationId, guests, apps, newUi)
                    )
                )
            },
            onChannelAccess = { access, permission ->
                channelRequest = runtime.navigateForResult(
                    ChannelAccessOnUpdateRoute(
                        route.sessionId,
                        route.conversationId,
                        access.toNavigation3(),
                        permission.toNavigation3(),
                    ),
                    ChannelAccessUpdateNavigation3ResultType,
                )?.value
            },
            onEditSelfDeletingMessages = {
                runtime.navigator.navigate(
                    WireNavigationCommand(EditSelfDeletingMessagesRoute(route.sessionId, route.conversationId))
                )
            },
            onEditGroupName = {
                renameResult = null
                renameRequest = runtime.navigateForResult(
                    EditConversationNameRoute(route.sessionId, route.conversationId),
                    EditConversationNameNavigation3ResultType,
                )?.value
            },
            onSearchConversationMessages = { cells, name ->
                actions.openSearchMessages(route.conversationId, cells, name)
            },
            onConversationMedia = { cells, name ->
                actions.openConversationMedia(route.conversationId, cells, name)
            },
            onMoveToFolder = { args, _ ->
                folderRequest = runtime.navigateForResult(
                    ConversationFoldersRoute(
                        route.sessionId,
                        ConversationAuxId(args.conversationId.value, args.conversationId.domain),
                        args.conversationName,
                        args.currentFolderId,
                    ),
                    ConversationFoldersNavigation3ResultType,
                )?.value
            },
            onConversationCompleted = { action, name ->
                val legacy = GroupConversationDetailsNavBackArgs(action, name)
                if (!runtime.completeCurrentAndPop(
                        ConversationCompletionNavigation3ResultType,
                        WireNavResult.Value(legacy.toNavigation3()),
                    )
                ) {
                    actions.completeDetails(action, name)
                }
            },
            onPromoteAdmin = actions::openPromoteAdmin,
            onOpenConversationDebugMenu = actions::openConversationDebugMenu,
        ),
    )
}
