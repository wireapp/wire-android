/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.conversations.ConversationFoldersNavigation3ResultType
import com.wire.android.ui.home.conversations.toNavigation3Route
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedViewModel
import com.wire.android.ui.miscViewModel
import com.wire.android.ui.home.settings.otherUserProfileScreenViewModel
import com.wire.android.ui.home.settings.selfQRCodeViewModel
import com.wire.android.ui.home.settings.serviceDetailsViewModel
import com.wire.android.ui.home.settings.settingsViewModel
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerResult
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerResultContract
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerRoute
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerScreen
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel
import com.wire.android.ui.userprofile.other.ConnectionRequestIgnoredResult
import com.wire.android.ui.userprofile.other.ConnectionRequestIgnoredResultContract
import com.wire.android.ui.userprofile.other.OtherUserProfileRoute
import com.wire.android.ui.userprofile.other.OtherUserProfileRouteScreen
import com.wire.android.ui.userprofile.other.toViewModelArgs as toOtherUserProfileViewModelArgs
import com.wire.android.ui.userprofile.qr.SelfQRCodeScreen
import com.wire.android.ui.userprofile.qr.SelfQrCodeRoute
import com.wire.android.ui.userprofile.qr.toViewModelArgs as toSelfQrCodeViewModelArgs
import com.wire.android.ui.userprofile.self.SelfUserProfileRoute
import com.wire.android.ui.userprofile.self.SelfUserProfileRouteScreen
import com.wire.android.ui.userprofile.self.SelfUserProfileViewModel
import com.wire.android.ui.userprofile.service.ServiceDetailsRoute
import com.wire.android.ui.userprofile.service.ServiceDetailsScreen
import com.wire.android.ui.userprofile.service.toViewModelArgs as toServiceDetailsViewModelArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireNavigationCommand

internal val AvatarPickerNavigation3ResultType = WireNavigation3ResultType(
    contract = AvatarPickerResultContract,
    serializer = AvatarPickerResult.serializer(),
)

internal val ConnectionRequestIgnoredNavigation3ResultType = WireNavigation3ResultType(
    contract = ConnectionRequestIgnoredResultContract,
    serializer = ConnectionRequestIgnoredResult.serializer(),
)

/**
 * Cross-batch exits used by the user-profile entries.
 *
 * Keeping these operations semantic prevents the contribution from importing generated
 * destinations while their owning batches are still on the legacy host.
 */
internal interface UserProfileNavigation3Actions {
    fun exitUserProfile()
    fun openAppSettings()
    fun openAddAccount()
    fun openTeamMigration(wasMigrationDotActive: Boolean)
    fun openAccountDetails()
    fun switchedToAnotherAccount()
    fun noOtherAccountToSwitch()
    fun openConversation(conversationId: UserProfileQualifiedId)
    fun openDeviceDetails(userId: UserProfileQualifiedId, clientId: String)
    fun searchConversationMessages(conversationId: UserProfileQualifiedId)
    fun openConversationMedia(conversationId: UserProfileQualifiedId)
    fun openConversationDebugMenu(conversationId: UserProfileQualifiedId)
}

internal object UserProfileNavigation3Contribution {
    val resultTypes: List<WireNavigation3ResultType<*>> = listOf(
        AvatarPickerNavigation3ResultType,
        ConnectionRequestIgnoredNavigation3ResultType,
    )

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: UserProfileNavigation3Actions,
    ): List<WireEntryProviderInstaller> = listOf(
        userProfileNavigation3Entries(runtime, actions)
    )
}

internal fun userProfileNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: UserProfileNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<AvatarPickerRoute>(presentation = WireEntryPresentation.Slide) {
        AvatarPickerNavigation3Entry(runtime)
    }
    wireEntry<SelfQrCodeRoute>(presentation = WireEntryPresentation.Slide) {
        SelfQrCodeNavigation3Entry(runtime, it, actions)
    }
    wireEntry<SelfUserProfileRoute>(presentation = WireEntryPresentation.PopUp) {
        SelfUserProfileNavigation3Entry(runtime, it, actions)
    }
    wireEntry<OtherUserProfileRoute>(presentation = WireEntryPresentation.PopUp) {
        OtherUserProfileNavigation3Entry(runtime, it, actions)
    }
    wireEntry<ServiceDetailsRoute>(presentation = WireEntryPresentation.PopUp) {
        ServiceDetailsNavigation3Entry(runtime, it, actions)
    }
}

@Composable
private fun AvatarPickerNavigation3Entry(
    runtime: WireNavigation3Runtime,
) {
    val viewModel = settingsViewModel<AvatarPickerViewModel>()
    AvatarPickerScreen(
        viewModel = viewModel,
        onNavigateBack = { runtime.navigator.goBack() },
        onAvatarSelected = { assetId ->
            if (
                !runtime.completeCurrentAndPop(
                    AvatarPickerNavigation3ResultType,
                    WireNavResult.Value(AvatarPickerResult(assetId)),
                )
            ) {
                runtime.navigator.goBack()
            }
        },
    )
}

@Composable
private fun SelfQrCodeNavigation3Entry(
    runtime: WireNavigation3Runtime,
    route: SelfQrCodeRoute,
    actions: UserProfileNavigation3Actions,
) {
    SelfQRCodeScreen(
        viewModel = selfQRCodeViewModel(route.toSelfQrCodeViewModelArgs()),
        onNavigateBack = {
            if (!runtime.navigator.goBack()) actions.exitUserProfile()
        },
    )
}

