/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

@file:Suppress("TooManyFunctions")

package com.wire.android.navigation.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.feature.cells.ui.AllFilesNavigationActions
import com.wire.android.feature.cells.navigation.AddRemoveTagsRoute
import com.wire.android.feature.cells.navigation.CellImageViewerRoute
import com.wire.android.feature.cells.navigation.CellsFilesArguments
import com.wire.android.feature.cells.navigation.CellsSearchType
import com.wire.android.feature.cells.navigation.ConversationFilesRoute
import com.wire.android.feature.cells.navigation.PublicLinkRoute
import com.wire.android.feature.cells.navigation.SearchRoute
import com.wire.android.feature.cells.navigation.VideoPlayerRoute
import com.wire.android.feature.meetings.ui.create.MeetingParticipantId
import com.wire.android.feature.meetings.ui.create.NewMeetingDetailsRoute
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.routes.auth.AuthenticationLoginCompletion
import com.wire.android.navigation.routes.auth.AuthenticationNavigation3Router
import com.wire.android.navigation.routes.auth.AuthenticationNavigationTransition
import com.wire.android.navigation.routes.auth.AuthenticationTeamAccountCreationRequest
import com.wire.android.navigation.routes.auth.CreateAccountUsernameRoute
import com.wire.android.navigation.routes.auth.NewLoginRoute
import com.wire.android.navigation.routes.auth.WelcomeRoute
import com.wire.android.navigation.routes.media.ConversationMediaRoute
import com.wire.android.navigation.routes.media.MediaAssetDto
import com.wire.android.navigation.routes.media.MediaConversationId
import com.wire.android.navigation.routes.utility.DebugRoute
import com.wire.android.navigation.routes.utility.InitialSyncRoute
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.android.ui.home.HomeExternalDestination
import com.wire.android.ui.home.HomeRequirement
import com.wire.android.ui.home.appLock.SetLockCodeRoute
import com.wire.android.ui.home.conversations.BrowseChannelsRoute
import com.wire.android.ui.home.conversations.ConversationAuxId
import com.wire.android.ui.home.conversations.ConversationCompletionResult
import com.wire.android.ui.home.conversations.ConversationPendingAsset
import com.wire.android.ui.home.conversations.ConversationRoute
import com.wire.android.ui.home.conversations.ConversationRouteId
import com.wire.android.ui.home.conversations.DebugConversationRoute
import com.wire.android.ui.home.conversations.PromoteAdminRoute
import com.wire.android.ui.home.conversations.SearchConversationMessagesRoute
import com.wire.android.ui.home.conversations.AddMembersSearchRoute
import com.wire.android.ui.home.conversations.toNavigation3
import com.wire.android.ui.home.conversations.details.ConversationDetailsId
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversationslist.ConversationsNavigationActions
import com.wire.android.ui.home.newconversation.NewConversationSearchPeopleRoute
import com.wire.android.ui.home.settings.AboutThisAppRoute
import com.wire.android.ui.home.settings.AppSettingsRoute
import com.wire.android.ui.home.settings.MyAccountRoute
import com.wire.android.ui.home.settings.SettingsNavigation3Destination
import com.wire.android.ui.home.whatsnew.WhatsNewNavigation3Target
import com.wire.android.ui.settings.devices.DeviceDetailsRoute
import com.wire.android.ui.settings.devices.DeviceTargetUserId
import com.wire.android.ui.settings.devices.SelfDevicesRoute
import com.wire.android.ui.settings.devices.SessionSetupDestination
import com.wire.android.ui.authentication.devices.register.RegisterDeviceRoute
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceRoute
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentRoute
import com.wire.android.ui.userprofile.UserProfileQualifiedId
import com.wire.android.ui.userprofile.other.OtherUserProfileRoute
import com.wire.android.ui.userprofile.self.SelfUserProfileRoute
import com.wire.android.ui.userprofile.service.ServiceDetailsRoute
import com.wire.android.ui.userprofile.service.ServiceProfileTarget
import com.wire.android.ui.userprofile.teammigration.TeamMigrationTeamPlanRoute
import com.wire.android.feature.meetings.ui.create.NewMeetingType
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId

/**
 * Activity-owned effects which must not leak an Android Activity, Intent or generated destination
 * into the Navigation 3 action implementation.
 */
