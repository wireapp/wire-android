/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.ObserveSyncStateUseCaseProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.migration.MigrationManager
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.services.ServicesManager
import com.wire.android.ui.authentication.devices.model.displayName
import com.wire.android.ui.common.dialogs.CustomBEDeeplinkDialogState
import com.wire.android.ui.joinConversation.JoinConversationViaCodeState
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.client.NewClientResult
import com.wire.kalium.logic.feature.client.ObserveNewClientsUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase
import com.wire.kalium.logic.feature.conversation.JoinConversationViaCodeUseCase
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.util.DateTimeUtil.toIsoDateTimeString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val dispatchers: DispatcherProvider,
    private val currentSessionFlow: CurrentSessionFlowUseCase,
    private val getServerConfigUseCase: GetServerConfigUseCase,
    private val deepLinkProcessor: DeepLinkProcessor,
    private val navigationManager: NavigationManager,
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val getSessions: GetSessionsUseCase,
    private val accountSwitch: AccountSwitchUseCase,
    private val migrationManager: MigrationManager,
    private val servicesManager: ServicesManager,
    private val observeSyncStateUseCaseProviderFactory: ObserveSyncStateUseCaseProvider.Factory,
    private val observeIfAppUpdateRequired: ObserveIfAppUpdateRequiredUseCase,
    private val observeNewClients: ObserveNewClientsUseCase,
    private val currentScreenManager: CurrentScreenManager,
) : ViewModel() {

    var globalAppState: GlobalAppState by mutableStateOf(GlobalAppState())
        private set

    private val observeUserId = currentSessionFlow()
        .onEach {
            if (it is CurrentSessionResult.Success) {
                if (it.accountInfo.isValid().not()) {
                    handleInvalidSession((it.accountInfo as AccountInfo.Invalid).logoutReason)
                }
            }
        }
        .map { result ->
            if (result is CurrentSessionResult.Success) {
                if (result.accountInfo.isValid()) {
                    result.accountInfo.userId
                } else {
                    null
                }
            } else {
                null
            }
        }.distinctUntilChanged().flowOn(dispatchers.io()).shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    private val _observeSyncFlowState: MutableStateFlow<SyncState?> = MutableStateFlow(null)
    val observeSyncFlowState: StateFlow<SyncState?> = _observeSyncFlowState

    init {
        viewModelScope.launch(dispatchers.io()) {
            observeUserId
                .flatMapLatest {
                    it?.let { observeSyncStateUseCaseProviderFactory.create(it).observeSyncState() } ?: flowOf(null)
                }
                .distinctUntilChanged()
                .collect { _observeSyncFlowState.emit(it) }
        }
        viewModelScope.launch(dispatchers.io()) {
            observeIfAppUpdateRequired(BuildConfig.VERSION_CODE)
                .distinctUntilChanged()
                .collect {
                    globalAppState = globalAppState.copy(updateAppDialog = it)
                }
        }
        viewModelScope.launch(dispatchers.io()) {
            val currentScreenStateFlow = currentScreenManager.observeCurrentScreen(this)
            observeNewClients()
                .collect {
                    val newClientDialog = when (it) {
                        is NewClientResult.InCurrentAccount -> {
                            NewClientData.CurrentUser(
                                it.newClient.registrationTime?.toIsoDateTimeString() ?: "",
                                it.newClient.displayName()
                            )
                        }

                        is NewClientResult.InOtherAccount -> {
                            NewClientData.OtherUser(
                                it.newClient.registrationTime?.toIsoDateTimeString() ?: "",
                                it.newClient.displayName(),
                                it.userId,
                                it.userName,
                                it.userHandle
                            )
                        }

                        else -> null
                    }

                    newClientDialog?.let {
                        globalAppState = if (currentScreenStateFlow.value.isGlobalDialogAllowed()) {
                            globalAppState.copy(newClientDialog = newClientDialog)
                        } else {
                            globalAppState.copy(newClientDialogRemembered = newClientDialog)
                        }
                    }
                }
        }

        viewModelScope.launch {
            currentScreenManager.observeCurrentScreen(this)
                .map { it.isGlobalDialogAllowed() }
                .scan(true to true) { prevPair, newValue -> prevPair.second to newValue }
                .collect { (wasAllowedOnPrev, isAllowedOnCurrent) ->
                    if (!wasAllowedOnPrev && isAllowedOnCurrent) {
                        globalAppState = globalAppState.copy(
                            newClientDialog = globalAppState.newClientDialogRemembered,
                            newClientDialogRemembered = null
                        )
                    } else if (wasAllowedOnPrev && !isAllowedOnCurrent) {
                        globalAppState = globalAppState.copy(
                            newClientDialogRemembered = globalAppState.newClientDialog,
                            newClientDialog = null
                        )
                    }
                }
        }
    }

    private suspend fun handleInvalidSession(logoutReason: LogoutReason) {
        withContext(dispatchers.main()) {
            when (logoutReason) {
                LogoutReason.SELF_SOFT_LOGOUT, LogoutReason.SELF_HARD_LOGOUT -> {
                    // Self logout is handled from the Self user profile screen directly
                }

                LogoutReason.REMOVED_CLIENT ->
                    globalAppState = globalAppState.copy(blockUserUI = CurrentSessionErrorState.RemovedClient)

                LogoutReason.DELETED_ACCOUNT ->
                    globalAppState = globalAppState.copy(blockUserUI = CurrentSessionErrorState.DeletedAccount)

                LogoutReason.SESSION_EXPIRED ->
                    globalAppState = globalAppState.copy(blockUserUI = CurrentSessionErrorState.SessionExpired)
            }
        }
    }

    fun navigateToNextAccountOrWelcome() {
        viewModelScope.launch {
            globalAppState = globalAppState.copy(blockUserUI = null)
            accountSwitch(SwitchAccountParam.SwitchToNextAccountOrWelcome)
        }
    }

    fun startNavigationRoute(): String = when {
        shouldGoToMigration() -> NavigationItem.Migration.getRouteWithArgs()
        shouldGoToWelcome() -> NavigationItem.Welcome.getRouteWithArgs()
        else -> NavigationItem.Home.getRouteWithArgs()
    }

    fun isSharingIntent(intent: Intent?): Boolean {
        return intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_SEND_MULTIPLE
    }

    @Suppress("ComplexMethod")
    fun handleDeepLink(intent: Intent?) {
        if (shouldGoToMigration()) {
            // means User is Logged in, but didn't finish the migration yet.
            // so we need to finish migration first.
            return
        }

        viewModelScope.launch {
            val result = intent?.data?.let { deepLinkProcessor(it) }
            when {
                result is DeepLinkResult.SSOLogin -> openSsoLogin(result)
                result is DeepLinkResult.MigrationLogin -> openMigrationLogin(result.userHandle)
                result is DeepLinkResult.CustomServerConfig -> onCustomServerConfig(result)

                isSharingIntent(intent) -> navigateToImportMediaScreen()

                shouldGoToWelcome() -> {
                    // to handle the deepLinks above user needs to be Logged in
                    // do nothing, navigating to Login is handled by startNavigationRoute()
                }

                result is DeepLinkResult.IncomingCall -> openIncomingCall(result.conversationsId)
                result is DeepLinkResult.OngoingCall -> openOngoingCall(result.conversationsId)
                result is DeepLinkResult.OpenConversation -> openConversation(result.conversationsId)
                result is DeepLinkResult.OpenOtherUserProfile -> openOtherUserProfile(result.userId)
                result is DeepLinkResult.JoinConversation -> onConversationInviteDeepLink(result.code, result.key, result.domain)
                result is DeepLinkResult.Unknown -> appLogger.e("unknown deeplink result $result")
            }
        }
    }

    fun dismissCustomBackendDialog() {
        globalAppState = globalAppState.copy(customBackendDialog = CustomBEDeeplinkDialogState(shouldShowDialog = false))
    }

    fun customBackendDialogProceedButtonClicked(serverLinks: ServerConfig.Links) {
        viewModelScope.launch {
            dismissCustomBackendDialog()
            authServerConfigProvider.updateAuthServer(serverLinks)
            if (checkNumberOfSessions() == BuildConfig.MAX_ACCOUNTS) {
                globalAppState = globalAppState.copy(maxAccountDialog = true)
            } else {
                navigateTo(NavigationCommand(NavigationItem.Welcome.getRouteWithArgs()))
            }
        }
    }

    fun dismissNewClientDialog() {
        globalAppState = globalAppState.copy(newClientDialog = null, newClientDialogRemembered = null)
    }

    fun switchAccount(userId: UserId) {
        viewModelScope.launch {
            accountSwitch(SwitchAccountParam.SwitchToAccount(userId))
            openDeviceManager()
        }
    }

    private suspend fun checkNumberOfSessions(): Int {
        getSessions().let {
            return when (it) {
                is GetAllSessionsResult.Success -> {
                    it.sessions.filterIsInstance<AccountInfo.Valid>().size
                }

                is GetAllSessionsResult.Failure.Generic -> 0
                GetAllSessionsResult.Failure.NoSessionFound -> 0
            }
        }
    }

    fun openDeviceManager() {
        navigateTo(NavigationCommand(NavigationItem.SelfDevices.getRouteWithArgs()))
    }

    private fun navigateToImportMediaScreen() {
        navigateTo(NavigationCommand(NavigationItem.ImportMedia.getRouteWithArgs(), backStackMode = BackStackMode.UPDATE_EXISTED))
    }

    private fun openIncomingCall(conversationId: ConversationId) {
        navigateTo(NavigationCommand(NavigationItem.IncomingCall.getRouteWithArgs(listOf(conversationId))))
    }

    private fun openOngoingCall(conversationId: ConversationId) {
        navigateTo(NavigationCommand(NavigationItem.OngoingCall.getRouteWithArgs(listOf(conversationId))))
    }

    private fun openConversation(conversationId: ConversationId) {
        navigateTo(NavigationCommand(NavigationItem.Conversation.getRouteWithArgs(listOf(conversationId)), BackStackMode.UPDATE_EXISTED))
    }

    private fun openOtherUserProfile(userId: QualifiedID) {
        navigateTo(NavigationCommand(NavigationItem.OtherUserProfile.getRouteWithArgs(listOf(userId)), BackStackMode.UPDATE_EXISTED))
    }

    private fun openMigrationLogin(userHandle: String) {
        navigateTo(NavigationCommand(NavigationItem.Login.getRouteWithArgs(listOf(userHandle)), BackStackMode.UPDATE_EXISTED))
    }

    private fun openSsoLogin(ssoLogin: DeepLinkResult.SSOLogin) {
        navigateTo(NavigationCommand(NavigationItem.Login.getRouteWithArgs(listOf(ssoLogin)), BackStackMode.UPDATE_EXISTED))
    }

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch {
            navigationManager.navigate(command)
        }
    }

    private suspend fun loadServerConfig(url: String): ServerConfig.Links? = when (val result = getServerConfigUseCase(url)) {
        is GetServerConfigResult.Success -> result.serverConfigLinks
        // TODO: show error message on failure
        is GetServerConfigResult.Failure.Generic -> {
            appLogger.e("something went wrong during handling the custom server deep link: ${result.genericFailure}")
            null
        }
    }

    private suspend fun onCustomServerConfig(result: DeepLinkResult.CustomServerConfig) {
        loadServerConfig(result.url)?.let { serverLinks ->
            globalAppState = globalAppState.copy(
                customBackendDialog = CustomBEDeeplinkDialogState(
                    shouldShowDialog = true,
                    serverLinks = serverLinks
                )
            )
        }
    }

    private suspend fun onConversationInviteDeepLink(
        code: String,
        key: String,
        domain: String?
    ) = when (val currentSession = coreLogic.getGlobalScope().session.currentSession()) {
        is CurrentSessionResult.Failure.Generic -> null
        CurrentSessionResult.Failure.SessionNotFound -> null
        is CurrentSessionResult.Success -> {
            coreLogic.sessionScope(currentSession.accountInfo.userId) {
                when (val result = conversations.checkIConversationInviteCode(code, key, domain)) {
                    is CheckConversationInviteCodeUseCase.Result.Success -> {
                        if (result.isSelfMember) {
                            // TODO; display messsage that user is already a member and ask if they want to navigate to the conversation
                            openConversation(result.conversationId)
                        } else {
                            globalAppState =
                                globalAppState.copy(
                                    conversationJoinedDialog = JoinConversationViaCodeState.Show(
                                        result.name,
                                        code,
                                        key,
                                        domain
                                    )
                                )
                        }
                    }

                    is CheckConversationInviteCodeUseCase.Result.Failure -> globalAppState =
                        globalAppState.copy(conversationJoinedDialog = JoinConversationViaCodeState.Error(result))
                }
            }
        }
    }

    fun joinConversationViaCode(
        code: String,
        key: String,
        domain: String?
    ) = viewModelScope.launch {
        when (val currentSession = coreLogic.getGlobalScope().session.currentSession()) {
            is CurrentSessionResult.Failure.Generic -> globalAppState = globalAppState.copy(conversationJoinedDialog = null)

            CurrentSessionResult.Failure.SessionNotFound -> globalAppState = globalAppState.copy(conversationJoinedDialog = null)

            is CurrentSessionResult.Success -> {
                coreLogic.sessionScope(currentSession.accountInfo.userId) {
                    when (val result = conversations.joinConversationViaCode(code, key, domain)) {
                        is JoinConversationViaCodeUseCase.Result.Failure -> {
                            appLogger.e("something went wrong during handling the join conversation deep link: ${result.failure}")
                            globalAppState = globalAppState.copy(conversationJoinedDialog = null)
                        }

                        is JoinConversationViaCodeUseCase.Result.Success -> {
                            globalAppState = globalAppState.copy(conversationJoinedDialog = null)
                            result.conversationId?.let {
                                openConversation(it)
                            }
                        }
                    }
                }
            }
        }
    }.invokeOnCompletion {
        // in case of failure, we need to dismiss the dialog
        it?.let {
            globalAppState = globalAppState.copy(conversationJoinedDialog = null)
        }
    }

    fun cancelJoinConversation() {
        globalAppState = globalAppState.copy(conversationJoinedDialog = null)
    }

    private fun shouldGoToWelcome(): Boolean = !hasValidCurrentSession()

    private fun hasValidCurrentSession(): Boolean = runBlocking {
        // TODO: the usage of currentSessionFlow is a temporary solution, it should be replaced with a proper solution
        currentSessionFlow().first().let {
            when (it) {
                is CurrentSessionResult.Failure.Generic -> false
                CurrentSessionResult.Failure.SessionNotFound -> false
                is CurrentSessionResult.Success -> true
            }
        }
    }

    private fun shouldGoToMigration(): Boolean = runBlocking {
        migrationManager.shouldMigrate()
    }

    fun openProfile() {
        dismissMaxAccountDialog()
        navigateTo(NavigationCommand(NavigationItem.SelfUserProfile.getRouteWithArgs()))
    }

    fun dismissMaxAccountDialog() {
        globalAppState = globalAppState.copy(maxAccountDialog = false)
    }

    fun observePersistentConnectionStatus() {
        viewModelScope.launch {
            coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus().let { result ->
                when (result) {
                    is ObservePersistentWebSocketConnectionStatusUseCase.Result.Failure -> {
                        appLogger.e("Failure while fetching persistent web socket status flow from wire activity")
                    }

                    is ObservePersistentWebSocketConnectionStatusUseCase.Result.Success -> {
                        result.persistentWebSocketStatusListFlow.collect { statuses ->

                            if (statuses.any { it.isPersistentWebSocketEnabled }) {
                                if (!servicesManager.isPersistentWebSocketServiceRunning()) {
                                    servicesManager.startPersistentWebSocketService()
                                }
                            } else {
                                servicesManager.stopPersistentWebSocketService()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun CurrentScreen.isGlobalDialogAllowed(): Boolean = when (this) {
        CurrentScreen.ImportMedia -> false

        CurrentScreen.InBackground,
        is CurrentScreen.Conversation,
        CurrentScreen.Home,
        is CurrentScreen.IncomingCallScreen,
        is CurrentScreen.OngoingCallScreen,
        is CurrentScreen.OtherUserProfile,
        CurrentScreen.SomeOther -> true
    }
}

sealed class CurrentSessionErrorState {
    object RemovedClient : CurrentSessionErrorState()
    object DeletedAccount : CurrentSessionErrorState()
    object SessionExpired : CurrentSessionErrorState()
}

sealed class NewClientData(open val date: String, open val deviceInfo: UIText) {
    data class CurrentUser(override val date: String, override val deviceInfo: UIText) : NewClientData(date, deviceInfo)
    data class OtherUser(
        override val date: String,
        override val deviceInfo: UIText,
        val userId: UserId,
        val userName: String?,
        val userHandle: String?
    ) : NewClientData(date, deviceInfo)
}

data class GlobalAppState(
    val customBackendDialog: CustomBEDeeplinkDialogState = CustomBEDeeplinkDialogState(),
    val maxAccountDialog: Boolean = false,
    val blockUserUI: CurrentSessionErrorState? = null,
    val updateAppDialog: Boolean = false,
    val conversationJoinedDialog: JoinConversationViaCodeState? = null,
    val newClientDialog: NewClientData? = null,
    // In cases when the new client comes and we need to inform user about it, but user is in some screen that doesn't allow dialogs,
    // we need to store that state somewhere and show the dialog later when it's possible.
    // This field is not used in Compose, only for storing and using latter.
    val newClientDialogRemembered: NewClientData? = null,
)
