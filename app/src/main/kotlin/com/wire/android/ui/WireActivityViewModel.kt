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
import com.wire.android.ui.common.dialogs.CustomBEDeeplinkDialogState
import com.wire.android.ui.joinConversation.JoinConversationViaCodeState
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase
import com.wire.kalium.logic.feature.conversation.JoinConversationViaCodeUseCase
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
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
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val currentSessionFlow: CurrentSessionFlowUseCase,
    private val getServerConfigUseCase: GetServerConfigUseCase,
    private val deepLinkProcessor: DeepLinkProcessor,
    private val navigationManager: NavigationManager,
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val getSessions: GetSessionsUseCase,
    private val accountSwitch: AccountSwitchUseCase,
    private val migrationManager: MigrationManager,
    private val observeSyncStateUseCaseProviderFactory: ObserveSyncStateUseCaseProvider.Factory,
    private val observeIfAppUpdateRequired: ObserveIfAppUpdateRequiredUseCase,
    @KaliumCoreLogic private val coreLogic: CoreLogic
) : ViewModel() {

    private val navigationArguments = mutableMapOf<String, Any>(SERVER_CONFIG_ARG to ServerConfig.DEFAULT)
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

    fun navigationArguments() = navigationArguments.values.toList()

    fun startNavigationRoute(navigationItem: NavigationItem? = null, hasSharingIntent: Boolean = false): String = when {
        shouldGoToMigration() -> NavigationItem.Migration.getRouteWithArgs()
        shouldGoToWelcome() -> NavigationItem.Welcome.getRouteWithArgs()
        hasSharingIntent -> NavigationItem.ImportMedia.getRouteWithArgs()
        navigationItem != null -> navigationItem.getRouteWithArgs()
        else -> NavigationItem.Home.getRouteWithArgs()
    }

    fun isSharingIntent(intent: Intent?): Boolean {
        return intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_SEND_MULTIPLE
    }

    @Suppress("ComplexMethod")
    fun handleDeepLink(intent: Intent?) {
        if (shouldGoToImport(intent)) {
            navigateToImportMediaScreen()
        } else {
            intent?.data?.let { deepLink ->
                viewModelScope.launch {
                    if (isLaunchedFromHistory(intent)) {
                        // We don't need to handle deepLink, if activity was launched from history.
                        // For example: user opened app by deepLink, then closed it by back button click,
                        // then open the app from the "Recent Apps"
                        appLogger.d("Deep link is launched from history, ignoring")
                        return@launch
                    }

                    when (val result = deepLinkProcessor(deepLink, viewModelScope)) {
                        is DeepLinkResult.CustomServerConfig -> runBlocking {
                            loadServerConfig(result.url)?.let { serverLinks ->
                                globalAppState = globalAppState.copy(
                                    customBackendDialog = CustomBEDeeplinkDialogState(
                                        shouldShowDialog = true,
                                        serverLinks = serverLinks
                                    )
                                )
                                navigationArguments[SERVER_CONFIG_ARG] = serverLinks
                            }
                        }

                        is DeepLinkResult.SSOLogin -> navigationArguments[SSO_DEEPLINK_ARG] = result

                        is DeepLinkResult.IncomingCall -> navigationArguments[INCOMING_CALL_CONVERSATION_ID_ARG] = result.conversationsId

                        is DeepLinkResult.OngoingCall -> navigationArguments[ONGOING_CALL_CONVERSATION_ID_ARG] = result.conversationsId

                        is DeepLinkResult.OpenConversation -> navigationArguments[OPEN_CONVERSATION_ID_ARG] = result.conversationsId

                        is DeepLinkResult.OpenOtherUserProfile -> navigationArguments[OPEN_OTHER_USER_PROFILE_ARG] = result.userId

                        is DeepLinkResult.JoinConversation -> onConversationInviteDeepLink(result.code, result.key, result.domain)

                        is DeepLinkResult.MigrationLogin -> {
                            if (isLaunchedFromHistory(intent)) {
                                appLogger.i("MigrationLogin deepLink launched from the history")
                            } else {
                                navigationArguments.put(MIGRATION_LOGIN_ARG, result.userHandle)
                            }
                        }

                        DeepLinkResult.Unknown -> appLogger.e("unknown deeplink result $result")
                    }
                }
            }
        }
    }

    /**
     * Some of the deepLinks require to recreate Activity (Login, Welcome, etc.)
     * Others need to just open some screen, without recreating (Conversation, IncomingCall, etc.)
     *
     * @return true if Activity needs to be recreated, false - otherwise
     */
    fun handleDeepLinkOnNewIntent(intent: Intent?): Boolean {

        // removing arguments that could be there from prev deeplink handling
        navigationArguments.apply {
            remove(INCOMING_CALL_CONVERSATION_ID_ARG)
            remove(ONGOING_CALL_CONVERSATION_ID_ARG)
            remove(OPEN_CONVERSATION_ID_ARG)
            remove(OPEN_OTHER_USER_PROFILE_ARG)
            remove(SSO_DEEPLINK_ARG)
            remove(MIGRATION_LOGIN_ARG)
            remove(JOIN_CONVERSATION_ARG)
        }

        handleDeepLink(intent)
        if (isSharingIntent(intent)) {
            return false
        }
        return when {
            shouldGoToMigrationLogin() -> {
                openMigrationLogin(navigationArguments[MIGRATION_LOGIN_ARG] as String)
                false
            }

            shouldGoToLogin() || shouldGoToWelcome() || shouldGoToMigration() -> true

            shouldGoToIncomingCall() -> {
                openIncomingCall(navigationArguments[INCOMING_CALL_CONVERSATION_ID_ARG] as ConversationId)
                false
            }

            shouldGoToOngoingCall() -> {
                openOngoingCall(navigationArguments[ONGOING_CALL_CONVERSATION_ID_ARG] as ConversationId)
                false
            }

            shouldGoToConversation() -> {
                openConversation(navigationArguments[OPEN_CONVERSATION_ID_ARG] as ConversationId)
                false
            }

            shouldGoToOtherProfile() -> {
                openOtherUserProfile(navigationArguments[OPEN_OTHER_USER_PROFILE_ARG] as QualifiedID)
                false
            }

            isServerConfigOnPremises() -> false

            intent == null -> false
            else -> true
        }
    }

    fun dismissCustomBackendDialog() {
        globalAppState = globalAppState.copy(customBackendDialog = CustomBEDeeplinkDialogState(shouldShowDialog = false))
    }

    fun navigateToImportMediaScreen() {
        navigateTo(
            NavigationCommand(
                NavigationItem.ImportMedia.getRouteWithArgs(), backStackMode = BackStackMode.CLEAR_WHOLE
            )
        )
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

    private fun isLaunchedFromHistory(intent: Intent?) =
        intent?.flags != null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0

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

    private fun isServerConfigOnPremises(): Boolean =
        (navigationArguments[SERVER_CONFIG_ARG] as? ServerConfig.Links) != ServerConfig.DEFAULT

    private fun shouldGoToLogin(): Boolean =
        navigationArguments[SSO_DEEPLINK_ARG] != null || navigationArguments[MIGRATION_LOGIN_ARG] != null

    private fun shouldGoToIncomingCall(): Boolean =
        (navigationArguments[INCOMING_CALL_CONVERSATION_ID_ARG] as? ConversationId) != null

    private fun shouldGoToConversation(): Boolean =
        (navigationArguments[OPEN_CONVERSATION_ID_ARG] as? ConversationId) != null

    private fun shouldGoToOngoingCall(): Boolean =
        (navigationArguments[ONGOING_CALL_CONVERSATION_ID_ARG] as? ConversationId) != null

    private fun shouldGoToOtherProfile(): Boolean = (navigationArguments[OPEN_OTHER_USER_PROFILE_ARG] as? QualifiedID) != null

    private fun shouldGoToMigrationLogin(): Boolean = (navigationArguments[MIGRATION_LOGIN_ARG] as? String) != null

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

    private fun shouldGoToImport(intent: Intent?): Boolean {
        // Show import screen only if there is a valid session
        return hasValidCurrentSession() && isSharingIntent(intent)
    }

    fun openProfile() {
        dismissMaxAccountDialog()
        navigateTo(NavigationCommand(NavigationItem.SelfUserProfile.getRouteWithArgs()))
    }

    fun dismissMaxAccountDialog() {
        globalAppState = globalAppState.copy(maxAccountDialog = false)
    }

    fun appWasUpdate() {
        globalAppState = globalAppState.copy(updateAppDialog = false)
    }

    companion object {
        private const val SERVER_CONFIG_ARG = "server_config"
        private const val SSO_DEEPLINK_ARG = "sso_deeplink"
        private const val INCOMING_CALL_CONVERSATION_ID_ARG = "incoming_call_conversation_id"
        private const val ONGOING_CALL_CONVERSATION_ID_ARG = "ongoing_call_conversation_id"
        private const val OPEN_CONVERSATION_ID_ARG = "open_conversation_id"
        private const val OPEN_OTHER_USER_PROFILE_ARG = "open_other_user_id"
        private const val MIGRATION_LOGIN_ARG = "migration_login"
        private const val JOIN_CONVERSATION_ARG = "join_conversation"
    }
}

sealed class CurrentSessionErrorState {
    object RemovedClient : CurrentSessionErrorState()
    object DeletedAccount : CurrentSessionErrorState()
    object SessionExpired : CurrentSessionErrorState()
}

data class GlobalAppState(
    val customBackendDialog: CustomBEDeeplinkDialogState = CustomBEDeeplinkDialogState(),
    val maxAccountDialog: Boolean = false,
    val blockUserUI: CurrentSessionErrorState? = null,
    val updateAppDialog: Boolean = false,
    val conversationJoinedDialog: JoinConversationViaCodeState? = null
)