@Suppress("LongParameterList")
internal data class WireNavigation3ActivityCallbacks(
    val finish: () -> Unit,
    val openUrl: (String) -> Unit,
    val openIntent: (WireNavigation3ExternalIntent) -> Unit,
    val openTeamAccountWebFlow: (AuthenticationTeamAccountCreationRequest) -> Unit,
    val completeAppLock: () -> Unit,
    val cancelAppLock: () -> Unit,
    val hardLogout: () -> Unit,
    val restartAfterLogout: () -> Unit,
    val moveTaskToBackground: () -> Unit,
)

internal enum class WireNavigation3ExternalIntent {
    SUPPORT,
    TEAM_MANAGEMENT,
    TEAM_PLAN,
    TERMS_OF_USE,
    WIRE_WEBSITE,
    PRIVACY_POLICY,
    REPORT_MISUSE,
    GIVE_FEEDBACK,
    WELCOME_ANDROID,
    ANDROID_RELEASE_NOTES,
}

/**
 * Production implementation shared by every app-owned Navigation 3 contribution.
 *
 * Route creation and back-stack mutation live here; Activity-only work is represented by the
 * semantic callback ports above.
 */
@Suppress("LargeClass", "TooManyFunctions")
internal class WireNavigation3ProductionActions(
    private val runtime: WireNavigation3Runtime,
    private val activity: WireNavigation3ActivityCallbacks,
    private val currentSessionId: () -> WireSessionId?,
    private val loginTypeSelector: LoginTypeSelector,
    private val authenticationRouter: AuthenticationNavigation3Router,
) : WireNavigation3CompositeActions {

    internal var pendingBackgroundAfterHomeSession by mutableStateOf<WireSessionId?>(null)
        private set

    override val cells: AllFilesNavigationActions = AllFilesNavigationActions(
        openSearch = {
            navigate(SearchRoute(requireSession(), screenType = CellsSearchType.DRIVE))
        },
        showPublicLink = {
            navigate(
                PublicLinkRoute(
                    sessionId = requireSession(),
                    assetId = it.assetId,
                    fileName = it.fileName,
                    publicLinkId = it.linkId,
                    isFolder = it.isFolder,
                )
            )
        },
        showAddRemoveTags = {
            navigate(AddRemoveTagsRoute(requireSession(), it.uuid, it.tags))
        },
        showImageViewer = {
            navigate(
                CellImageViewerRoute(
                    sessionId = requireSession(),
                    localPath = it.localPath,
                    contentUrl = it.contentUrl,
                    previewUrl = it.previewUrl,
                    contentHash = it.contentHash,
                    fileName = it.name,
                )
            )
        },
        showVideoPlayer = {
            navigate(
                VideoPlayerRoute(
                    sessionId = requireSession(),
                    localPath = it.localPath,
                    contentUrl = it.contentUrl,
                    fileName = it.name,
                )
            )
        },
        showAudioPlayer = {
            navigate(
                com.wire.android.feature.cells.navigation.AudioPlayerRoute(
                    sessionId = requireSession(),
                    localPath = it.localPath,
                    contentUrl = it.contentUrl,
                    fileName = it.name,
                )
            )
        },
    )
    override val conversations: ConversationsNavigationActions = ConversationsNavigationActions(
        openConversation = { openConversation(it.toProfileId()) },
        openUserProfile = { openUserProfile(it.value, it.domain) },
        startConversation = ::openNewConversation,
        browseChannels = { navigate(BrowseChannelsRoute(requireSession())) },
        openConversationFolders = {},
        promoteAdmin = { args ->
            navigate(
                PromoteAdminRoute(
                    requireSession(),
                    args.conversationId.toAuxId(),
                    args.eligibleMembers.mapNotNull(::parseEligibleMember),
                )
            )
        },
        openDebugMenu = { navigate(DebugConversationRoute(requireSession(), it.conversationId.toAuxId())) },
    )

    override fun canUseNewLogin() = loginTypeSelector.canUseNewLogin()
    override fun exitAuthentication() = activity.finish()
    override fun openUrl(url: String) = activity.openUrl(url)
    override fun openTeamAccountCreation(request: AuthenticationTeamAccountCreationRequest) =
        activity.openTeamAccountWebFlow(request)

    override fun onRequirement(requirement: HomeRequirement) =
        authenticationRouter.homeRequirement(requirement, currentSessionId()).let { Unit }

    override fun openNewConversation() =
        navigate(NewConversationSearchPeopleRoute.start(requireSession()))

    override fun openSelfProfile() = navigate(SelfUserProfileRoute(requireSession()))

    override fun openExternal(destination: HomeExternalDestination) =
        activity.openIntent(
            when (destination) {
                HomeExternalDestination.SUPPORT -> WireNavigation3ExternalIntent.SUPPORT
                HomeExternalDestination.TEAM_MANAGEMENT -> WireNavigation3ExternalIntent.TEAM_MANAGEMENT
            }
        )

    override fun openWhatsNew(target: WhatsNewNavigation3Target) =
        when (target) {
            WhatsNewNavigation3Target.Welcome ->
                activity.openIntent(WireNavigation3ExternalIntent.WELCOME_ANDROID)
            WhatsNewNavigation3Target.AllAndroidReleaseNotes ->
                activity.openIntent(WireNavigation3ExternalIntent.ANDROID_RELEASE_NOTES)
            is WhatsNewNavigation3Target.ExternalReleaseNote ->
                activity.openUrl(target.url)
        }
    override fun openNewMeeting(type: NewMeetingType) =
        navigate(NewMeetingDetailsRoute.start(requireSession(), type))

    override fun exitFlow() = activity.finish()
    override fun openUserProfile(userIdValue: String, userIdDomain: String) =
        navigate(OtherUserProfileRoute(requireSession(), UserProfileQualifiedId(userIdValue, userIdDomain)))

    override fun openServiceDetails(serviceId: String, providerId: String, useNewAppsUi: Boolean) =
        navigate(
            ServiceDetailsRoute(
                requireSession(),
                conversationId = null,
                target = serviceTarget(serviceId, providerId, useNewAppsUi),
            )
        )

    override fun completeFlowWithCreatedConversation(
        conversationIdValue: String,
        conversationIdDomain: String,
    ) = navigate(
        ConversationRoute(requireSession(), ConversationRouteId(conversationIdValue, conversationIdDomain)),
        WireBackStackMode.REMOVE_CURRENT_NESTED_FLOW,
    )

    override fun discardFlowToHome() = navigate(HomeRoute(requireSession()), WireBackStackMode.CLEAR_WHOLE)
    override fun openTeamPlan() = activity.openIntent(WireNavigation3ExternalIntent.TEAM_PLAN)

    override fun open(destination: SettingsNavigation3Destination) {
        WireNavigation3ProductionCommandResolver.settings(destination, requireSession())?.let {
            runtime.navigator.navigate(it)
        } ?: activity.openIntent(destination.toExternalIntent())
    }

    override fun openAppLockSetup() = navigate(SetLockCodeRoute(requireSession()))
    override fun openConversations() = navigate(HomeRoute(requireSession()), WireBackStackMode.CLEAR_WHOLE)
    override fun exitSettings() = goBackOrFinish()

    override fun exitDeviceManagement() = goBackOrFinish()
    override fun exitUserProfile() = goBackOrFinish()
    override fun openAppSettings() = navigate(AppSettingsRoute(requireSession()))
    override fun openAddAccount() {
        authenticationRouter.navigate(
            AuthenticationNavigationTransition.HOME_TO_ADD_ACCOUNT,
            authenticationRoute(),
        )
    }
    override fun openTeamMigration(wasMigrationDotActive: Boolean) =
        navigate(TeamMigrationTeamPlanRoute.start(requireSession(), wasMigrationDotActive))

    override fun openAccountDetails() = navigate(MyAccountRoute(requireSession()))
    override fun switchedToAnotherAccount() =
        navigate(HomeRoute(requireSession()), WireBackStackMode.CLEAR_WHOLE)

    override fun noOtherAccountToSwitch() {
        noOtherAccountNavigationCommand(
            currentRoute = runtime.navigator.currentRoute,
            useNewLogin = canUseNewLogin(),
        )?.let {
            authenticationRouter.navigate(AuthenticationNavigationTransition.ACCOUNT_SWITCH_TO_LOGIN, it)
        }
    }

    override fun openConversation(conversationId: UserProfileQualifiedId) =
        navigate(
            ConversationRoute(requireSession(), conversationId.toConversationId()),
            WireBackStackMode.UPDATE_EXISTING,
        )

    override fun openDeviceDetails(userId: UserProfileQualifiedId, clientId: String) =
        navigate(
            DeviceDetailsRoute(
                requireSession(),
                DeviceTargetUserId(userId.value, userId.domain),
                clientId,
            )
        )

    override fun searchConversationMessages(conversationId: UserProfileQualifiedId) =
        navigate(SearchConversationMessagesRoute(requireSession(), conversationId.toAuxId()))

    override fun openConversationMedia(conversationId: UserProfileQualifiedId) =
        navigate(ConversationMediaRoute(requireSession(), conversationId.toMediaId()))

    override fun openConversationDebugMenu(conversationId: UserProfileQualifiedId) =
        navigate(DebugConversationRoute(requireSession(), conversationId.toAuxId()))

    override fun exitMigration() = goBackOrFinish()
    override fun completeMigrationToHome() =
        navigate(HomeRoute(requireSession()), WireBackStackMode.CLEAR_WHOLE)

    override fun exitConversation() = goBackOrFinish()
    override fun completeConversation(result: ConversationCompletionResult) = goBackOrFinish()
    override fun openConversationAtMessage(conversationId: ConversationAuxId, messageId: String) =
        navigate(
            ConversationRoute(
                requireSession(),
                conversationId.toConversationId(),
                searchedMessageId = messageId,
            ),
            WireBackStackMode.UPDATE_EXISTING,
        )

    override fun openCellsSearch(conversationId: ConversationAuxId) =
        navigate(
            SearchRoute(
                sessionId = requireSession(),
                conversationId = QualifiedID(conversationId.value, conversationId.domain).toString(),
            )
        )

    override fun openUserProfile(userId: ConversationAuxId) =
        navigate(OtherUserProfileRoute(requireSession(), userId.toProfileId()))

    override fun openService(
        conversationId: ConversationAuxId,
        serviceId: ConversationAuxId,
        shouldUseNewAppsUi: Boolean,
    ) = navigate(
        ServiceDetailsRoute(
            requireSession(),
            conversationId.toProfileId(),
            serviceTarget(serviceId.value, serviceId.domain, shouldUseNewAppsUi),
        )
    )

    override fun exitDetails() = activity.finish()

    override fun openParticipantProfile(participant: UIParticipant, conversationId: ConversationDetailsId) {
        val destination = when {
            participant.isSelf -> SelfUserProfileRoute(requireSession())
            participant.isService -> ServiceDetailsRoute(
                requireSession(),
                conversationId.toProfileId(),
                participant.botService?.let {
                    ServiceProfileTarget.Bot(UserProfileQualifiedId(it.id, it.provider))
                } ?: ServiceProfileTarget.App(participant.id.toProfileId()),
            )
            else -> OtherUserProfileRoute(
                requireSession(),
                participant.id.toProfileId(),
                conversationId.toProfileId(),
            )
        }
        navigate(destination)
    }

    override fun openAddMembers(
        conversationId: ConversationDetailsId,
        isConversationAppsEnabled: Boolean,
        isSelfPartOfATeam: Boolean,
        protocolInfo: Conversation.ProtocolInfo,
        shouldUseNewAppsUi: Boolean,
    ) = navigate(
        AddMembersSearchRoute(
            requireSession(),
            conversationId.toAuxId(),
            isConversationAppsEnabled,
            isSelfPartOfATeam,
            protocolInfo.toNavigation3(),
            shouldUseNewAppsUi,
        )
    )

    override fun openSearchMessages(
        conversationId: ConversationDetailsId,
        isCellsConversation: Boolean,
        groupName: String,
    ) = navigate(
        SearchConversationMessagesRoute(
            requireSession(),
            conversationId.toAuxId(),
            groupName,
            isCellsConversation,
        )
    )

    override fun openConversationMedia(
        conversationId: ConversationDetailsId,
        isCellsConversation: Boolean,
        groupName: String,
    ) = navigate(
        if (isCellsConversation) {
            ConversationFilesRoute(
                sessionId = requireSession(),
                args = CellsFilesArguments(
                    conversationId = QualifiedID(conversationId.value, conversationId.domain).toString(),
                    breadcrumbs = listOf(groupName),
                ),
            )
        } else {
            ConversationMediaRoute(requireSession(), conversationId.toMediaId())
        }
    )

    override fun completeDetails(
        action: com.wire.android.ui.home.conversations.details.GroupConversationActionType,
        conversationName: String,
    ) = goBackOrFinish()

    override fun openPromoteAdmin(conversationId: ConversationId, eligibleMembers: List<UserId>) =
        navigate(
            PromoteAdminRoute(
                requireSession(),
                conversationId.toAuxId(),
                eligibleMembers.map { it.toAuxId() },
            )
        )

    override fun openConversationDebugMenu(conversationId: ConversationId) =
        navigate(DebugConversationRoute(requireSession(), conversationId.toAuxId()))

    override fun finishShare() = activity.finish()
    override fun openAuthentication() =
        navigate(authenticationRoute(), WireBackStackMode.CLEAR_WHOLE)

    override fun openConversationFromShare(
        conversationId: MediaConversationId,
        assets: List<MediaAssetDto>,
        text: String?,
    ) {
        runtime.navigator.navigate(
            WireNavigation3ProductionCommandResolver.conversationFromShare(
                requireSession(),
                conversationId,
                assets,
                text,
            )
        )
    }

    override fun openPublicLink(assetId: String, fileName: String, publicLinkId: String?) =
        navigate(
            PublicLinkRoute(
                sessionId = requireSession(),
                assetId = assetId,
                fileName = fileName,
                publicLinkId = publicLinkId,
                isFolder = false,
            )
        )

    override fun completeInitialSync(route: InitialSyncRoute, shouldMoveToBackground: Boolean) {
        // The remember wrapper observes Home before invoking the Activity effect, matching legacy.
        pendingBackgroundAfterHomeSession = route.sessionId.takeIf { shouldMoveToBackground }
        authenticationRouter.completeSessionSetup(route.sessionId, SessionSetupDestination.HOME)
    }

    override fun completeUnlock() = activity.completeAppLock()
    override fun cancelUnlock() = activity.cancelAppLock()
    override fun restartAfterLogout() = activity.restartAfterLogout()

    override fun exitCellsFlow() = goBackOrFinish()

    override fun exitMeetingFlow() = activity.finish()

    override fun openUserProfile(userId: MeetingParticipantId) =
        openUserProfile(userId.value, userId.domain)

    internal fun moveTaskToBackgroundAfterHomeIsVisible() {
        val expectedSession = pendingBackgroundAfterHomeSession
        val current = runtime.navigator.currentRoute as? HomeRoute
        if (expectedSession != null && current?.sessionId == expectedSession) {
            pendingBackgroundAfterHomeSession = null
            activity.moveTaskToBackground()
        }
    }

    private fun authenticationRoute(): WireRoute =
        if (canUseNewLogin()) NewLoginRoute.start() else WelcomeRoute()

    private fun navigate(
        route: WireRoute,
        mode: WireBackStackMode = WireBackStackMode.NONE,
    ) {
        runtime.navigator.navigate(WireNavigationCommand(route, mode))
    }

    private fun goBackOrFinish() {
        if (!runtime.navigator.goBack()) activity.finish()
    }

    private fun requireSession(): WireSessionId =
        checkNotNull(currentSessionId()) { "A session-backed Navigation 3 action requires an active session" }
}

