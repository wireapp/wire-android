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
import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.di.ObserveSyncStateUseCaseProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.migration.MigrationManager
import com.wire.android.services.ServicesManager
import com.wire.android.ui.authentication.devices.model.displayName
import com.wire.android.ui.common.dialogs.CustomServerDialogState
import com.wire.android.ui.joinConversation.JoinConversationViaCodeState
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.user.IsUserLoggedInUseCase
import com.wire.kalium.logic.feature.user.UpdateLoggedInUsersCountUseCase
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.client.ClearNewClientsForUserUseCase
import com.wire.kalium.logic.feature.client.NewClientResult
import com.wire.kalium.logic.feature.client.ObserveNewClientsUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.util.DateTimeUtil.toIsoDateTimeString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val dispatchers: DispatcherProvider,
    private val currentSessionFlow: CurrentSessionFlowUseCase,
    private val observeValidAccount: ObserveValidAccountsUseCase,
    private val getServerConfigUseCase: GetServerConfigUseCase,
    private val deepLinkProcessor: DeepLinkProcessor,
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val getSessions: GetSessionsUseCase,
    private val accountSwitch: AccountSwitchUseCase,
    private val migrationManager: MigrationManager,
    private val servicesManager: ServicesManager,
    private val observeSyncStateUseCaseProviderFactory: ObserveSyncStateUseCaseProvider.Factory,
    private val observeIfAppUpdateRequired: ObserveIfAppUpdateRequiredUseCase,
    private val observeNewClients: ObserveNewClientsUseCase,
    private val clearNewClientsForUser: ClearNewClientsForUserUseCase,
    private val currentScreenManager: CurrentScreenManager,
    private val isUserLoggedIn: IsUserLoggedInUseCase,
    private val updateLoggedInUsersCount: UpdateLoggedInUsersCountUseCase,
    private val observeScreenshotCensoringConfigUseCaseProviderFactory: ObserveScreenshotCensoringConfigUseCaseProvider.Factory,
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
        }
        .distinctUntilChanged()
        .flowOn(dispatchers.io())
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    private val _observeSyncFlowState: MutableStateFlow<SyncState?> = MutableStateFlow(null)
    val observeSyncFlowState: StateFlow<SyncState?> = _observeSyncFlowState

    init {
        observeSyncState()
        observeUpdateAppState()
        observeNewClientState()
        observeScreenshotCensoringConfigState()
        viewModelScope.launch {
            observeValidAccount().distinctUntilChanged().collectLatest {
                updateLoggedInUsersCount(it.size)
            }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch(dispatchers.io()) {
            observeUserId
                .flatMapLatest {
                    it?.let { observeSyncStateUseCaseProviderFactory.create(it).observeSyncState() }
                        ?: flowOf(null)
                }
                .distinctUntilChanged()
                .collect { _observeSyncFlowState.emit(it) }
        }
    }

    private fun observeUpdateAppState() {
        viewModelScope.launch(dispatchers.io()) {
            observeIfAppUpdateRequired(BuildConfig.VERSION_CODE)
                .distinctUntilChanged()
                .collect {
                    globalAppState = globalAppState.copy(updateAppDialog = it)
                }
        }
    }

    private fun observeNewClientState() {
        viewModelScope.launch(dispatchers.io()) {
            currentScreenManager.observeCurrentScreen(this)
                .flatMapLatest {
                    if (it.isGlobalDialogAllowed()) observeNewClients()
                    else flowOf(NewClientResult.Empty)
                }
                .collect {
                    val newClientDialog = NewClientsData.fromUseCaseResul(it)
                    globalAppState = globalAppState.copy(newClientDialog = newClientDialog)
                }
        }
    }

    private fun observeScreenshotCensoringConfigState() {
        viewModelScope.launch(dispatchers.io()) {
            observeUserId
                .flatMapLatest {
                    it?.let {
                        observeScreenshotCensoringConfigUseCaseProviderFactory.create(it)
                            .observeScreenshotCensoringConfig()
                    } ?: flowOf(ObserveScreenshotCensoringConfigResult.Disabled)
                }.collect {
                    globalAppState = globalAppState.copy(
                        screenshotCensoringEnabled = it is ObserveScreenshotCensoringConfigResult.Enabled
                    )
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
                    globalAppState =
                        globalAppState.copy(blockUserUI = CurrentSessionErrorState.RemovedClient)

                LogoutReason.DELETED_ACCOUNT ->
                    globalAppState =
                        globalAppState.copy(blockUserUI = CurrentSessionErrorState.DeletedAccount)

                LogoutReason.SESSION_EXPIRED ->
                    globalAppState =
                        globalAppState.copy(blockUserUI = CurrentSessionErrorState.SessionExpired)
            }
        }
    }

    val initialAppState: InitialAppState
        get() = when {
            shouldMigrate() -> InitialAppState.NOT_MIGRATED
            else -> InitialAppState.LOGGED_IN
        }

    fun isSharingIntent(intent: Intent?): Boolean {
        return intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_SEND_MULTIPLE
    }

    @Suppress("ComplexMethod")
    fun handleDeepLink(
        intent: Intent?,
        onIsSharingIntent: () -> Unit,
        onOpenConversation: (ConversationId) -> Unit,
        onResult: (DeepLinkResult) -> Unit
    ) {
        if (shouldMigrate()) {
            // means User is Logged in, but didn't finish the migration yet.
            // so we need to finish migration first.
            return
        }
        viewModelScope.launch {
            val result = intent?.data?.let { deepLinkProcessor(it) }
            when {
                result is DeepLinkResult.SSOLogin -> onResult(result)
                result is DeepLinkResult.MigrationLogin -> onResult(result)
                result is DeepLinkResult.CustomServerConfig -> onCustomServerConfig(result)
                isSharingIntent(intent) -> onIsSharingIntent()
                shouldLogIn() -> {
                    // to handle the deepLinks above user needs to be Logged in
                    // do nothing, already handled by initialAppState
                }

                result is DeepLinkResult.JoinConversation ->
                    onConversationInviteDeepLink(
                        result.code,
                        result.key,
                        result.domain,
                        onOpenConversation
                    )

                result != null -> onResult(result)
                result is DeepLinkResult.Unknown -> appLogger.e("unknown deeplink result $result")
            }
        }
    }

    fun dismissCustomBackendDialog() {
        globalAppState = globalAppState.copy(customBackendDialog = null)
    }

    fun customBackendDialogProceedButtonClicked(onProceed: () -> Unit) {
        if (globalAppState.customBackendDialog != null) {
            viewModelScope.launch {
                authServerConfigProvider.updateAuthServer(globalAppState.customBackendDialog!!.serverLinks)
                dismissCustomBackendDialog()
                if (checkNumberOfSessions() >= BuildConfig.MAX_ACCOUNTS) {
                    globalAppState = globalAppState.copy(maxAccountDialog = true)
                } else {
                    onProceed()
                }
            }
        }
    }

    fun dismissNewClientsDialog(userId: UserId) {
        globalAppState = globalAppState.copy(newClientDialog = null)
        viewModelScope.launch { clearNewClientsForUser(userId) }
    }

    fun switchAccount(userId: UserId, actions: SwitchAccountActions, onComplete: () -> Unit) {
        viewModelScope.launch {
            accountSwitch(SwitchAccountParam.SwitchToAccount(userId))
                .callAction(actions)
            onComplete()
        }
    }

    fun tryToSwitchAccount(actions: SwitchAccountActions) {
        viewModelScope.launch {
            globalAppState = globalAppState.copy(blockUserUI = null)
            accountSwitch(SwitchAccountParam.TryToSwitchToNextAccount)
                .callAction(actions)
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

    private suspend fun loadServerConfig(url: String): ServerConfig.Links? =
        when (val result = getServerConfigUseCase(url)) {
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
                customBackendDialog = CustomServerDialogState(
                    serverLinks = serverLinks
                )
            )
        }
    }

    private suspend fun onConversationInviteDeepLink(
        code: String,
        key: String,
        domain: String?,
        onSuccess: (ConversationId) -> Unit
    ) = when (val currentSession = coreLogic.getGlobalScope().session.currentSession()) {
        is CurrentSessionResult.Failure.Generic -> null
        CurrentSessionResult.Failure.SessionNotFound -> null
        is CurrentSessionResult.Success -> {
            coreLogic.sessionScope(currentSession.accountInfo.userId) {
                when (val result = conversations.checkIConversationInviteCode(code, key, domain)) {
                    is CheckConversationInviteCodeUseCase.Result.Success -> {
                        if (result.isSelfMember) {
                            // TODO; display messsage that user is already a member and ask if they want to navigate to the conversation
                            appLogger.d("user is already a member of the conversation")
                            onSuccess(result.conversationId)
                        } else {
                            globalAppState =
                                globalAppState.copy(
                                    conversationJoinedDialog = JoinConversationViaCodeState.Show(
                                        result.name,
                                        code,
                                        key,
                                        domain,
                                        result.isPasswordProtected
                                    )
                                )
                        }
                    }

                    is CheckConversationInviteCodeUseCase.Result.Failure -> globalAppState =
                        globalAppState.copy(
                            conversationJoinedDialog = JoinConversationViaCodeState.Error(
                                result
                            )
                        )
                }
            }
        }
    }

    fun onJoinConversationFlowCompleted() {
        globalAppState = globalAppState.copy(conversationJoinedDialog = null)
    }

    fun shouldLogIn(): Boolean = runBlocking { !isUserLoggedIn() }

    fun shouldMigrate(): Boolean = runBlocking {
        migrationManager.shouldMigrate()
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
        CurrentScreen.ImportMedia,
        CurrentScreen.DeviceManager -> false

        CurrentScreen.InBackground,
        is CurrentScreen.Conversation,
        CurrentScreen.Home,
        is CurrentScreen.IncomingCallScreen,
        is CurrentScreen.OngoingCallScreen,
        is CurrentScreen.OtherUserProfile,
        CurrentScreen.AuthRelated,
        CurrentScreen.SomeOther -> true
    }
}