@Composable
private fun SelfUserProfileNavigation3Entry(
    runtime: WireNavigation3Runtime,
    route: SelfUserProfileRoute,
    actions: UserProfileNavigation3Actions,
) {
    val viewModel = settingsViewModel<SelfUserProfileViewModel>()
    val switchAccountActions = remember(route.entryId.value, actions) {
        UserProfileSwitchAccountActions(actions)
    }
    var avatarRequestIdValue by rememberSaveable(route.entryId.value) {
        mutableStateOf<String?>(null)
    }

    val currentEntryId = runtime.navigator.currentRoute?.entryId
    LaunchedEffect(avatarRequestIdValue, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = avatarRequestIdValue?.let(::WireNavResultRequestId)
            ?: return@LaunchedEffect
        when (
            val result = runtime.consumeResult(
                requestId,
                AvatarPickerNavigation3ResultType,
            )
        ) {
            is WireNavResult.Value -> {
                result.value.assetId?.let(viewModel::reloadNewPickedAvatar)
                avatarRequestIdValue = null
            }

            WireNavResult.Canceled -> avatarRequestIdValue = null
            null -> Unit
        }
    }

    SelfUserProfileRouteScreen(
        viewModelSelf = viewModel,
        legalHoldRequestedViewModel = miscViewModel<LegalHoldRequestedViewModel>(),
        onClose = {
            if (!runtime.navigator.goBack()) actions.exitUserProfile()
        },
        onLogout = { viewModel.logout(it, switchAccountActions) },
        onChangeAvatar = {
            avatarRequestIdValue = runtime.navigateForResult(
                AvatarPickerRoute(sessionId = route.sessionId),
                AvatarPickerNavigation3ResultType,
            )?.value
        },
        onEditProfile = actions::openAppSettings,
        onAddAccount = actions::openAddAccount,
        onSwitchAccount = { viewModel.switchAccount(it, switchAccountActions) },
        onOpenQrCode = { handle, isTeamMember ->
            runtime.navigator.navigate(
                WireNavigationCommand(
                    SelfQrCodeRoute(
                        sessionId = route.sessionId,
                        userHandle = handle,
                        isTeamMember = isTeamMember,
                    )
                )
            )
        },
        onCreateTeam = actions::openTeamMigration,
        onOpenAccountDetails = actions::openAccountDetails,
    )
}

@Composable
private fun OtherUserProfileNavigation3Entry(
    runtime: WireNavigation3Runtime,
    route: OtherUserProfileRoute,
    actions: UserProfileNavigation3Actions,
) {
    var folderRequestIdValue by rememberSaveable(route.entryId.value) {
        mutableStateOf<String?>(null)
    }
    val snackbarHostState = LocalSnackbarHostState.current
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    LaunchedEffect(folderRequestIdValue, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = folderRequestIdValue?.let(::WireNavResultRequestId)
            ?: return@LaunchedEffect
        when (val result = runtime.consumeResult(requestId, ConversationFoldersNavigation3ResultType)) {
            is WireNavResult.Value -> {
                // The snackbar is the durable fallback; no pre-recreation callback is required.
                snackbarHostState.showSnackbar(result.value.message)
                folderRequestIdValue = null
            }
            WireNavResult.Canceled -> folderRequestIdValue = null
            null -> Unit
        }
    }

    OtherUserProfileRouteScreen(
        viewModel = otherUserProfileScreenViewModel(
            route.toOtherUserProfileViewModelArgs()
        ),
        onNavigateBack = {
            if (!runtime.navigator.goBack()) actions.exitUserProfile()
        },
        onIgnoredConnectionRequest = { userName ->
            if (
                !runtime.completeCurrentAndPop(
                    ConnectionRequestIgnoredNavigation3ResultType,
                    WireNavResult.Value(ConnectionRequestIgnoredResult(userName)),
                )
            ) {
                runtime.navigator.goBack()
            }
        },
        onOpenConversation = { actions.openConversation(it.toNavigationId()) },
        onOpenDeviceDetails = {
            actions.openDeviceDetails(route.targetUserId, it.clientId.value)
        },
        onSearchConversationMessages = {
            actions.searchConversationMessages(it.toNavigationId())
        },
        onOpenConversationMedia = {
            actions.openConversationMedia(it.toNavigationId())
        },
        onMoveToFolder = { arguments ->
            folderRequestIdValue = runtime.navigateForResult(
                arguments.toNavigation3Route(route.sessionId),
                ConversationFoldersNavigation3ResultType,
            )?.value
        },
        onOpenConversationDebugMenu = {
            actions.openConversationDebugMenu(it.toNavigationId())
        },
    )
}

@Composable
private fun ServiceDetailsNavigation3Entry(
    runtime: WireNavigation3Runtime,
    route: ServiceDetailsRoute,
    actions: UserProfileNavigation3Actions,
) {
    ServiceDetailsScreen(
        viewModel = serviceDetailsViewModel(route.toServiceDetailsViewModelArgs()),
        navigateBack = {
            if (!runtime.navigator.goBack()) actions.exitUserProfile()
        },
        openConversation = { actions.openConversation(it.toNavigationId()) },
    )
}

private class UserProfileSwitchAccountActions(
    private val actions: UserProfileNavigation3Actions,
) : SwitchAccountActions {
    override fun switchedToAnotherAccount() = actions.switchedToAnotherAccount()
    override fun noOtherAccountToSwitch() = actions.noOtherAccountToSwitch()
}

private fun ConversationId.toNavigationId() = UserProfileQualifiedId(value, domain)