@Composable
internal fun rememberWireNavigation3ProductionActions(
    runtime: WireNavigation3Runtime,
    activity: WireNavigation3ActivityCallbacks,
    currentSessionId: () -> WireSessionId?,
    loginTypeSelector: LoginTypeSelector,
    authenticationRouter: AuthenticationNavigation3Router,
): WireNavigation3ProductionActions {
    val actions = remember(runtime, activity, currentSessionId, loginTypeSelector, authenticationRouter) {
        WireNavigation3ProductionActions(
            runtime,
            activity,
            currentSessionId,
            loginTypeSelector,
            authenticationRouter,
        )
    }
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    val pendingBackgroundSession = actions.pendingBackgroundAfterHomeSession
    LaunchedEffect(currentEntryId, pendingBackgroundSession) {
        actions.moveTaskToBackgroundAfterHomeIsVisible()
    }
    return actions
}

internal object WireNavigation3ProductionCommandResolver {
    fun completeLogin(
        completion: AuthenticationLoginCompletion,
    ): WireNavigationCommand = WireNavigationCommand(
        destination = when (completion) {
            is AuthenticationLoginCompletion.Home -> HomeRoute(completion.sessionId)
            is AuthenticationLoginCompletion.InitialSync -> InitialSyncRoute(completion.sessionId)
            is AuthenticationLoginCompletion.E2EIEnrollment -> E2EIEnrollmentRoute(completion.sessionId)
            is AuthenticationLoginCompletion.RemoveDevice -> RemoveDeviceRoute(completion.sessionId)
        },
        backStackMode = WireBackStackMode.CLEAR_WHOLE,
    )