sealed class CurrentSessionErrorState {
    data object RemovedClient : CurrentSessionErrorState()
    data object DeletedAccount : CurrentSessionErrorState()
    data object SessionExpired : CurrentSessionErrorState()
}

sealed class NewClientsData(open val clientsInfo: List<NewClientInfo>, open val userId: UserId) {
    data class CurrentUser(
        override val clientsInfo: List<NewClientInfo>,
        override val userId: UserId
    ) : NewClientsData(clientsInfo, userId)

    data class OtherUser(
        override val clientsInfo: List<NewClientInfo>,
        override val userId: UserId,
        val userName: String?,
        val userHandle: String?
    ) : NewClientsData(clientsInfo, userId)

    companion object {
        fun fromUseCaseResul(result: NewClientResult): NewClientsData? = when (result) {
            is NewClientResult.InCurrentAccount -> {
                CurrentUser(
                    result.newClients.map(NewClientInfo::fromClient),
                    result.userId
                )
            }

            is NewClientResult.InOtherAccount -> {
                OtherUser(
                    result.newClients.map(NewClientInfo::fromClient),
                    result.userId,
                    result.userName,
                    result.userHandle
                )
            }

            else -> null
        }
    }
}

data class NewClientInfo(val date: String, val deviceInfo: UIText) {
    companion object {
        fun fromClient(client: Client): NewClientInfo =
            NewClientInfo(
                client.registrationTime?.toIsoDateTimeString() ?: "",
                client.displayName()
            )
    }
}

data class GlobalAppState(
    val customBackendDialog: CustomServerDialogState? = null,
    val maxAccountDialog: Boolean = false,
    val blockUserUI: CurrentSessionErrorState? = null,
    val updateAppDialog: Boolean = false,
    val conversationJoinedDialog: JoinConversationViaCodeState? = null,
    val newClientDialog: NewClientsData? = null,
    val screenshotCensoringEnabled: Boolean = true,
)

enum class InitialAppState {
    NOT_MIGRATED, NOT_LOGGED_IN, LOGGED_IN
}