    fun homeRequirement(
        requirement: HomeRequirement,
        currentSessionId: WireSessionId?,
    ): WireNavigationCommand = WireNavigationCommand(
        destination = when (requirement) {
            is HomeRequirement.RegisterDevice ->
                RegisterDeviceRoute(WireSessionId(requirement.userId.value, requirement.userId.domain))
            HomeRequirement.CreateAccountUsername -> CreateAccountUsernameRoute.start(
                checkNotNull(currentSessionId) {
                    "$requirement requires the active session"
                }
            )
            HomeRequirement.InitialSync -> InitialSyncRoute(
                checkNotNull(currentSessionId) { "InitialSync requires an active session" }
            )
        },
        backStackMode = WireBackStackMode.CLEAR_WHOLE,
    )

    fun settings(
        destination: SettingsNavigation3Destination,
        sessionId: WireSessionId,
    ): WireNavigationCommand? {
        val route = when (destination) {
            SettingsNavigation3Destination.ACCOUNT -> MyAccountRoute(sessionId)
            SettingsNavigation3Destination.MANAGE_DEVICES -> SelfDevicesRoute(sessionId)
            SettingsNavigation3Destination.DEBUG_SETTINGS -> DebugRoute(sessionId)
            SettingsNavigation3Destination.ABOUT_APP -> AboutThisAppRoute(sessionId)
            else -> return null
        }
        return WireNavigationCommand(route)
    }

    fun conversationFromShare(
        sessionId: WireSessionId,
        conversationId: MediaConversationId,
        assets: List<MediaAssetDto>,
        text: String?,
    ) = WireNavigationCommand(
        destination = ConversationRoute(
            sessionId,
            ConversationRouteId(conversationId.value, conversationId.domain),
            pendingAssets = assets.map(MediaAssetDto::toConversationAsset),
            pendingText = text,
        ),
        backStackMode = WireBackStackMode.REMOVE_CURRENT_AND_REPLACE,
    )
}

private fun SettingsNavigation3Destination.toExternalIntent() =
    when (this) {
        SettingsNavigation3Destination.TERMS_OF_USE -> WireNavigation3ExternalIntent.TERMS_OF_USE
        SettingsNavigation3Destination.WIRE_WEBSITE -> WireNavigation3ExternalIntent.WIRE_WEBSITE
        SettingsNavigation3Destination.PRIVACY_POLICY -> WireNavigation3ExternalIntent.PRIVACY_POLICY
        SettingsNavigation3Destination.SUPPORT -> WireNavigation3ExternalIntent.SUPPORT
        SettingsNavigation3Destination.REPORT_MISUSE -> WireNavigation3ExternalIntent.REPORT_MISUSE
        SettingsNavigation3Destination.GIVE_FEEDBACK -> WireNavigation3ExternalIntent.GIVE_FEEDBACK
        else -> error("$this is owned by a typed Navigation 3 route")
    }

private fun serviceTarget(value: String, domain: String, useNewAppsUi: Boolean) =
    if (useNewAppsUi) {
        ServiceProfileTarget.App(UserProfileQualifiedId(value, domain))
    } else {
        ServiceProfileTarget.Bot(UserProfileQualifiedId(value, domain))
    }

private fun MediaAssetDto.toConversationAsset() = ConversationPendingAsset(
    key,
    mimeType,
    dataPath,
    dataSize,
    fileName,
    assetType,
    audioWavesMask
)

private fun UserProfileQualifiedId.toConversationId() = ConversationRouteId(value, domain)
private fun UserProfileQualifiedId.toAuxId() = ConversationAuxId(value, domain)
private fun UserProfileQualifiedId.toMediaId() = MediaConversationId(value, domain)
private fun ConversationAuxId.toConversationId() = ConversationRouteId(value, domain)
private fun ConversationAuxId.toProfileId() = UserProfileQualifiedId(value, domain)
private fun ConversationDetailsId.toAuxId() = ConversationAuxId(value, domain)
private fun ConversationDetailsId.toProfileId() = UserProfileQualifiedId(value, domain)
private fun ConversationDetailsId.toMediaId() = MediaConversationId(value, domain)
private fun QualifiedID.toAuxId() = ConversationAuxId(value, domain)
private fun QualifiedID.toProfileId() = UserProfileQualifiedId(value, domain)

private fun parseEligibleMember(value: String): ConversationAuxId? {
    val separator = value.lastIndexOf('@')
    if (separator <= 0 || separator == value.lastIndex) return null
    return ConversationAuxId(value.substring(0, separator), value.substring(separator + 1))
}
